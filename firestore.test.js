const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
});

test("Authenticated user: can create own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice_sudais",
      email: "alice@sudais.com",
      displayName: "Alice Smith",
      department: "Finance",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: cannot impersonate another user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "bob_sudais",
      email: "bob@sudais.com",
      displayName: "Bob Jones",
      department: "Legal",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: can create chat and post message", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const chatId = "chat_alice_bob";

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      chatId: chatId,
      isGroup: false,
      title: "Direct Chat",
      creatorId: ALICE_UID,
      members: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").set({
      messageId: "msg_1",
      chatId: chatId,
      senderId: ALICE_UID,
      senderUsername: "alice_sudais",
      text: "Hello Bob!",
      createdAt: new Date(),
    })
  );
});
