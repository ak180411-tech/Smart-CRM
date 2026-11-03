const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
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

test("Unauthenticated user: cannot read leads or projects", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("leads").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("projects").get());
});

test("Cross-user isolation: Bob cannot read Alice's leads or projects", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("leads").doc("lead_1").set({
      userId: ALICE_UID,
      customerName: "Alice Lead",
      phone: "555-123-4567",
      pipelineStage: "NEW_LEAD",
      createdAt: new Date(),
    });
    await context.firestore().collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").set({
      userId: ALICE_UID,
      customerName: "Alice Project",
      phone: "555-123-4567",
      projectStage: "SITE_AUDIT",
      totalContractPrice: 25000,
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("leads").doc("lead_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("leads").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").get());
});

test("Authenticated owner: can create and read their own lead and project", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("leads").doc("lead_1").set({
      userId: ALICE_UID,
      customerName: "John Doe",
      phone: "555-019-2834",
      pipelineStage: "NEW_LEAD",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").set({
      userId: ALICE_UID,
      customerName: "Jane Smith",
      phone: "555-019-9988",
      projectStage: "SITE_AUDIT",
      totalContractPrice: 32000,
      createdAt: new Date(),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("leads").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("projects").get());
});

test("Validation rejects invalid payloads (missing required fields or invalid types)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Missing phone
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("leads").doc("lead_bad").set({
      userId: ALICE_UID,
      customerName: "Bad Lead",
      pipelineStage: "NEW_LEAD",
      createdAt: new Date(),
    })
  );

  // Negative contract price
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_bad").set({
      userId: ALICE_UID,
      customerName: "Bad Project",
      phone: "555-019-9988",
      projectStage: "SITE_AUDIT",
      totalContractPrice: -500,
      createdAt: new Date(),
    })
  );
});
