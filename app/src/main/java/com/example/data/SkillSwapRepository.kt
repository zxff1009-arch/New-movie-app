package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SkillSwapRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun sanitizeId(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        return cleaned.take(120).ifBlank { UUID.randomUUID().toString().replace("-", "_") }
    }

    // --- REALTIME OBSERVATION STREAMS ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error -> handleStreamError(error, OperationType.GET, path, userId) }
        )
    }

    fun observePartners(userId: String): Flow<List<MentorPartner>> = flow {
        val path = "users/$userId/partners"
        emitAll(
            db.collection("users").document(userId).collection("partners")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        MentorPartner::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
        )
    }

    fun observeConversations(userId: String): Flow<List<Conversation>> = flow {
        val path = "users/$userId/conversations"
        emitAll(
            db.collection("users").document(userId).collection("conversations")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        Conversation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
        )
    }

    fun observeMessages(userId: String, conversationId: String): Flow<List<ChatMessage>> = flow {
        val path = "users/$userId/conversations/$conversationId/messages"
        emitAll(
            db.collection("users").document(userId)
                .collection("conversations").document(conversationId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
        )
    }

    fun observeSessions(userId: String): Flow<List<LearningSession>> = flow {
        val path = "users/$userId/sessions"
        emitAll(
            db.collection("users").document(userId).collection("sessions")
                .orderBy("scheduledDate", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        LearningSession::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
        )
    }

    fun observeRoadmaps(userId: String): Flow<List<LearningRoadmap>> = flow {
        val path = "users/$userId/roadmaps"
        emitAll(
            db.collection("users").document(userId).collection("roadmaps")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        LearningRoadmap::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
        )
    }

    fun observeAchievements(userId: String): Flow<List<Achievement>> = flow {
        val path = "users/$userId/achievements"
        emitAll(
            db.collection("users").document(userId).collection("achievements")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        Achievement::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
        )
    }

    fun observeReviews(userId: String): Flow<List<Review>> = flow {
        val path = "users/$userId/reviews"
        emitAll(
            db.collection("users").document(userId).collection("reviews")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        Review::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
        )
    }

    private fun handleStreamError(
        error: Throwable,
        opType: OperationType,
        path: String,
        uid: String
    ): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, opType, path)
        }
        throw error
    }

    // --- INITIALIZATION & SEEDING ---

    suspend fun ensureUserSeeded(displayNameHint: String): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            val existingUser = userRef.get().await()

            if (!existingUser.exists()) {
                val safeName = displayNameHint.trim().take(80).ifBlank { "SkillSwap Explorer" }
                val userPayload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "bio" to "Curious builder trading mobile engineering skills for creative arts and languages.",
                    "avatarColorHex" to "#7C3AED",
                    "teachSkills" to listOf("Kotlin & Compose", "Python & AI"),
                    "learnSkills" to listOf("UI/UX Design & Figma", "Acoustic Guitar"),
                    "skillLevel" to "Intermediate",
                    "availability" to "Weekday Evenings",
                    "xp" to 250L,
                    "level" to 2L,
                    "streakDays" to 3L,
                    "completedSessionsCount" to 1L,
                    "onboardingCompleted" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                // Create parent user doc first so subcollection exists(/users/{uid}) rule passes cleanly
                userRef.set(userPayload).await()
            }

            // Seed community partners if empty
            val partnersCol = userRef.collection("partners")
            if (partnersCol.limit(1).get().await().isEmpty) {
                val batch = db.batch()
                CommunitySeedData.seedPartners.forEach { p ->
                    val ref = partnersCol.document(p.id)
                    batch.set(
                        ref,
                        mapOf(
                            "userId" to uid,
                            "name" to p.name,
                            "headline" to p.headline,
                            "bio" to p.bio,
                            "avatarInitials" to p.initials,
                            "avatarColorHex" to p.colorHex,
                            "teachSkill" to p.teachSkill,
                            "teachCategory" to p.teachCategory,
                            "teachLevel" to p.teachLevel,
                            "wantSkill" to p.wantSkill,
                            "wantCategory" to p.wantCategory,
                            "availability" to p.availability,
                            "rating" to p.rating,
                            "reviewCount" to p.reviewCount,
                            "xp" to p.xp,
                            "connected" to p.connected,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                // Seed starter conversations
                val convMayaRef = userRef.collection("conversations").document("conv_partner_maya")
                batch.set(
                    convMayaRef,
                    mapOf(
                        "userId" to uid,
                        "partnerId" to "partner_maya",
                        "partnerName" to "Maya Lin",
                        "partnerInitials" to "ML",
                        "partnerColorHex" to "#8B5CF6",
                        "partnerSkill" to "UI/UX Design & Figma",
                        "lastMessage" to "Want to review your Compose screen & trade Figma tokens?",
                        "unreadCount" to 1L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val convMateoRef = userRef.collection("conversations").document("conv_partner_mateo")
                batch.set(
                    convMateoRef,
                    mapOf(
                        "userId" to uid,
                        "partnerId" to "partner_mateo",
                        "partnerName" to "Mateo Alvarez",
                        "partnerInitials" to "MA",
                        "partnerColorHex" to "#3B82F6",
                        "partnerSkill" to "Acoustic Guitar",
                        "lastMessage" to "Proposed: Acoustic Guitar on 2026-11-22",
                        "unreadCount" to 1L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                // Seed starter sessions
                batch.set(
                    userRef.collection("sessions").document("sess_upcoming_1"),
                    mapOf(
                        "userId" to uid,
                        "partnerId" to "partner_maya",
                        "partnerName" to "Maya Lin",
                        "skillTopic" to "Figma Design Tokens & Auto-Layout",
                        "sessionRole" to "learning",
                        "scheduledDate" to "2026-11-20",
                        "scheduledTime" to "18:30",
                        "durationMinutes" to 45L,
                        "status" to "scheduled",
                        "notes" to "Bring dashboard wireframe to iterate on glassmorphic cards.",
                        "xpAwarded" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.set(
                    userRef.collection("sessions").document("sess_completed_1"),
                    mapOf(
                        "userId" to uid,
                        "partnerId" to "partner_mateo",
                        "partnerName" to "Mateo Alvarez",
                        "skillTopic" to "Intro to Python Automation",
                        "sessionRole" to "teaching",
                        "scheduledDate" to "2026-11-16",
                        "scheduledTime" to "19:00",
                        "durationMinutes" to 60L,
                        "status" to "completed",
                        "notes" to "Walked through file sorting script and virtual environments.",
                        "xpAwarded" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                // Seed starter roadmap
                batch.set(
                    userRef.collection("roadmaps").document("road_starter_guitar"),
                    mapOf(
                        "userId" to uid,
                        "skillName" to "Acoustic Guitar",
                        "targetLevel" to "Beginner",
                        "summary" to "A focused 4-milestone foundation covering posture, open chords, clean strumming, and your first full song.",
                        "milestones" to listOf(
                            mapOf(
                                "title" to "Guitar Posture & Tuning",
                                "description" to "Hold the instrument ergonomically and tune all 6 strings by ear or tuner.",
                                "duration" to "Week 1",
                                "exercise" to "Play each open string cleanly 10 times while checking fret-hand thumb placement.",
                                "completed" to true
                            ),
                            mapOf(
                                "title" to "Core Open Chords (G, C, D, Em)",
                                "description" to "Build muscle memory for four versatile chords used in hundreds of songs.",
                                "duration" to "Week 2",
                                "exercise" to "Cycle G -> C -> D -> Em at 60 BPM for 5 minutes without pausing.",
                                "completed" to false
                            ),
                            mapOf(
                                "title" to "Rhythm & Strumming Patterns",
                                "description" to "Lock in a steady down-up groove with muted percussive accents.",
                                "duration" to "Week 3",
                                "exercise" to "Practice the D-DU-UDU pattern on Em for 4 minutes with a metronome.",
                                "completed" to false
                            )
                        ),
                        "completedCount" to 1L,
                        "totalCount" to 3L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                // Seed achievements
                CommunitySeedData.seedAchievements.forEach { ach ->
                    batch.set(
                        userRef.collection("achievements").document(ach.id),
                        mapOf(
                            "userId" to uid,
                            "title" to ach.title,
                            "description" to ach.description,
                            "badgeKey" to ach.badgeKey,
                            "xpReward" to ach.xpReward,
                            "unlocked" to ach.unlocked,
                            "progress" to ach.progress,
                            "target" to ach.target,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                // Seed peer reviews
                batch.set(
                    userRef.collection("reviews").document("rev_mateo"),
                    mapOf(
                        "userId" to uid,
                        "authorName" to "Mateo Alvarez",
                        "authorInitials" to "MA",
                        "skillTopic" to "Python Automation",
                        "rating" to 5L,
                        "comment" to "Super patient mentor! Broke down Python loops and scripts so clearly I automated my audio bounce folder the same night.",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.set(
                    userRef.collection("reviews").document("rev_maya"),
                    mapOf(
                        "userId" to uid,
                        "authorName" to "Maya Lin",
                        "authorInitials" to "ML",
                        "skillTopic" to "Jetpack Compose State",
                        "rating" to 5L,
                        "comment" to "Loved how practical our swap was—turned my Figma card layout into responsive Compose code in 45 minutes.",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                batch.commit().await()

                // Seed messages inside conversations after parent conversation docs exist
                val msgBatch = db.batch()
                msgBatch.set(
                    convMayaRef.collection("messages").document("msg_maya_1"),
                    mapOf(
                        "userId" to uid,
                        "senderId" to "partner_maya",
                        "senderName" to "Maya Lin",
                        "body" to "Hey! Saw you teach Kotlin & Compose. Want to review your screen & trade Figma design tokens this week?",
                        "messageType" to "text",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                msgBatch.set(
                    convMateoRef.collection("messages").document("msg_mateo_1"),
                    mapOf(
                        "userId" to uid,
                        "senderId" to "partner_mateo",
                        "senderName" to "Mateo Alvarez",
                        "body" to "Thanks again for the Python session! Ready for your Acoustic Guitar chord workshop?",
                        "messageType" to "proposal",
                        "proposalSkill" to "Acoustic Guitar",
                        "proposalDate" to "2026-11-22",
                        "proposalTime" to "15:00",
                        "proposalStatus" to "pending",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                msgBatch.commit().await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    // --- USER PROFILE & ONBOARDING ---

    suspend fun updateProfile(
        displayName: String,
        bio: String,
        teachSkills: List<String>,
        learnSkills: List<String>,
        skillLevel: String,
        availability: String,
        onboardingCompleted: Boolean
    ): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
            docPath = ref.path
            ref.update(
                mapOf(
                    "displayName" to displayName.trim().take(80).ifBlank { "SkillSwap Member" },
                    "bio" to bio.trim().take(400),
                    "teachSkills" to teachSkills.map { it.trim() }.filter { it.isNotEmpty() }.take(20),
                    "learnSkills" to learnSkills.map { it.trim() }.filter { it.isNotEmpty() }.take(20),
                    "skillLevel" to skillLevel,
                    "availability" to availability.take(60),
                    "onboardingCompleted" to onboardingCompleted,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- PARTNERS & CONNECTIONS ---

    suspend fun setPartnerConnected(partner: MentorPartner, connected: Boolean): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
                .collection("partners").document(sanitizeId(partner.id))
            docPath = ref.path
            ref.update(
                mapOf(
                    "connected" to connected,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (connected) {
                ensureConversationForPartner(uid, partner)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun ensureConversation(partner: MentorPartner): Result<String> {
        return try {
            val uid = requireUserId()
            val convId = ensureConversationForPartner(uid, partner)
            Result.success(convId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/conversations")
            Result.failure(e)
        }
    }

    private suspend fun ensureConversationForPartner(uid: String, partner: MentorPartner): String {
        val convId = sanitizeId("conv_${partner.id}")
        val convRef = db.collection("users").document(uid)
            .collection("conversations").document(convId)
        val snap = convRef.get().await()
        if (!snap.exists()) {
            convRef.set(
                mapOf(
                    "userId" to uid,
                    "partnerId" to partner.id,
                    "partnerName" to partner.name,
                    "partnerInitials" to partner.avatarInitials,
                    "partnerColorHex" to partner.avatarColorHex,
                    "partnerSkill" to partner.teachSkill,
                    "lastMessage" to "Connected! Say hello to start your skill swap.",
                    "unreadCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            convRef.collection("messages").document("msg_intro").set(
                mapOf(
                    "userId" to uid,
                    "senderId" to partner.id,
                    "senderName" to partner.name,
                    "body" to "Excited to connect! I'd love to learn ${partner.wantSkill} and help you with ${partner.teachSkill}.",
                    "messageType" to "text",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        return convId
    }

    // --- MESSAGING & PROPOSALS ---

    suspend fun markConversationRead(conversationId: String): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
                .collection("conversations").document(sanitizeId(conversationId))
            docPath = ref.path
            ref.update(
                mapOf(
                    "unreadCount" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        conversationId: String,
        senderName: String,
        body: String
    ): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val cleanBody = body.trim().take(1000)
            require(cleanBody.isNotEmpty()) { "Message cannot be empty" }
            val convRef = db.collection("users").document(uid)
                .collection("conversations").document(sanitizeId(conversationId))
            val msgRef = convRef.collection("messages").document()
            docPath = msgRef.path
            val batch = db.batch()
            batch.set(
                msgRef,
                mapOf(
                    "userId" to uid,
                    "senderId" to uid,
                    "senderName" to senderName.trim().take(80).ifBlank { "You" },
                    "body" to cleanBody,
                    "messageType" to "text",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                convRef,
                mapOf(
                    "lastMessage" to cleanBody.take(500),
                    "unreadCount" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun sendSessionProposal(
        conversationId: String,
        senderName: String,
        skill: String,
        date: String,
        time: String,
        note: String
    ): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val cleanSkill = skill.trim().take(80).ifBlank { "Skill Swap" }
            val cleanDate = date.trim().take(40)
            val cleanTime = time.trim().take(40)
            val body = note.trim().take(1000).ifBlank {
                "Proposed a $cleanSkill session on $cleanDate at $cleanTime"
            }
            val convRef = db.collection("users").document(uid)
                .collection("conversations").document(sanitizeId(conversationId))
            val msgRef = convRef.collection("messages").document()
            docPath = msgRef.path
            val batch = db.batch()
            batch.set(
                msgRef,
                mapOf(
                    "userId" to uid,
                    "senderId" to uid,
                    "senderName" to senderName.trim().take(80).ifBlank { "You" },
                    "body" to body,
                    "messageType" to "proposal",
                    "proposalSkill" to cleanSkill,
                    "proposalDate" to cleanDate,
                    "proposalTime" to cleanTime,
                    "proposalStatus" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                convRef,
                mapOf(
                    "lastMessage" to "Proposed: $cleanSkill on $cleanDate".take(500),
                    "unreadCount" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun respondToProposal(
        conversation: Conversation,
        message: ChatMessage,
        accept: Boolean
    ): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val msgRef = db.collection("users").document(uid)
                .collection("conversations").document(sanitizeId(conversation.id))
                .collection("messages").document(sanitizeId(message.id))
            docPath = msgRef.path
            val newStatus = if (accept) "accepted" else "declined"
            val batch = db.batch()
            batch.update(
                msgRef,
                mapOf(
                    "proposalStatus" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (accept) {
                val sessRef = db.collection("users").document(uid).collection("sessions").document()
                batch.set(
                    sessRef,
                    mapOf(
                        "userId" to uid,
                        "partnerId" to conversation.partnerId,
                        "partnerName" to conversation.partnerName,
                        "skillTopic" to (message.proposalSkill ?: conversation.partnerSkill).take(100),
                        "sessionRole" to "learning",
                        "scheduledDate" to (message.proposalDate ?: "2026-11-23").take(40),
                        "scheduledTime" to (message.proposalTime ?: "18:00").take(40),
                        "durationMinutes" to 45L,
                        "status" to "scheduled",
                        "notes" to "Confirmed via chat proposal.",
                        "xpAwarded" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- LEARNING SESSIONS & GAMIFICATION ---

    suspend fun scheduleSession(
        partnerId: String,
        partnerName: String,
        skillTopic: String,
        sessionRole: String,
        scheduledDate: String,
        scheduledTime: String,
        durationMinutes: Long,
        notes: String
    ): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val sessRef = db.collection("users").document(uid).collection("sessions").document()
            docPath = sessRef.path
            sessRef.set(
                mapOf(
                    "userId" to uid,
                    "partnerId" to partnerId.take(128),
                    "partnerName" to partnerName.trim().take(80),
                    "skillTopic" to skillTopic.trim().take(100),
                    "sessionRole" to if (sessionRole == "teaching") "teaching" else "learning",
                    "scheduledDate" to scheduledDate.trim().take(40),
                    "scheduledTime" to scheduledTime.trim().take(40),
                    "durationMinutes" to durationMinutes.coerceIn(15L, 240L),
                    "status" to "scheduled",
                    "notes" to notes.trim().take(1000),
                    "xpAwarded" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateSessionNotes(sessionId: String, notes: String): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
                .collection("sessions").document(sanitizeId(sessionId))
            docPath = ref.path
            ref.update(
                mapOf(
                    "notes" to notes.trim().take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun completeSession(
        session: LearningSession,
        currentProfile: UserProfile,
        notes: String,
        achievements: List<Achievement>
    ): Result<Long> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            val sessRef = userRef.collection("sessions").document(sanitizeId(session.id))
            docPath = sessRef.path

            val xpEarned = 150L
            val newCompletedCount = currentProfile.completedSessionsCount + 1L
            val newStreak = currentProfile.streakDays + 1L
            var totalNewXp = currentProfile.xp + xpEarned
            val batch = db.batch()

            batch.update(
                sessRef,
                mapOf(
                    "status" to "completed",
                    "xpAwarded" to true,
                    "notes" to notes.trim().take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // Unlock or advance relevant achievements
            achievements.forEach { ach ->
                val achRef = userRef.collection("achievements").document(sanitizeId(ach.id))
                when (ach.badgeKey) {
                    "mentor_5" -> {
                        val nextProg = newCompletedCount.coerceAtMost(ach.target)
                        val unlocked = nextProg >= ach.target
                        if (unlocked && !ach.unlocked) totalNewXp += ach.xpReward
                        batch.update(
                            achRef,
                            mapOf(
                                "progress" to nextProg,
                                "unlocked" to unlocked,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    "streak_5" -> {
                        val nextProg = newStreak.coerceAtMost(ach.target)
                        val unlocked = nextProg >= ach.target
                        if (unlocked && !ach.unlocked) totalNewXp += ach.xpReward
                        batch.update(
                            achRef,
                            mapOf(
                                "progress" to nextProg,
                                "unlocked" to unlocked,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }

            val newLevel = (totalNewXp / 200L) + 1L
            batch.update(
                userRef,
                mapOf(
                    "xp" to totalNewXp,
                    "level" to newLevel,
                    "streakDays" to newStreak,
                    "completedSessionsCount" to newCompletedCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(xpEarned)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun cancelSession(sessionId: String): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
                .collection("sessions").document(sanitizeId(sessionId))
            docPath = ref.path
            ref.update(
                mapOf(
                    "status" to "cancelled",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- AI ROADMAPS ---

    suspend fun saveRoadmap(roadmap: LearningRoadmap): Result<String> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("roadmaps").document()
            docPath = ref.path
            val milestoneMaps = roadmap.milestones.take(10).map { m ->
                mapOf(
                    "title" to m.title.take(120),
                    "description" to m.description.take(300),
                    "duration" to m.duration.take(40),
                    "exercise" to m.exercise.take(300),
                    "completed" to m.completed
                )
            }
            val completed = milestoneMaps.count { it["completed"] == true }.toLong()
            ref.set(
                mapOf(
                    "userId" to uid,
                    "skillName" to roadmap.skillName.take(80),
                    "targetLevel" to roadmap.targetLevel.take(40),
                    "summary" to roadmap.summary.take(600),
                    "milestones" to milestoneMaps,
                    "completedCount" to completed,
                    "totalCount" to milestoneMaps.size.toLong().coerceAtLeast(1L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleRoadmapMilestone(roadmap: LearningRoadmap, milestoneIndex: Int): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
                .collection("roadmaps").document(sanitizeId(roadmap.id))
            docPath = ref.path
            val updatedMilestones = roadmap.milestones.mapIndexed { idx, m ->
                val toggled = if (idx == milestoneIndex) m.copy(completed = !m.completed) else m
                mapOf(
                    "title" to toggled.title,
                    "description" to toggled.description,
                    "duration" to toggled.duration,
                    "exercise" to toggled.exercise,
                    "completed" to toggled.completed
                )
            }
            val completedCount = updatedMilestones.count { it["completed"] == true }.toLong()
            ref.update(
                mapOf(
                    "milestones" to updatedMilestones,
                    "completedCount" to completedCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteRoadmap(roadmapId: String): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
                .collection("roadmaps").document(sanitizeId(roadmapId))
            docPath = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
