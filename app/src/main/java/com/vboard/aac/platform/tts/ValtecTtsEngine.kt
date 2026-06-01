package com.vboard.aac.platform.tts

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer
import java.nio.LongBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.random.Random

@Singleton
class ValtecTtsEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ValtecTtsEngine"
        private const val TEXT_ENCODER_MODEL = "valtec/text_encoder.onnx"
        private const val DURATION_MODEL = "valtec/duration_predictor.onnx"
        private const val FLOW_MODEL = "valtec/flow.onnx"
        private const val DECODER_MODEL = "valtec/decoder.onnx"
        private const val CONFIG_PATH = "valtec/tts_config.json"
        private const val MODEL_CACHE_VERSION = "v2"
        private const val DEFAULT_SPEAKER_ID = 1 // SF - Southern Female in Valtec config.
        private const val MAX_FRAMES = 1200

        const val VOICE_NF = "valtec-nf"
        const val VOICE_SF = "valtec-sf"
        const val VOICE_NM1 = "valtec-nm1"
        const val VOICE_SM = "valtec-sm"
        const val VOICE_NM2 = "valtec-nm2"

        const val SAMPLE_RATE = 24000
        const val SPEAKER_EMBEDDING_DIM = 512

        fun speakerIdForVoiceType(voiceType: String): Int? = when (voiceType) {
            VOICE_NF, "nu-bac" -> 0
            VOICE_SF, "nu-nam" -> 1
            VOICE_NM1, "nam-bac" -> 2
            VOICE_SM, "nam-nam" -> 3
            VOICE_NM2 -> 4
            else -> null
        }
    }

    private var ortEnvironment: OrtEnvironment? = null
    private var textEncoderSession: OrtSession? = null
    private var durationSession: OrtSession? = null
    private var flowSession: OrtSession? = null
    private var decoderSession: OrtSession? = null
    private var config: ValtecConfig? = null

    val isInitialized: Boolean
        get() = textEncoderSession != null &&
            durationSession != null &&
            flowSession != null &&
            decoderSession != null &&
            config != null

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext

        try {
            Log.d(TAG, "Initializing Valtec-TTS...")
            ortEnvironment = OrtEnvironment.getEnvironment()
            config = loadConfig()
            textEncoderSession = loadModel(TEXT_ENCODER_MODEL)
            durationSession = loadModel(DURATION_MODEL)
            flowSession = loadModel(FLOW_MODEL)
            decoderSession = loadModel(DECODER_MODEL)
            Log.d(TAG, "Initialized Valtec-TTS: ready=$isInitialized")
        } catch (e: Exception) {
            Log.e(TAG, "Init failed", e)
            release()
        }
    }

    private fun loadConfig(): ValtecConfig {
        val json = context.assets.open(CONFIG_PATH).bufferedReader(Charsets.UTF_8).use { reader ->
            JSONObject(reader.readText())
        }
        val symbolJson = json.getJSONObject("symbol_to_id")
        val symbolToId = mutableMapOf<String, Int>()
        val keys = symbolJson.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            symbolToId[key] = symbolJson.getInt(key)
        }

        val languageId = json.getJSONObject("language_id_map").getInt("VI")
        val sampleRate = json.optInt("sample_rate", SAMPLE_RATE)
        return ValtecConfig(symbolToId, languageId, sampleRate)
    }

    private fun loadModel(assetPath: String): OrtSession? {
        val env = ortEnvironment ?: return null
        val tempFile = File(context.cacheDir, "${MODEL_CACHE_VERSION}_${File(assetPath).name}")
        return try {
            if (!tempFile.exists() || tempFile.length() == 0L) {
                extractAssetToFile(assetPath, tempFile)
            }
            env.createSession(tempFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Load failed: $assetPath", e)
            null
        }
    }

    private fun extractAssetToFile(assetPath: String, destFile: File) {
        context.assets.open(assetPath).use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        Log.d(TAG, "Extracted: ${destFile.name} (${destFile.length() / 1024 / 1024}MB)")
    }

    /**
     * The current Valtec ONNX assets expose five preset Vietnamese speakers, not
     * a zero-shot speaker encoder. Keep the database contract stable by storing
     * the selected speaker id in the first slot of the embedding vector.
     */
    suspend fun extractSpeakerEmbedding(audioPath: String): FloatArray = withContext(Dispatchers.IO) {
        Log.d(TAG, "Using preset Valtec speaker for profile recorded at: $audioPath")
        createSpeakerEmbedding(DEFAULT_SPEAKER_ID)
    }

    fun createSpeakerEmbedding(speakerId: Int = DEFAULT_SPEAKER_ID): FloatArray {
        val safeSpeakerId = speakerId.coerceIn(0, 4)
        return FloatArray(SPEAKER_EMBEDDING_DIM).also { embedding ->
            embedding[0] = safeSpeakerId.toFloat()
        }
    }

    suspend fun synthesize(
        text: String,
        speakerEmbedding: FloatArray,
        noiseScale: Float = 0.667f,
        lengthScale: Float = 1.0f
    ): FloatArray = withContext(Dispatchers.IO) {
        if (!isInitialized) initialize()

        val env = ortEnvironment ?: return@withContext FloatArray(0)
        val ttsConfig = config ?: return@withContext FloatArray(0)
        val textEncoder = textEncoderSession ?: return@withContext FloatArray(0)
        val durationPredictor = durationSession ?: return@withContext FloatArray(0)
        val flow = flowSession ?: return@withContext FloatArray(0)
        val decoder = decoderSession ?: return@withContext FloatArray(0)

        try {
            val tokens = VietnameseG2pTokenizer(
                symbolToId = ttsConfig.symbolToId,
                vietnameseLanguageId = ttsConfig.vietnameseLanguageId,
                blankId = ttsConfig.blankId,
                unknownId = ttsConfig.unknownId
            ).tokenize(text)
            val seqLen = tokens.phonemes.size.coerceAtLeast(1)
            val speakerId = resolveSpeakerId(speakerEmbedding)

            val phoneIds = env.longTensor(tokens.phonemes, longArrayOf(1, seqLen.toLong()))
            val phoneLengths = env.longTensor(longArrayOf(seqLen.toLong()), longArrayOf(1))
            val toneIds = env.longTensor(tokens.tones, longArrayOf(1, seqLen.toLong()))
            val languageIds = env.longTensor(tokens.languages, longArrayOf(1, seqLen.toLong()))
            val bert = env.floatTensor(FloatArray(1024 * seqLen), longArrayOf(1, 1024, seqLen.toLong()))
            val jaBert = env.floatTensor(FloatArray(768 * seqLen), longArrayOf(1, 768, seqLen.toLong()))
            val sid = env.longTensor(longArrayOf(speakerId.toLong()), longArrayOf(1))

            val encoderInputs = mapOf(
                "phone_ids" to phoneIds,
                "phone_lengths" to phoneLengths,
                "tone_ids" to toneIds,
                "language_ids" to languageIds,
                "bert" to bert,
                "ja_bert" to jaBert,
                "speaker_id" to sid
            )

            textEncoder.run(encoderInputs).use { encOutputs ->
                val xEncoded = encOutputs.tensor("x_encoded")
                val mP = encOutputs.tensor("m_p")
                val logsP = encOutputs.tensor("logs_p")
                val xMask = encOutputs.tensor("x_mask")
                val g = encOutputs.tensor("g")

                val (logwData, maskData) = durationPredictor.run(
                    mapOf("x" to xEncoded, "x_mask" to xMask, "g" to g)
                ).use { dpOutputs ->
                    dpOutputs.tensor("logw").toFloatArray() to xMask.toFloatArray()
                }

                val durations = IntArray(seqLen)
                var totalFrames = 0
                for (i in 0 until seqLen) {
                    val mask = maskData.getOrElse(i) { 1f }
                    val duration = ceil(exp(logwData.getOrElse(i) { 0f }.toDouble()) * mask * lengthScale)
                        .toInt()
                        .coerceIn(0, 80)
                    durations[i] = duration
                    totalFrames += duration
                }
                totalFrames = totalFrames.coerceIn(1, MAX_FRAMES)

                val channels = mP.channels()
                val expandedMean = FloatArray(channels * totalFrames)
                val expandedLogs = FloatArray(channels * totalFrames)
                expandEncoderOutput(
                    sourceMean = mP.toFloatArray(),
                    sourceLogs = logsP.toFloatArray(),
                    seqLen = seqLen,
                    channels = channels,
                    durations = durations,
                    totalFrames = totalFrames,
                    expandedMean = expandedMean,
                    expandedLogs = expandedLogs
                )

                val zP = FloatArray(channels * totalFrames) { index ->
                    val noise = (Random.nextFloat() * 2f - 1f) * noiseScale
                    expandedMean[index] + exp(expandedLogs[index].toDouble()).toFloat() * noise
                }
                val zPTensor = env.floatTensor(zP, longArrayOf(1, channels.toLong(), totalFrames.toLong()))
                val yMask = env.floatTensor(FloatArray(totalFrames) { 1f }, longArrayOf(1, 1, totalFrames.toLong()))

                flow.run(mapOf("z_p" to zPTensor, "y_mask" to yMask, "g" to g)).use { flowOutputs ->
                    val z = flowOutputs.tensor("z")
                    decoder.run(mapOf("z" to z, "g" to g)).use { decoderOutputs ->
                        return@withContext decoderOutputs.tensor("audio").toFloatArray()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Synthesis failed", e)
            FloatArray(0)
        }
    }

    private fun expandEncoderOutput(
        sourceMean: FloatArray,
        sourceLogs: FloatArray,
        seqLen: Int,
        channels: Int,
        durations: IntArray,
        totalFrames: Int,
        expandedMean: FloatArray,
        expandedLogs: FloatArray
    ) {
        var frame = 0
        for (tokenIndex in 0 until seqLen) {
            repeat(durations[tokenIndex]) {
                if (frame >= totalFrames) return
                for (channel in 0 until channels) {
                    val sourceIndex = channel * seqLen + tokenIndex
                    val targetIndex = channel * totalFrames + frame
                    expandedMean[targetIndex] = sourceMean.getOrElse(sourceIndex) { 0f }
                    expandedLogs[targetIndex] = sourceLogs.getOrElse(sourceIndex) { 0f }
                }
                frame++
            }
        }
        while (frame < totalFrames) {
            for (channel in 0 until channels) {
                val sourceIndex = channel * seqLen + (seqLen - 1)
                val targetIndex = channel * totalFrames + frame
                expandedMean[targetIndex] = sourceMean.getOrElse(sourceIndex) { 0f }
                expandedLogs[targetIndex] = sourceLogs.getOrElse(sourceIndex) { 0f }
            }
            frame++
        }
    }

    private fun resolveSpeakerId(speakerEmbedding: FloatArray): Int {
        val id = speakerEmbedding.firstOrNull()?.takeIf { it.isFinite() }?.toInt() ?: DEFAULT_SPEAKER_ID
        return id.coerceIn(0, 4)
    }

    fun isReady(): Boolean = isInitialized

    fun release() {
        try {
            textEncoderSession?.close()
            durationSession?.close()
            flowSession?.close()
            decoderSession?.close()
            ortEnvironment?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Release error", e)
        } finally {
            textEncoderSession = null
            durationSession = null
            flowSession = null
            decoderSession = null
            ortEnvironment = null
            config = null
        }
    }

    private fun OrtEnvironment.longTensor(values: LongArray, shape: LongArray): OnnxTensor =
        OnnxTensor.createTensor(this, LongBuffer.wrap(values), shape)

    private fun OrtEnvironment.floatTensor(values: FloatArray, shape: LongArray): OnnxTensor =
        OnnxTensor.createTensor(this, FloatBuffer.wrap(values), shape)

    private fun OrtSession.Result.tensor(name: String): OnnxTensor =
        get(name).orElseThrow { IllegalStateException("Missing ONNX output: $name") } as OnnxTensor

    private fun OnnxTensor.toFloatArray(): FloatArray {
        val buffer = floatBuffer
        buffer.rewind()
        return FloatArray(buffer.remaining()).also { buffer.get(it) }
    }

    private fun OnnxTensor.channels(): Int {
        val info = info as TensorInfo
        return info.shape.getOrNull(1)?.toInt()?.coerceAtLeast(1) ?: 1
    }
}

private data class ValtecConfig(
    val symbolToId: Map<String, Int>,
    val vietnameseLanguageId: Int,
    val sampleRate: Int
) {
    val unknownId: Int = symbolToId["UNK"] ?: 305
    val blankId: Int = symbolToId["_"] ?: 0
}
