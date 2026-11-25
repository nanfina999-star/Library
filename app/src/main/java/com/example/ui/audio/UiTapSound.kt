package com.example.ui.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.R
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

interface UiSoundPlayer {
    fun playClick()
}

object NoOpUiSoundPlayer : UiSoundPlayer {
    override fun playClick() = Unit
}

val LocalUiSoundPlayer = staticCompositionLocalOf<UiSoundPlayer> { NoOpUiSoundPlayer }
val LocalUiSoundEnabled = compositionLocalOf { true }

class AndroidUiSoundPlayer : UiSoundPlayer {
    private companion object {
        const val SAMPLE_RATE = 24_000
        const val DURATION_MS = 22
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "ui-tap-sound").apply { isDaemon = true }
    }

    private val pcmSamples: ShortArray by lazy { generateClickSamples() }

    @Volatile
    private var released = false
    private var audioTrack: AudioTrack? = null

    override fun playClick() {
        if (released) return
        runCatching {
            executor.execute {
                if (released) return@execute
                playClickInternal()
            }
        }
    }

    private fun playClickInternal() {
        runCatching {
            val track = ensureAudioTrack() ?: return
            if (track.playState != AudioTrack.PLAYSTATE_STOPPED) {
                track.stop()
            }
            val reloadStatus = track.reloadStaticData()
            if (reloadStatus != AudioTrack.SUCCESS) {
                track.release()
                audioTrack = null
                ensureAudioTrack()?.play()
            } else {
                track.play()
            }
        }
    }

    private fun ensureAudioTrack(): AudioTrack? {
        audioTrack?.let { existing ->
            if (existing.state == AudioTrack.STATE_INITIALIZED) return existing
            runCatching { existing.release() }
            audioTrack = null
        }

        val samples = pcmSamples
        val bufferSizeBytes = samples.size * 2
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        val created = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufferSizeBytes)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        if (created.state != AudioTrack.STATE_INITIALIZED) {
            created.release()
            return null
        }

        val written = created.write(samples, 0, samples.size)
        if (written < 0) {
            created.release()
            return null
        }
        audioTrack = created
        return created
    }

    fun release() {
        released = true
        runCatching {
            executor.execute {
                runCatching {
                    audioTrack?.stop()
                    audioTrack?.release()
                }
                audioTrack = null
            }
            executor.shutdown()
        }
    }

    private fun generateClickSamples(): ShortArray {
        val sampleCount = (SAMPLE_RATE * DURATION_MS) / 1000
        return ShortArray(sampleCount) { index ->
            val t = index.toDouble() / SAMPLE_RATE
            val attack = (t / 0.0015).coerceAtMost(1.0)
            val decay = exp(-t * 195.0)
            val fundamental = sin(2.0 * PI * 1150.0 * t)
            val overtone = sin(2.0 * PI * 2300.0 * t)
            val mixed = 0.72 * fundamental + 0.28 * overtone
            (mixed * attack * decay * 0.42 * Short.MAX_VALUE)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }
}

fun Modifier.detectGlobalUiTapSound(): Modifier = composed {
    val soundPlayer = LocalUiSoundPlayer.current
    val soundEnabled = LocalUiSoundEnabled.current
    val viewConfiguration = LocalViewConfiguration.current

    if (!soundEnabled) {
        this
    } else {
        this.pointerInput(soundPlayer, soundEnabled) {
            val touchSlop = viewConfiguration.touchSlop
            awaitEachGesture {
                val down = awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial,
                )
                val downPos = down.position
                var maxDistance = 0f
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    val distance = (change.position - downPos).getDistance()
                    if (distance > maxDistance) {
                        maxDistance = distance
                    }
                    if (change.changedToUpIgnoreConsumed()) {
                        if (maxDistance <= touchSlop) {
                            soundPlayer.playClick()
                        }
                        break
                    }
                    if (!change.pressed) {
                        break
                    }
                }
            }
        }
    }
}

@Composable
fun SoundToggleIconButton(
    soundEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(
        if (soundEnabled) R.string.sound_toggle_on else R.string.sound_toggle_off,
    )
    IconButton(
        onClick = onToggle,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("sound_toggle_button"),
    ) {
        Icon(
            imageVector = if (soundEnabled) {
                Icons.AutoMirrored.Outlined.VolumeUp
            } else {
                Icons.AutoMirrored.Outlined.VolumeOff
            },
            contentDescription = description,
            tint = if (soundEnabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
