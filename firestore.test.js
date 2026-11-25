const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

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

test("Unauthenticated user cannot read or write accounts, rooms, or messages", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("studyRooms").where("visibility", "==", "shared").get());
  await assertFails(
    unauthDb.collection("studyRooms").doc("general_study").collection("messages")
      .where("roomId", "==", "general_study")
      .get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Sokha",
      gradeLevel: "Grade 12",
      passcodeHash: "a".repeat(44),
      passcodeSalt: "b".repeat(24),
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("UserAccount enforces strict owner isolation, valid schema, and immutable createdAt", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Valid create payload (Kotlin parity)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "សុខា",
      gradeLevel: "ថ្នាក់ទី ១២",
      passcodeHash: "h".repeat(44),
      passcodeSalt: "s".repeat(24),
      createdAt: created,
      updatedAt: created,
    })
  );

  // Bob cannot read or overwrite Alice's account (protects passcodeHash/passcodeSalt)
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Ghost/shadow field is rejected
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Dara",
      gradeLevel: "Grade 11",
      passcodeHash: "h".repeat(44),
      passcodeSalt: "s".repeat(24),
      isAdmin: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid partial update preserves createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "សុខា ធី",
      gradeLevel: "ថ្នាក់ទី ១២ A",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Mutating createdAt fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("StudyRooms require visibility filter on list and enforce parent existence for messages", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  // Creating message before room exists fails (parent gate)
  await assertFails(
    aliceDb.collection("studyRooms").doc("missing_room").collection("messages").doc("msg_1").set({
      roomId: "missing_room",
      senderId: ALICE_UID,
      senderName: "សុខា",
      senderGrade: "ថ្នាក់ទី ១២",
      text: "Hello?",
      createdAt: now,
    })
  );

  // Alice creates a shared study room
  await assertSucceeds(
    aliceDb.collection("studyRooms").doc("math_qa").set({
      creatorId: ALICE_UID,
      name: "គណិតវិទ្យា",
      description: "សួរដេញដោលលំហាត់",
      subjectTag: "គណិតវិទ្យា",
      visibility: "shared",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered query succeeds; unfiltered query fails
  await assertSucceeds(aliceDb.collection("studyRooms").where("visibility", "==", "shared").get());
  await assertFails(aliceDb.collection("studyRooms").get());

  // Bob posts a message with an attached lesson reference
  await assertSucceeds(
    bobDb.collection("studyRooms").doc("math_qa").collection("messages").doc("msg_1").set({
      roomId: "math_qa",
      senderId: BOB_UID,
      senderName: "តារា",
      senderGrade: "ថ្នាក់ទី ១១",
      text: "សូមជួយពន្យល់រូបមន្តអាំងតេក្រាលនេះផង",
      attachedLessonTitle: "ជំពូកទី ១៖ អាំងតេក្រាល",
      attachedLessonType: "PDF",
      attachedLessonSubject: "គណិតវិទ្យា",
      attachedLessonNote: "សង្ខេបរូបមន្ត",
      createdAt: now,
    })
  );

  // Bob updates room preview fields (allowed), cannot alter creatorId or room name
  await assertSucceeds(
    bobDb.collection("studyRooms").doc("math_qa").update({
      lastMessageText: "សូមជួយពន្យល់រូបមន្តអាំងតេក្រាលនេះផង",
      lastSenderName: "តារា",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    bobDb.collection("studyRooms").doc("math_qa").update({
      name: "Renamed by non-creator",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Filtered messages query succeeds; unfiltered messages query fails
  await assertSucceeds(
    aliceDb.collection("studyRooms").doc("math_qa").collection("messages")
      .where("roomId", "==", "math_qa")
      .get()
  );
  await assertFails(
    aliceDb.collection("studyRooms").doc("math_qa").collection("messages").get()
  );

  // Alice cannot impersonate Bob or delete Bob's message
  await assertFails(
    aliceDb.collection("studyRooms").doc("math_qa").collection("messages").doc("msg_2").set({
      roomId: "math_qa",
      senderId: BOB_UID,
      senderName: "តារា",
      senderGrade: "ថ្នាក់ទី ១១",
      text: "Spoofed message",
      createdAt: now,
    })
  );
  await assertFails(
    aliceDb.collection("studyRooms").doc("math_qa").collection("messages").doc("msg_1").delete()
  );
  await assertSucceeds(
    bobDb.collection("studyRooms").doc("math_qa").collection("messages").doc("msg_1").delete()
  );
});

test("StudentDirectory exposes public student cards only to signed-in users with owner-only writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertFails(
    unauthDb.collection("studentDirectory").where("visibility", "==", "students").get()
  );

  await assertSucceeds(
    aliceDb.collection("studentDirectory").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "សុខា",
      gradeLevel: "ថ្នាក់ទី ១២",
      studentCode: "ALICEU",
      visibility: "students",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    bobDb.collection("studentDirectory").where("visibility", "==", "students").get()
  );
  await assertFails(
    bobDb.collection("studentDirectory").doc(ALICE_UID).update({
      displayName: "Tampered",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("FriendConnections enforce mutual request/accept/decline and gate private 1-on-1 lesson chat", async () => {
  const CHARLIE_UID = "charlie_uid_789";
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date(Date.now() - 5000);
  const pairId = `${ALICE_UID}_${BOB_UID}`;
  const participants = [ALICE_UID, BOB_UID];

  // Unsorted / invalid pairId or self-friend request fails
  await assertFails(
    aliceDb.collection("friendConnections").doc(`${ALICE_UID}_${ALICE_UID}`).set({
      participantIds: [ALICE_UID, ALICE_UID],
      requesterId: ALICE_UID,
      requesterName: "សុខា",
      requesterGrade: "ថ្នាក់ទី ១២",
      recipientId: ALICE_UID,
      recipientName: "សុខា",
      recipientGrade: "ថ្នាក់ទី ១២",
      status: "pending",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice cannot pre-accept her own friend request
  await assertFails(
    aliceDb.collection("friendConnections").doc(pairId).set({
      participantIds: participants,
      requesterId: ALICE_UID,
      requesterName: "សុខា",
      requesterGrade: "ថ្នាក់ទី ១២",
      recipientId: BOB_UID,
      recipientName: "តារា",
      recipientGrade: "ថ្នាក់ទី ១១",
      status: "accepted",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice sends valid pending friend request to Bob
  await assertSucceeds(
    aliceDb.collection("friendConnections").doc(pairId).set({
      participantIds: participants,
      requesterId: ALICE_UID,
      requesterName: "សុខា",
      requesterGrade: "ថ្នាក់ទី ១២",
      recipientId: BOB_UID,
      recipientName: "តារា",
      recipientGrade: "ថ្នាក់ទី ១១",
      status: "pending",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Direct messages are blocked while friend request is still pending
  await assertFails(
    aliceDb.collection("friendConnections").doc(pairId).collection("messages").doc("dm_0").set({
      connectionId: pairId,
      participantIds: participants,
      senderId: ALICE_UID,
      senderName: "សុខា",
      senderGrade: "ថ្នាក់ទី ១២",
      text: "Hello before accept",
      createdAt: now,
    })
  );

  // Alice cannot force-accept her own outgoing request; Charlie cannot read or accept
  await assertFails(
    aliceDb.collection("friendConnections").doc(pairId).update({
      status: "accepted",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    charlieDb.collection("friendConnections").doc(pairId).get()
  );

  // Bob (recipient) accepts the friend request
  await assertSucceeds(
    bobDb.collection("friendConnections").doc(pairId).update({
      status: "accepted",
      recipientName: "តារា",
      recipientGrade: "ថ្នាក់ទី ១១",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Now Alice & Bob can exchange 1-on-1 direct lesson messages
  await assertSucceeds(
    aliceDb.collection("friendConnections").doc(pairId).collection("messages").doc("dm_1").set({
      connectionId: pairId,
      participantIds: participants,
      senderId: ALICE_UID,
      senderName: "សុខា",
      senderGrade: "ថ្នាក់ទី ១២",
      text: "តារា ជួយមើលមេរៀនរូបវិទ្យានេះបន្តិច!",
      attachedLessonTitle: "ច្បាប់ញូតុន",
      attachedLessonType: "PDF",
      attachedLessonSubject: "រូបវិទ្យា",
      createdAt: new Date(Date.now() - 500),
    })
  );

  // Bob reads DM with single-field participantIds filter; Charlie is denied get & unfiltered list
  await assertSucceeds(
    bobDb.collection("friendConnections").doc(pairId).collection("messages")
      .where("participantIds", "array-contains", BOB_UID)
      .get()
  );
  await assertFails(
    charlieDb.collection("friendConnections").doc(pairId).collection("messages").doc("dm_1").get()
  );
  await assertFails(
    charlieDb.collection("friendConnections").doc(pairId).collection("messages").get()
  );

  // Bob updates preview on accepted friend connection
  await assertSucceeds(
    bobDb.collection("friendConnections").doc(pairId).update({
      lastMessageText: "យល់ហើយ!",
      lastSenderName: "តារា",
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

