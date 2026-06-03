package com.vboard.aac.platform.tts

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VieneuCloneClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun synthesize(
        serverUrl: String,
        text: String,
        referenceAudio: File
    ): File = withContext(Dispatchers.IO) {
        require(text.isNotBlank()) { "Text is required" }
        require(referenceAudio.isFile) { "Reference audio is missing" }

        val endpoint = URL("${serverUrl.trim().trimEnd('/')}/v1/clone")
        val boundary = "VBoard-${UUID.randomUUID()}"
        val connection = endpoint.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Accept", "audio/wav")
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

            DataOutputStream(connection.outputStream).use { output ->
                output.writeTextPart(boundary, "text", text)
                output.writeTextPart(boundary, "ref_text", REFERENCE_TEXT)
                output.writeFilePart(boundary, "reference_audio", referenceAudio)
                output.writeBytes("--$boundary--\r\n")
            }

            if (connection.responseCode !in 200..299) {
                val message = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw IllegalStateException("VieNeu server returned ${connection.responseCode}: $message")
            }

            val outputFile = File.createTempFile("vieneu_clone_", ".wav", context.cacheDir)
            try {
                connection.inputStream.use { input ->
                    outputFile.outputStream().use { output -> input.copyTo(output) }
                }
                outputFile
            } catch (e: Exception) {
                outputFile.delete()
                throw e
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun DataOutputStream.writeTextPart(boundary: String, name: String, value: String) {
        writeBytes("--$boundary\r\n")
        writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n")
        writeBytes("Content-Type: text/plain; charset=UTF-8\r\n\r\n")
        write(value.toByteArray(Charsets.UTF_8))
        writeBytes("\r\n")
    }

    private fun DataOutputStream.writeFilePart(boundary: String, name: String, file: File) {
        writeBytes("--$boundary\r\n")
        writeBytes("Content-Disposition: form-data; name=\"$name\"; filename=\"reference.wav\"\r\n")
        writeBytes("Content-Type: audio/wav\r\n\r\n")
        file.inputStream().use { input -> input.copyTo(this) }
        writeBytes("\r\n")
    }

    companion object {
        const val DEFAULT_SERVER_URL = "http://127.0.0.1:8765"

        private const val CONNECT_TIMEOUT_MS = 2_500
        private const val READ_TIMEOUT_MS = 180_000
        private const val REFERENCE_TEXT =
            "Con mu\u1ed1n u\u1ed1ng n\u01b0\u1edbc, h\u00f4m nay tr\u1eddi \u0111\u1eb9p qu\u00e1 m\u1eb9 \u01a1i."
    }
}
