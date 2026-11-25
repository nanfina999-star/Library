package com.example.ui.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.cloud.VoiceAttachmentDraft
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

private const val MAX_VOICE_SECONDS = 45
private const val MAX_VOICE_BASE64_CHARS = 240_000

@Stable
class VoiceRecorderController(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startMillis: Long = 0L

    var isRecording by mutableStateOf(false)
        private set

    fun startRecording(): Boolean {
        if (isRecording) return true
        val tempFile = File(context.cacheDir, "voice_note_${System.currentTimeMillis()}.m4a")
        return try {
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16_000)
                setAudioEncodingBitRate(24_000)
                setAudioChannels(1)
                setMaxDuration(MAX_VOICE_SECONDS * 1000)
                setOutputFile(tempFile.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            outputFile = tempFile
            startMillis = System.currentTimeMillis()
            isRecording = true
            true
        } catch (_: Exception) {
            cleanup()
            false
        }
    }

    fun stopAndBuildDraft(): VoiceAttachmentDraft? {
        if (!isRecording) return null
        val file = outputFile
        val elapsedSec = (((System.currentTimeMillis() - startMillis) + 500L) / 1000L)
            .toInt()
            .coerceIn(1, MAX_VOICE_SECONDS)
        return try {
            recorder?.apply {
                runCatching { stop() }
                runCatching { reset() }
                runCatching { release() }
            }
            recorder = null
            isRecording = false
            val bytes = file?.takeIf { it.exists() && it.length() > 0L }?.readBytes()
            file?.delete()
            outputFile = null
            if (bytes == null || bytes.isEmpty()) return null
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            if (encoded.isBlank() || encoded.length > MAX_VOICE_BASE64_CHARS) {
                null
            } else {
                VoiceAttachmentDraft(audioBase64 = encoded, durationSec = elapsedSec)
            }
        } catch (_: Exception) {
            cleanup()
            null
        }
    }

    fun cancel() {
        if (!isRecording && recorder == null) return
        cleanup()
    }

    fun release() {
        cleanup()
    }

    private fun cleanup() {
        recorder?.let { rec ->
            runCatching { rec.stop() }
            runCatching { rec.reset() }
            runCatching { rec.release() }
        }
        recorder = null
        outputFile?.delete()
        outputFile = null
        isRecording = false
    }
}

@Stable
class VoicePlaybackController(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var tempPlaybackFile: File? = null

    var activeMessageId by mutableStateOf<String?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var progressFraction by mutableFloatStateOf(0f)
        private set
    var currentSecond by mutableIntStateOf(0)
        private set

    fun togglePlay(messageId: String, audioBase64: String) {
        if (activeMessageId == messageId && mediaPlayer != null) {
            val mp = mediaPlayer ?: return
            if (mp.isPlaying) {
                mp.pause()
                isPlaying = false
            } else {
                mp.start()
                isPlaying = true
            }
            return
        }

        stop()
        val decoded = runCatching { Base64.decode(audioBase64, Base64.DEFAULT) }.getOrNull() ?: return
        if (decoded.isEmpty()) return

        val file = File(context.cacheDir, "play_${messageId.hashCode()}.m4a")
        try {
            file.writeBytes(decoded)
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    this@VoicePlaybackController.isPlaying = false
                    this@VoicePlaybackController.progressFraction = 0f
                    this@VoicePlaybackController.currentSecond = 0
                    this@VoicePlaybackController.activeMessageId = null
                    releaseInternal()
                }
                prepare()
                start()
            }
            tempPlaybackFile = file
            mediaPlayer = player
            activeMessageId = messageId
            isPlaying = true
            progressFraction = 0f
            currentSecond = 0
        } catch (_: Exception) {
            file.delete()
            stop()
        }
    }

    fun updateProgressTick() {
        val player = mediaPlayer ?: return
        if (!isPlaying) return
        runCatching {
            val total = player.duration.coerceAtLeast(1)
            val pos = player.currentPosition.coerceIn(0, total)
            progressFraction = pos.toFloat() / total.toFloat()
            currentSecond = pos / 1000
        }
    }

    fun stop() {
        releaseInternal()
        activeMessageId = null
        isPlaying = false
        progressFraction = 0f
        currentSecond = 0
    }

    fun release() {
        stop()
    }

    private fun releaseInternal() {
        mediaPlayer?.let { mp ->
            runCatching { if (mp.isPlaying) mp.stop() }
            runCatching { mp.reset() }
            runCatching { mp.release() }
        }
        mediaPlayer = null
        tempPlaybackFile?.delete()
        tempPlaybackFile = null
    }
}

@Composable
fun rememberVoicePlaybackController(): VoicePlaybackController {
    val context = LocalContext.current
    val controller = remember(context) { VoicePlaybackController(context) }

    LaunchedEffect(controller.activeMessageId, controller.isPlaying) {
        while (controller.isPlaying && controller.activeMessageId != null) {
            controller.updateProgressTick()
            delay(150L)
        }
    }

    DisposableEffect(controller) {
        onDispose { controller.release() }
    }
    return controller
}

@Composable
fun VoiceMessagePlayerCard(
    messageId: String,
    voiceBase64: String,
    durationSec: Int,
    voicePlayer: VoicePlaybackController,
    isOwnMessage: Boolean,
    modifier: Modifier = Modifier,
) {
    val isThisActive = voicePlayer.activeMessageId == messageId
    val isThisPlaying = isThisActive && voicePlayer.isPlaying
    val shownProgress = if (isThisActive) voicePlayer.progressFraction else 0f
    val shownSeconds = if (isThisActive && voicePlayer.currentSecond > 0) {
        voicePlayer.currentSecond
    } else {
        durationSec
    }

    Surface(
        color = if (isOwnMessage) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag("voice_message_card_$messageId"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FilledTonalIconButton(
                onClick = { voicePlayer.togglePlay(messageId, voiceBase64) },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("voice_play_button_$messageId"),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    imageVector = if (isThisPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = stringResource(
                        if (isThisPlaying) R.string.voice_pause_button else R.string.voice_play_button,
                    ),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.voice_play_button),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = formatVoiceDuration(shownSeconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinearProgressIndicator(
                    progress = { shownProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

@Composable
fun VoiceRecordIconButton(
    enabled: Boolean,
    onStartRecording: () -> Unit,
    onPermissionDenied: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            onStartRecording()
        } else {
            onPermissionDenied()
        }
    }

    FilledTonalIconButton(
        onClick = {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                onStartRecording()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        enabled = enabled,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Icon(
            imageVector = Icons.Outlined.Mic,
            contentDescription = stringResource(R.string.voice_record_button),
        )
    }
}

@Composable
fun LiveVoiceRecordingBar(
    elapsedSeconds: Int,
    onCancel: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pulseTransition = rememberInfiniteTransition(label = "voice_pulse")
    val dotAlpha by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot_alpha",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("live_voice_recording_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("cancel_voice_recording_button"),
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.voice_cancel_button),
                tint = MaterialTheme.colorScheme.error,
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
            modifier = Modifier.weight(1f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .alpha(dotAlpha)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error),
                    )
                    Text(
                        text = stringResource(R.string.voice_recording_status),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${formatVoiceDuration(elapsedSeconds)} / ${formatVoiceDuration(MAX_VOICE_SECONDS)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        FilledIconButton(
            onClick = onSend,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("send_voice_recording_button"),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.voice_send_button),
            )
        }
    }
}

fun formatVoiceDuration(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return String.format(Locale.US, "%d:%02d", mins, secs)
}
