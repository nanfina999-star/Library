package com.example.data.cloud

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StudyCloudRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id),
        ),
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserAccount(userId: String): Flow<UserAccount?> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                },
        )
    }

    suspend fun saveUserAccount(
        userId: String,
        displayName: String,
        gradeLevel: String,
        password: String,
        existingAccount: UserAccount?,
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            val trimmedName = displayName.trim()
            val trimmedGrade = gradeLevel.trim()
            require(trimmedName.length in 2..40) { "ឈ្មោះត្រូវមានពី ២ ទៅ ៤០ តួអក្សរ" }
            require(trimmedGrade.length in 1..30) { "សូមបញ្ចូលថ្នាក់ទីប៉ុន្មាន (១-៣០ តួអក្សរ)" }

            val docRef = db.collection("users").document(userId)
            if (existingAccount == null) {
                require(password.length >= 4) { "ពាក្យសម្ងាត់ត្រូវមានយ៉ាងតិច ៤ តួអក្សរ" }
                val salted = PasscodeHasher.hashNewPasscode(password)
                val payload = buildUserAccountCreateMap(
                    userId = uid,
                    displayName = trimmedName,
                    gradeLevel = trimmedGrade,
                    passcodeHash = salted.hashHex,
                    passcodeSalt = salted.saltHex,
                )
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to trimmedName,
                    "gradeLevel" to trimmedGrade,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                if (password.isNotEmpty()) {
                    require(password.length >= 4) { "ពាក្យសម្ងាត់ថ្មីត្រូវមានយ៉ាងតិច ៤ តួអក្សរ" }
                    val salted = PasscodeHasher.hashNewPasscode(password)
                    updates["passcodeHash"] = salted.hashHex
                    updates["passcodeSalt"] = salted.saltHex
                }
                docRef.update(updates).await()
            }
            upsertStudentDirectoryProfile(uid, trimmedName, trimmedGrade)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (existingAccount == null) OperationType.CREATE else OperationType.UPDATE,
                docPath,
            )
            Result.failure(e)
        }
    }

    suspend fun syncStudentDirectoryProfile(account: UserAccount): Result<Unit> {
        val docPath = "studentDirectory/${account.userId}"
        return try {
            val uid = requireUserId()
            check(uid == account.userId) { "Signed-in user mismatch" }
            upsertStudentDirectoryProfile(uid, account.displayName.trim(), account.gradeLevel.trim())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun upsertStudentDirectoryProfile(
        uid: String,
        displayName: String,
        gradeLevel: String,
    ) {
        val dirCollection = db.collection("studentDirectory")
        val existingEntries = dirCollection
            .whereEqualTo("visibility", "students")
            .get()
            .await()
        val alreadyExists = existingEntries.documents.any { it.id == uid }
        val dirRef = dirCollection.document(uid)
        if (alreadyExists) {
            dirRef.update(
                mapOf(
                    "displayName" to displayName,
                    "gradeLevel" to gradeLevel,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        } else {
            dirRef.set(
                buildStudentProfileCreateMap(
                    userId = uid,
                    displayName = displayName,
                    gradeLevel = gradeLevel,
                ),
            ).await()
        }
    }

    fun verifyUserPasscode(account: UserAccount, password: String): Boolean =
        PasscodeHasher.verifyPasscode(
            passcode = password,
            saltHex = account.passcodeSalt,
            expectedHashHex = account.passcodeHash,
        )

    fun observeStudyRooms(): Flow<List<StudyRoom>> = flow {
        val uid = requireUserId()
        val path = "studyRooms"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "shared")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(StudyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                },
        )
    }

    suspend fun ensureDefaultStudyRooms(): Result<Unit> {
        val path = "studyRooms"
        return try {
            val uid = requireUserId()
            val existing = db.collection(path)
                .whereEqualTo("visibility", "shared")
                .limit(1)
                .get()
                .await()
            if (!existing.isEmpty) {
                return Result.success(Unit)
            }

            val defaults = listOf(
                Triple("general_study", "បន្ទប់ជជែករួម", "សួរដេញដោលមេរៀន និងឯកសារទូទៅ") to "ទូទៅ",
                Triple("math_qa", "គណិតវិទ្យា (Q&A)", "ជជែកដោះស្រាយលំហាត់ និងរូបមន្តគណិតវិទ្យា") to "គណិតវិទ្យា",
                Triple("history_qa", "ប្រវត្តិសាស្ត្រ និងសង្គម", "សួរសំណួរអំពីមេរៀនប្រវត្តិសាស្ត្រ និងឯកសារ") to "ប្រវត្តិសាស្ត្រ",
            )
            defaults.forEach { (meta, subjectTag) ->
                val (roomId, name, desc) = meta
                val payload = buildStudyRoomCreateMap(
                    creatorId = uid,
                    name = name,
                    description = desc,
                    subjectTag = subjectTag,
                )
                db.collection(path).document(roomId).set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createStudyRoom(
        name: String,
        description: String,
        subjectTag: String,
    ): Result<String> {
        val collectionRef = db.collection("studyRooms")
        var docPath = "studyRooms"
        return try {
            val uid = requireUserId()
            val trimmedName = name.trim()
            val trimmedDesc = description.trim().take(160)
            val trimmedTag = subjectTag.trim().ifBlank { "ទូទៅ" }.take(30)
            require(trimmedName.length in 2..60) { "ឈ្មោះបន្ទប់ត្រូវមានពី ២ ទៅ ៦០ តួអក្សរ" }

            val docRef = collectionRef.document()
            docPath = docRef.path
            val payload = buildStudyRoomCreateMap(
                creatorId = uid,
                name = trimmedName,
                description = trimmedDesc,
                subjectTag = trimmedTag,
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    fun observeRoomMessages(roomId: String): Flow<List<ChatMessage>> = flow {
        require(roomId.isNotBlank()) { "Room ID must not be blank" }
        val uid = requireUserId()
        val path = "studyRooms/$roomId/messages"
        emitAll(
            db.collection("studyRooms")
                .document(roomId)
                .collection("messages")
                .whereEqualTo("roomId", roomId)
                .limit(200)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp.now() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                },
        )
    }

    suspend fun sendMessage(
        roomId: String,
        account: UserAccount,
        text: String,
        attachment: LessonAttachmentDraft?,
        voice: VoiceAttachmentDraft? = null,
    ): Result<Unit> {
        var docPath = "studyRooms/$roomId/messages"
        return try {
            val uid = requireUserId()
            require(roomId.isNotBlank()) { "Room ID must not be blank" }
            val trimmedInput = text.trim()
            val effectiveText = if (trimmedInput.isNotBlank()) {
                trimmedInput
            } else if (voice != null && voice.audioBase64.isNotBlank()) {
                "$VOICE_DEFAULT_LABEL_PREFIX (${voice.durationSec} វិនាទី)"
            } else {
                ""
            }
            require(effectiveText.length in 1..1000) { "សារជជែកត្រូវមានពី ១ ទៅ ១០០០ តួអក្សរ" }

            val roomRef = db.collection("studyRooms").document(roomId)
            val msgRef = roomRef.collection("messages").document()
            docPath = msgRef.path

            val payload = buildChatMessageCreateMap(
                roomId = roomId,
                senderId = uid,
                senderName = account.displayName.take(40),
                senderGrade = account.gradeLevel.take(30),
                text = effectiveText,
                attachment = attachment,
                voice = voice,
            )
            msgRef.set(payload).await()

            val previewText = buildString {
                if (attachment != null) {
                    append("[${attachment.title.take(30)}] ")
                }
                if (voice != null) {
                    append("$VOICE_DEFAULT_LABEL_PREFIX (${voice.durationSec}វិ) ")
                    if (trimmedInput.isNotBlank()) append(trimmedInput)
                } else {
                    append(effectiveText)
                }
            }.trim().take(200)

            roomRef.update(
                mapOf(
                    "lastMessageText" to previewText,
                    "lastSenderName" to account.displayName.take(40),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(roomId: String, messageId: String): Result<Unit> {
        val docPath = "studyRooms/$roomId/messages/$messageId"
        return try {
            requireUserId()
            require(roomId.isNotBlank() && messageId.isNotBlank()) { "IDs must not be blank" }
            db.collection("studyRooms")
                .document(roomId)
                .collection("messages")
                .document(messageId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    fun observeStudentDirectory(): Flow<List<StudentProfile>> = flow {
        val uid = requireUserId()
        val path = "studentDirectory"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "students")
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(StudentProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.displayName.lowercase() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                },
        )
    }

    fun observeFriendConnections(userId: String): Flow<List<FriendConnection>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "friendConnections"
        emitAll(
            db.collection(path)
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FriendConnection::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                },
        )
    }

    suspend fun sendFriendRequest(
        myAccount: UserAccount,
        targetStudent: StudentProfile,
        existingConnection: FriendConnection?,
    ): Result<String> {
        val pairId = canonicalPairId(myAccount.userId, targetStudent.userId)
        val docPath = "friendConnections/$pairId"
        return try {
            val uid = requireUserId()
            check(uid == myAccount.userId) { "Signed-in user mismatch" }
            require(targetStudent.userId.isNotBlank() && targetStudent.userId != uid) {
                "មិនអាចបន្ថែមខ្លួនឯងជាមិត្តភក្តិបានទេ"
            }

            val docRef = db.collection("friendConnections").document(pairId)
            when (existingConnection?.status) {
                "accepted" -> return Result.success(pairId)
                "pending" -> return Result.success(pairId)
                "declined" -> {
                    docRef.update(
                        mapOf(
                            "requesterId" to uid,
                            "requesterName" to myAccount.displayName.take(40),
                            "requesterGrade" to myAccount.gradeLevel.take(30),
                            "recipientId" to targetStudent.userId,
                            "recipientName" to targetStudent.displayName.take(40),
                            "recipientGrade" to targetStudent.gradeLevel.take(30),
                            "status" to "pending",
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                    ).await()
                }
                else -> {
                    val payload = buildFriendConnectionCreateMap(
                        requesterId = uid,
                        requesterName = myAccount.displayName.take(40),
                        requesterGrade = myAccount.gradeLevel.take(30),
                        recipientId = targetStudent.userId,
                        recipientName = targetStudent.displayName.take(40),
                        recipientGrade = targetStudent.gradeLevel.take(30),
                    )
                    docRef.set(payload).await()
                }
            }
            Result.success(pairId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (existingConnection == null) OperationType.CREATE else OperationType.UPDATE,
                docPath,
            )
            Result.failure(e)
        }
    }

    suspend fun respondToFriendRequest(
        connectionId: String,
        myAccount: UserAccount,
        accept: Boolean,
    ): Result<Unit> {
        val docPath = "friendConnections/$connectionId"
        return try {
            val uid = requireUserId()
            check(uid == myAccount.userId) { "Signed-in user mismatch" }
            require(connectionId.isNotBlank()) { "Connection ID must not be blank" }
            db.collection("friendConnections")
                .document(connectionId)
                .update(
                    mapOf(
                        "status" to if (accept) "accepted" else "declined",
                        "recipientName" to myAccount.displayName.take(40),
                        "recipientGrade" to myAccount.gradeLevel.take(30),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun removeFriendConnection(connectionId: String): Result<Unit> {
        val docPath = "friendConnections/$connectionId"
        return try {
            requireUserId()
            require(connectionId.isNotBlank()) { "Connection ID must not be blank" }
            db.collection("friendConnections")
                .document(connectionId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    fun observeDirectMessages(connectionId: String, userId: String): Flow<List<DirectLessonMessage>> = flow {
        require(connectionId.isNotBlank()) { "Connection ID must not be blank" }
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "friendConnections/$connectionId/messages"
        emitAll(
            db.collection("friendConnections")
                .document(connectionId)
                .collection("messages")
                .whereArrayContains("participantIds", uid)
                .limit(200)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(DirectLessonMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp.now() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                },
        )
    }

    suspend fun sendDirectMessage(
        connection: FriendConnection,
        account: UserAccount,
        text: String,
        attachment: LessonAttachmentDraft?,
        voice: VoiceAttachmentDraft? = null,
    ): Result<Unit> {
        val pairId = connection.id
        var docPath = "friendConnections/$pairId/messages"
        return try {
            val uid = requireUserId()
            check(uid == account.userId) { "Signed-in user mismatch" }
            require(pairId.isNotBlank() && connection.participantIds.size == 2) {
                "Invalid friend connection"
            }
            val trimmedInput = text.trim()
            val effectiveText = if (trimmedInput.isNotBlank()) {
                trimmedInput
            } else if (voice != null && voice.audioBase64.isNotBlank()) {
                "$VOICE_DEFAULT_LABEL_PREFIX (${voice.durationSec} វិនាទី)"
            } else {
                ""
            }
            require(effectiveText.length in 1..1000) { "សារជជែកត្រូវមានពី ១ ទៅ ១០០០ តួអក្សរ" }

            val connRef = db.collection("friendConnections").document(pairId)
            val msgRef = connRef.collection("messages").document()
            docPath = msgRef.path

            val payload = buildDirectLessonMessageCreateMap(
                connectionId = pairId,
                participantIds = connection.participantIds,
                senderId = uid,
                senderName = account.displayName.take(40),
                senderGrade = account.gradeLevel.take(30),
                text = effectiveText,
                attachment = attachment,
                voice = voice,
            )
            msgRef.set(payload).await()

            val previewText = buildString {
                if (attachment != null) {
                    append("[${attachment.title.take(30)}] ")
                }
                if (voice != null) {
                    append("$VOICE_DEFAULT_LABEL_PREFIX (${voice.durationSec}វិ) ")
                    if (trimmedInput.isNotBlank()) append(trimmedInput)
                } else {
                    append(effectiveText)
                }
            }.trim().take(200)

            connRef.update(
                mapOf(
                    "lastMessageText" to previewText,
                    "lastSenderName" to account.displayName.take(40),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteDirectMessage(connectionId: String, messageId: String): Result<Unit> {
        val docPath = "friendConnections/$connectionId/messages/$messageId"
        return try {
            requireUserId()
            require(connectionId.isNotBlank() && messageId.isNotBlank()) { "IDs must not be blank" }
            db.collection("friendConnections")
                .document(connectionId)
                .collection("messages")
                .document(messageId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
