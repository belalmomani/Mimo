package com.mimo.companion.speech

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.mimo.companion.domain.dialogue.ClipRotation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.UUID

data class VoiceClip(val id: String, val name: String, val file: File)

sealed interface AddClipResult {
    data class Added(val clip: VoiceClip) : AddClipResult
    object TooMany : AddClipResult
    object TooBig : AddClipResult
    object TooLong : AddClipResult
    object NotAudio : AddClipResult
    data class Failed(val reason: String) : AddClipResult
}

/**
 * The user's own voice clips, copied into app-private storage (nothing is uploaded anywhere).
 * Adding or deleting clips never touches the built-in text-to-speech voice.
 */
class CustomVoiceStore(context: Context) {
    private val app = context.applicationContext
    private val dir = File(app.filesDir, "voices").apply { mkdirs() }
    private val names = app.getSharedPreferences("custom_voices", Context.MODE_PRIVATE)
    private val _clips = MutableStateFlow(load())
    val clips: StateFlow<List<VoiceClip>> get() = _clips
    private var lastId: String? = null

    /** Blocking (copies a file): call off the main thread. */
    fun add(uri: Uri, displayName: String): AddClipResult {
        if (_clips.value.size >= MAX_CLIPS) return AddClipResult.TooMany
        val id = UUID.randomUUID().toString()
        val target = File(dir, "$id.clip")
        try {
            app.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { out ->
                    val buf = ByteArray(16 * 1024); var total = 0L
                    while (true) {
                        val n = input.read(buf); if (n < 0) break
                        total += n
                        if (total > MAX_BYTES) { target.delete(); return AddClipResult.TooBig }
                        out.write(buf, 0, n)
                    }
                }
            } ?: return AddClipResult.Failed("cannot open file")
            val durationMs = try {
                MediaMetadataRetriever().run {
                    try { setDataSource(target.absolutePath); extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() } finally { release() }
                }
            } catch (e: Exception) { null }
            if (durationMs == null || durationMs <= 0) { target.delete(); return AddClipResult.NotAudio }
            if (durationMs > MAX_MS) { target.delete(); return AddClipResult.TooLong }
            val clean = displayName.substringBeforeLast('.').ifBlank { "clip" }.take(40)
            names.edit().putString(id, clean).apply()
            val clip = VoiceClip(id, clean, target)
            _clips.value = _clips.value + clip
            return AddClipResult.Added(clip)
        } catch (e: Exception) {
            target.delete()
            return AddClipResult.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    fun remove(id: String) {
        _clips.value.firstOrNull { it.id == id }?.file?.delete()
        names.edit().remove(id).apply()
        _clips.value = _clips.value.filterNot { it.id == id }
    }

    /** Deletes every clip (privacy "delete my data"). */
    fun removeAll() { _clips.value.map { it.id }.forEach { remove(it) } }

    fun pick(): VoiceClip? {
        val c = ClipRotation.pick(_clips.value, _clips.value.firstOrNull { it.id == lastId }) { (Math.random() * 1_000_000).toInt() }
        lastId = c?.id
        return c
    }

    private fun load(): List<VoiceClip> =
        (dir.listFiles { f -> f.extension == "clip" } ?: emptyArray()).sortedBy { it.lastModified() }
            .map { VoiceClip(it.nameWithoutExtension, names.getString(it.nameWithoutExtension, null) ?: "clip", it) }

    companion object {
        const val MAX_CLIPS = 10
        const val MAX_BYTES = 5L * 1024 * 1024
        const val MAX_MS = 20_000L
    }
}
