package com.example.data.firebase

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "FirestoreManager"

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

class FirestoreManager(private val context: Context) {

    val firestore: FirebaseFirestore by lazy {
        val dbId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(dbId)
    }

    val auth: FirebaseAuth get() = Firebase.auth

    fun performGoogleSignIn(
        activity: Activity,
        scope: CoroutineScope,
        onSuccess: (uid: String, email: String, name: String) -> Unit,
        onError: (String) -> Unit,
        onCancelled: () -> Unit = {}
    ) {
        val credentialManager = CredentialManager.create(activity)
        val clientId = try {
            activity.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            onError("Google Sign-In configuration missing: default_web_client_id not found")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val user = authResult.user
                    if (user != null) {
                        onSuccess(
                            user.uid,
                            user.email.orEmpty(),
                            user.displayName ?: user.email?.substringBefore("@") ?: "User"
                        )
                    } else {
                        onError("User authentication failed")
                    }
                } else {
                    onError("Unexpected credential response")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled: ${e.message}", e)
                onCancelled()
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In error", e)
                onError(e.localizedMessage ?: "Sign-in error")
            }
        }
    }

    fun signOut(scope: CoroutineScope, onComplete: () -> Unit) {
        auth.signOut()
        val credentialManager = CredentialManager.create(context)
        scope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear credential state", e)
            } finally {
                onComplete()
            }
        }
    }

    fun syncUserProfile(
        userId: String,
        username: String,
        email: String,
        displayName: String,
        avatarUrl: String?,
        department: String,
        statusText: String,
        isOnline: Boolean
    ) {
        if (auth.currentUser == null) return
        val docRef = firestore.collection("users").document(userId)
        val data = hashMapOf<String, Any?>(
            "userId" to userId,
            "username" to username,
            "email" to email,
            "displayName" to displayName,
            "avatarUrl" to avatarUrl,
            "department" to department,
            "statusText" to statusText,
            "isOnline" to isOnline,
            "createdAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        docRef.set(data).addOnFailureListener { exception ->
            handleFirestoreError(exception, OperationType.WRITE, docRef.path)
        }
    }

    fun sendCloudMessage(
        chatId: String,
        messageId: String,
        senderId: String,
        senderUsername: String,
        text: String,
        imageUri: String?
    ) {
        if (auth.currentUser == null) return
        val msgRef = firestore.collection("chats").document(chatId)
            .collection("messages").document(messageId)

        val payload = hashMapOf<String, Any?>(
            "messageId" to messageId,
            "chatId" to chatId,
            "senderId" to senderId,
            "senderUsername" to senderUsername,
            "text" to text,
            "imageUri" to imageUri,
            "createdAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        msgRef.set(payload).addOnFailureListener { exception ->
            handleFirestoreError(exception, OperationType.CREATE, msgRef.path)
        }
    }
}
