package com.example.data.cloud

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

val GradeLevelPresets = listOf(
    "ថ្នាក់ទី ៧",
    "ថ្នាក់ទី ៨",
    "ថ្នាក់ទី ៩",
    "ថ្នាក់ទី ១០",
    "ថ្នាក់ទី ១១",
    "ថ្នាក់ទី ១២",
    "មហាវិទ្យាល័យ",
)

@Keep
@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val gradeLevel: String = "",
    val passcodeHash: String = "",
    val passcodeSalt: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class StudentProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val gradeLevel: String = "",
    val studentCode: String = "",
    val visibility: String = "students",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class FriendConnection(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val requesterId: String = "",
    val requesterName: String = "",
    val requesterGrade: String = "",
    val recipientId: String = "",
    val recipientName: String = "",
    val recipientGrade: String = "",
    val status: String = "pending",
    val lastMessageText: String? = null,
    val lastSenderName: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun peerUserId(myUid: String): String =
        if (requesterId == myUid) recipientId else requesterId

    fun peerName(myUid: String): String =
        if (requesterId == myUid) recipientName else requesterName

    fun peerGrade(myUid: String): String =
        if (requesterId == myUid) recipientGrade else requesterGrade
}

@Keep
@IgnoreExtraProperties
data class DirectLessonMessage(
    @DocumentId val id: String = "",
    val connectionId: String = "",
    val participantIds: List<String> = emptyList(),
    val senderId: String = "",
    val senderName: String = "",
    val senderGrade: String = "",
    val text: String = "",
    val attachedLessonTitle: String? = null,
    val attachedLessonType: String? = null,
    val attachedLessonSubject: String? = null,
    val attachedLessonNote: String? = null,
    val voiceAudioBase64: String? = null,
    val voiceDurationSec: Int? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class StudyRoom(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val name: String = "",
    val description: String = "",
    val subjectTag: String = "",
    val visibility: String = "shared",
    val lastMessageText: String? = null,
    val lastSenderName: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderGrade: String = "",
    val text: String = "",
    val attachedLessonTitle: String? = null,
    val attachedLessonType: String? = null,
    val attachedLessonSubject: String? = null,
    val attachedLessonNote: String? = null,
    val voiceAudioBase64: String? = null,
    val voiceDurationSec: Int? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
data class LessonAttachmentDraft(
    val title: String,
    val typeLabel: String,
    val subjectName: String,
    val noteExcerpt: String,
)

@Keep
data class VoiceAttachmentDraft(
    val audioBase64: String,
    val durationSec: Int,
)

const val VOICE_DEFAULT_LABEL_PREFIX = "សារសំឡេង"

fun sortedParticipantList(uidA: String, uidB: String): List<String> =
    if (uidA <= uidB) listOf(uidA, uidB) else listOf(uidB, uidA)

fun canonicalPairId(uidA: String, uidB: String): String {
    val pair = sortedParticipantList(uidA, uidB)
    return "${pair[0]}_${pair[1]}"
}

fun studentCodeForUid(uid: String): String =
    uid.filter { it.isLetterOrDigit() }
        .take(6)
        .uppercase()
        .padEnd(4, '0')

fun buildUserAccountCreateMap(
    userId: String,
    displayName: String,
    gradeLevel: String,
    passcodeHash: String,
    passcodeSalt: String,
): Map<String, Any> = mapOf(
    "userId" to userId,
    "displayName" to displayName,
    "gradeLevel" to gradeLevel,
    "passcodeHash" to passcodeHash,
    "passcodeSalt" to passcodeSalt,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp(),
)

fun buildStudentProfileCreateMap(
    userId: String,
    displayName: String,
    gradeLevel: String,
): Map<String, Any> = mapOf(
    "userId" to userId,
    "displayName" to displayName,
    "gradeLevel" to gradeLevel,
    "studentCode" to studentCodeForUid(userId),
    "visibility" to "students",
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp(),
)

fun buildFriendConnectionCreateMap(
    requesterId: String,
    requesterName: String,
    requesterGrade: String,
    recipientId: String,
    recipientName: String,
    recipientGrade: String,
): Map<String, Any> = mapOf(
    "participantIds" to sortedParticipantList(requesterId, recipientId),
    "requesterId" to requesterId,
    "requesterName" to requesterName,
    "requesterGrade" to requesterGrade,
    "recipientId" to recipientId,
    "recipientName" to recipientName,
    "recipientGrade" to recipientGrade,
    "status" to "pending",
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp(),
)

@Suppress("UNCHECKED_CAST")
fun buildDirectLessonMessageCreateMap(
    connectionId: String,
    participantIds: List<String>,
    senderId: String,
    senderName: String,
    senderGrade: String,
    text: String,
    attachment: LessonAttachmentDraft?,
    voice: VoiceAttachmentDraft? = null,
): Map<String, Any> {
    val validVoice = voice?.takeIf { it.audioBase64.isNotBlank() }
    val raw = mapOf<String, Any?>(
        "connectionId" to connectionId,
        "participantIds" to participantIds,
        "senderId" to senderId,
        "senderName" to senderName,
        "senderGrade" to senderGrade,
        "text" to text,
        "attachedLessonTitle" to attachment?.title?.take(120)?.takeIf { it.isNotBlank() },
        "attachedLessonType" to attachment?.typeLabel?.take(20)?.takeIf { it.isNotBlank() },
        "attachedLessonSubject" to attachment?.subjectName?.take(40)?.takeIf { it.isNotBlank() },
        "attachedLessonNote" to attachment?.noteExcerpt?.take(500),
        "voiceAudioBase64" to validVoice?.audioBase64?.take(260_000),
        "voiceDurationSec" to validVoice?.durationSec?.coerceIn(1, 60),
        "createdAt" to FieldValue.serverTimestamp(),
    )
    return raw.filterValues { it != null } as Map<String, Any>
}

fun buildStudyRoomCreateMap(
    creatorId: String,
    name: String,
    description: String,
    subjectTag: String,
): Map<String, Any> = mapOf(
    "creatorId" to creatorId,
    "name" to name,
    "description" to description,
    "subjectTag" to subjectTag,
    "visibility" to "shared",
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp(),
)

@Suppress("UNCHECKED_CAST")
fun buildChatMessageCreateMap(
    roomId: String,
    senderId: String,
    senderName: String,
    senderGrade: String,
    text: String,
    attachment: LessonAttachmentDraft?,
    voice: VoiceAttachmentDraft? = null,
): Map<String, Any> {
    val validVoice = voice?.takeIf { it.audioBase64.isNotBlank() }
    val raw = mapOf<String, Any?>(
        "roomId" to roomId,
        "senderId" to senderId,
        "senderName" to senderName,
        "senderGrade" to senderGrade,
        "text" to text,
        "attachedLessonTitle" to attachment?.title?.take(120)?.takeIf { it.isNotBlank() },
        "attachedLessonType" to attachment?.typeLabel?.take(20)?.takeIf { it.isNotBlank() },
        "attachedLessonSubject" to attachment?.subjectName?.take(40)?.takeIf { it.isNotBlank() },
        "attachedLessonNote" to attachment?.noteExcerpt?.take(500),
        "voiceAudioBase64" to validVoice?.audioBase64?.take(260_000),
        "voiceDurationSec" to validVoice?.durationSec?.coerceIn(1, 60),
        "createdAt" to FieldValue.serverTimestamp(),
    )
    return raw.filterValues { it != null } as Map<String, Any>
}
