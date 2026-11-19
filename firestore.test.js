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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTs(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

function futureTs(offsetMs = 60000) {
  return new Date(Date.now() + offsetMs);
}

function validUserPayload(uid) {
  const ts = pastTs(10000);
  return {
    userId: uid,
    displayName: "Alice Learner",
    bio: "Full-stack dev trading Kotlin for Acoustic Guitar.",
    avatarColorHex: "#7C3AED",
    teachSkills: ["Kotlin", "Jetpack Compose"],
    learnSkills: ["Acoustic Guitar", "Spanish"],
    skillLevel: "Intermediate",
    availability: "Weekday Evenings",
    xp: 250,
    level: 2,
    streakDays: 3,
    completedSessionsCount: 1,
    onboardingCompleted: true,
    createdAt: ts,
    updatedAt: ts,
  };
}

async function seedAliceProfile() {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .set(validUserPayload(ALICE_UID));
  });
}

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

test("Unauthenticated user: cannot read or write any collection", async () => {
  await seedAliceProfile();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("partners").get()
  );
  await assertFails(
    unauthDb.collection("users").doc("unauth_1").set(validUserPayload("unauth_1"))
  );
});

test("Authenticated owner: valid UserProfile create and action-based updates succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID))
  );

  // Action: edit profile fields
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      bio: "Updated bio for SkillSwap!",
      teachSkills: ["Kotlin", "Firebase"],
      updatedAt: pastTs(1000),
    })
  );

  // Action: award XP & streak
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      xp: 400,
      level: 3,
      streakDays: 4,
      completedSessionsCount: 2,
      updatedAt: pastTs(500),
    })
  );

  // Shadow update with ghost field must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      bio: "Ghost",
      isAdmin: true,
      updatedAt: pastTs(200),
    })
  );

  // Tampered future timestamp must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      bio: "Future ts",
      updatedAt: futureTs(),
    })
  );

  // Immutable createdAt mutation must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: pastTs(100),
      updatedAt: pastTs(100),
    })
  );
});

test("Cross-user isolation: Bob cannot read or mutate Alice's workspace", async () => {
  await seedAliceProfile();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      bio: "Hijacked",
      updatedAt: pastTs(1000),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("sessions").get()
  );
});

test("Partners, Conversations, Messages & Proposals: full lifecycle and terminal state locks", async () => {
  await seedAliceProfile();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastTs(5000);

  // Partner create + connect toggle
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("partners")
      .doc("partner_maya")
      .set({
        userId: ALICE_UID,
        name: "Maya Lin",
        headline: "Staff Product Designer",
        bio: "Love trading Figma systems for Kotlin.",
        avatarInitials: "ML",
        avatarColorHex: "#8B5CF6",
        teachSkill: "UI/UX Design & Figma",
        teachCategory: "Design",
        teachLevel: "Expert",
        wantSkill: "Kotlin",
        wantCategory: "Programming",
        availability: "Weekday Evenings",
        rating: 4.9,
        reviewCount: 28,
        xp: 1840,
        connected: false,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("partners")
      .doc("partner_maya")
      .update({ connected: true, updatedAt: pastTs(1000) })
  );

  // Conversation + Proposal message
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .doc("conv_maya")
      .set({
        userId: ALICE_UID,
        partnerId: "partner_maya",
        partnerName: "Maya Lin",
        partnerInitials: "ML",
        partnerColorHex: "#8B5CF6",
        partnerSkill: "UI/UX Design & Figma",
        lastMessage: "Proposed a session",
        unreadCount: 0,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .doc("conv_maya")
      .collection("messages")
      .doc("msg_1")
      .set({
        userId: ALICE_UID,
        senderId: ALICE_UID,
        senderName: "Alice Learner",
        body: "Let's swap Figma & Kotlin on Thursday!",
        messageType: "proposal",
        proposalSkill: "UI/UX Design & Figma",
        proposalDate: "2026-11-20",
        proposalTime: "19:00",
        proposalStatus: "pending",
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Accept proposal succeeds once
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .doc("conv_maya")
      .collection("messages")
      .doc("msg_1")
      .update({
        proposalStatus: "accepted",
        updatedAt: pastTs(500),
      })
  );

  // Subsequent change after non-pending status must fail
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .doc("conv_maya")
      .collection("messages")
      .doc("msg_1")
      .update({
        proposalStatus: "declined",
        updatedAt: pastTs(200),
      })
  );
});

test("Sessions, Roadmaps, Achievements & Reviews: schema validation and terminal lock", async () => {
  await seedAliceProfile();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastTs(5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("sess_1")
      .set({
        userId: ALICE_UID,
        partnerId: "partner_maya",
        partnerName: "Maya Lin",
        skillTopic: "Figma Auto-Layout",
        sessionRole: "learning",
        scheduledDate: "2026-11-20",
        scheduledTime: "19:00",
        durationMinutes: 45,
        status: "scheduled",
        notes: "Bring portfolio wireframes.",
        xpAwarded: false,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Complete session
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("sess_1")
      .update({
        status: "completed",
        xpAwarded: true,
        notes: "Covered components and tokens!",
        updatedAt: pastTs(500),
      })
  );

  // Terminal state lock: cannot update completed session
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("sess_1")
      .update({
        status: "scheduled",
        updatedAt: pastTs(200),
      })
  );

  // Roadmap create + milestone toggle update
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("roadmaps")
      .doc("road_1")
      .set({
        userId: ALICE_UID,
        skillName: "Acoustic Guitar",
        targetLevel: "Beginner",
        summary: "4-week chord & rhythm foundation",
        milestones: [
          {
            title: "Open Chords",
            description: "Learn G, C, D, Em",
            duration: "Week 1",
            exercise: "Switch G-C-D cleanly for 5 mins",
            completed: false,
          },
        ],
        completedCount: 0,
        totalCount: 1,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("roadmaps")
      .doc("road_1")
      .update({
        milestones: [
          {
            title: "Open Chords",
            description: "Learn G, C, D, Em",
            duration: "Week 1",
            exercise: "Switch G-C-D cleanly for 5 mins",
            completed: true,
          },
        ],
        completedCount: 1,
        updatedAt: pastTs(200),
      })
  );

  // Achievement & Review
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("achievements")
      .doc("ach_1")
      .set({
        userId: ALICE_UID,
        title: "First Swap",
        description: "Complete your first peer learning session.",
        badgeKey: "first_swap",
        xpReward: 100,
        unlocked: false,
        progress: 0,
        target: 1,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("reviews")
      .doc("rev_1")
      .set({
        userId: ALICE_UID,
        authorName: "Maya Lin",
        authorInitials: "ML",
        skillTopic: "Kotlin Coroutines",
        rating: 5,
        comment: "Super clear explanation of Flow operators!",
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // @DocumentId `id` key injection must fail
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("sess_bad")
      .set({
        id: "sess_bad",
        userId: ALICE_UID,
        partnerId: "partner_maya",
        partnerName: "Maya Lin",
        skillTopic: "Figma",
        sessionRole: "learning",
        scheduledDate: "2026-11-21",
        scheduledTime: "18:00",
        durationMinutes: 45,
        status: "scheduled",
        notes: "",
        xpAwarded: false,
        createdAt: ts,
        updatedAt: ts,
      })
  );
});
