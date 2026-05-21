# Voice Cloning with Valtec-TTS - Technical Specification

**Version:** 1.0 | **Date:** 2026-05-21
**Feature:** Voice Cloning for VBoard AAC using Valtec-TTS
**Status:** Ready for Implementation

---

## 1. Overview

### 1.1 Feature Summary

Valtec-TTS enables zero-shot voice cloning for Vietnamese TTS. Parents record a 10-second audio sample, and the app generates speech in their voice for all vocab card words.

### 1.2 Why Valtec-TTS

| Criteria | Valtec-TTS | Coqui XTTS v2 | Template TTS |
|----------|------------|---------------|---------------|
| Vietnamese | ✅ Native | ❌ Not supported | ✅ Native |
| Voice Cloning | ✅ Zero-shot (10s) | ✅ Zero-shot | ❌ Template only |
| Offline | ✅ CPU inference | ✅ CPU inference | ✅ Full offline |
| Quality | High | High | Medium |
| Speed | ~300ms/sentence | ~500ms/sentence | Instant |

### 1.3 Valtec-TTS Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Valtec-TTS Model                              │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐         │
│  │  Speaker   │    │   Text     │    │   Voice    │         │
│  │  Encoder   │───▶│  Encoder   │───▶│  Generator │         │
│  │ (audio→vec)│    │ (text→vec) │    │            │         │
│  └─────────────┘    └─────────────┘    └─────────────┘         │
│         │                                    │                  │
│         │  Speaker Embedding                 │  Output Audio    │
│         └──────────────────────────────────┴──────────────────┘
└─────────────────────────────────────────────────────────────────┘
```

### 1.4 Android Integration

```
┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
│  Android App    │      │  ONNX Runtime   │      │   Audio Output  │
│                 │      │                 │      │                 │
│ 1. Record 10s  │ ───▶ │  Valtec-TTS    │ ───▶ │  Parent's voice │
│    audio        │      │  Model (~185MB) │      │  speaking text  │
└─────────────────┘      └─────────────────┘      └─────────────────┘
```

---

## 2. Technical Architecture

### 2.1 Model Files Required

| File | Size | Purpose |
|------|------|---------|
| `speaker_encoder.onnx` | ~50MB | Extract voice embedding from reference audio |
| `text_encoder.onnx` | ~30MB | Encode Vietnamese text |
| `flow.onnx` | ~80MB | Generate mel spectrogram |
| `hifigan.onnx` | ~25MB | Convert mel to audio |
| `g2p/` | ~5MB | Grapheme-to-phoneme converter |

**Total:** ~185MB (stored in `assets/valtec/`)

### 2.2 ONNX Runtime Integration

```kotlin
// build.gradle
dependencies {
    implementation 'ai.onnxruntime:onnxruntime-android:1.16.0'
}
```

### 2.3 Voice Profile Structure

```kotlin
data class VoiceProfile(
    val id: String,
    val name: String,           // "Giọng mẹ", "Giọng ba"
    val createdAt: Long,
    val referenceAudioPath: String,  // Path to 10s audio file
    val speakerEmbedding: FloatArray, // 512-dim embedding
    val isActive: Boolean,
    val modelVersion: String = "1.0"
)
```

---

## 3. Data Flow

### 3.1 Voice Recording Flow

```
┌──────────────────────────────────────────────────────────────────┐
│                    Voice Recording Flow                             │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Parent taps "Ghi âm giọng nói"                                │
│                        │                                        │
│                        ▼                                        │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Record 10 seconds of audio                            │   │
│  │  - Microphone permission                                │   │
│  │  - Show waveform visualization                          │   │
│  │  - Auto-stop at 10s                                   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                        │                                        │
│                        ▼                                        │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Speaker Encoder extracts embedding                      │   │
│  │  - Process audio → 16kHz mono WAV                      │   │
│  │  - Run ONNX inference                                 │   │
│  │  - Output: 512-dim FloatArray                         │   │
│  └─────────────────────────────────────────────────────────┘   │
│                        │                                        │
│                        ▼                                        │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Save to database (encrypted)                          │   │
│  │  - VoiceProfileEntity                                  │   │
│  │  - Embedding stored as blob                            │   │
│  │  - Reference audio optionally stored                    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                        │                                        │
│                        ▼                                        │
│  ✅ "Giọng nói đã được lưu thành công!"                       │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### 3.2 Text-to-Speech Flow

```
┌──────────────────────────────────────────────────────────────────┐
│                    TTS Synthesis Flow                             │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  App sends sentence: "Con muốn uống nước"                       │
│                        │                                        │
│                        ▼                                        │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  G2P: Convert text to phonemes                         │   │
│  │  "Con muốn uống nước" → /k-on mɯəŋɯəŋŋɯək/          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                        │                                        │
│                        ▼                                        │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Text Encoder: Phonemes → Text embedding                 │   │
│  │  Output: [512-dim vector]                              │   │
│  └─────────────────────────────────────────────────────────┘   │
│                        │                                        │
│                        ▼                                        │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Voice Generator: Combine embeddings                    │   │
│  │  - Text embedding + Speaker embedding                  │   │
│  │  - Generate mel spectrogram                            │   │
│  └─────────────────────────────────────────────────────────┘   │
│                        │                                        │
│                        ▼                                        │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  HiFi-GAN Decoder: Mel → Audio                         │   │
│  │  Output: WAV 24kHz mono                               │   │
│  └─────────────────────────────────────────────────────────┘   │
│                        │                                        │
│                        ▼                                        │
│  Play audio → Parent's voice speaking!                          │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 4. Component Specifications

### 4.1 ValtecTtsEngine

```kotlin
class ValtecTtsEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var ortEnv: OrtEnvironment? = null
    private var speakerEncoder: OrtSession? = null
    private var textEncoder: OrtSession? = null
    private var flow: OrtSession? = null
    private var hifigan: OrtSession? = null

    suspend fun initialize() {
        // Load all ONNX models from assets
    }

    suspend fun extractSpeakerEmbedding(audioPath: String): FloatArray {
        // Process reference audio → 512-dim embedding
    }

    suspend fun synthesize(
        text: String,
        speakerEmbedding: FloatArray,
        noiseScale: Float = 0.667f,
        lengthScale: Float = 1.0f
    ): ByteArray {
        // Full synthesis pipeline
    }

    fun release() {
        // Clean up ONNX sessions
    }
}
```

### 4.2 VoiceRecordingManager

```kotlin
class VoiceRecordingManager @Inject constructor(
    private val valtecTtsEngine: ValtecTtsEngine,
    private val voiceProfileRepository: IVoiceProfileRepository,
    private val audioRecorderManager: AudioRecorderManager
) {
    suspend fun recordAndSaveVoiceProfile(name: String): Result<VoiceProfile>

    suspend fun extractEmbedding(audioPath: String): FloatArray {
        return valtecTtsEngine.extractSpeakerEmbedding(audioPath)
    }
}
```

### 4.3 Enhanced TtsManager

```kotlin
class TtsManager @Inject constructor(
    private val valtecTtsEngine: ValtecTtsEngine,
    private val systemTts: AndroidTextToSpeech,
    private val voiceProfileRepository: IVoiceProfileRepository
) {
    suspend fun speak(text: String) {
        val profile = voiceProfileRepository.getActiveProfile()
        if (profile != null) {
            // Use Valtec-TTS with parent's voice
            val audio = valtecTtsEngine.synthesize(text, profile.speakerEmbedding)
            playAudio(audio)
        } else {
            // Fallback to system TTS
            systemTts.speak(text)
        }
    }
}
```

---

## 5. Database Schema

### 5.1 VoiceProfileEntity

```kotlin
@Entity(tableName = "voice_profiles")
data class VoiceProfileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val createdAt: Long,
    val referenceAudioPath: String?,
    val speakerEmbedding: String,  // Base64-encoded FloatArray
    val isActive: Boolean,
    val modelVersion: String
)
```

---

## 6. UI Flow

### 6.1 First-Time Voice Setup

```
┌─────────────────────────────────────────────────────────────────┐
│  Voice Settings                                                  │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │  🎤 Giọng người thân                                     │ │
│  │                                                           │ │
│  │  Sử dụng giọng của bạn: [  ON  ]                       │ │
│  │                                                           │ │
│  │  [ 🎙 Ghi âm giọng nói ]                               │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
                            │
                            │ Tap "Ghi âm giọng nói"
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  Voice Recording Dialog                                           │
│                                                                  │
│  Hướng dẫn:                                                    │
│  "Hãy đọc to và rõ ràng trong 10 giây..."                    │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │                                                           │ │
│  │              ▂▃▅▇▆▅▃▂▃▅▇▆▅▃▂                        │ │
│  │                                                           │ │
│  │                    [ 10:00 ]                            │ │
│  │                                                           │ │
│  │           ┌─────────────────┐                            │ │
│  │           │       🎤        │                            │ │
│  │           │    RECORDING     │                            │ │
│  │           └─────────────────┘                            │ │
│  │                                                           │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                  │
│  [Hủy]                                           [Xong]       │
└─────────────────────────────────────────────────────────────────┘
                            │
                            │ Recording complete
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  Processing...                                                   │
│                                                                  │
│  ████████████████████░░░░░░░░░░░  80%                       │
│                                                                  │
│  Đang tạo giọng nói...                                         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2 Speaker Verification (Optional)

After recording, play a test sentence to verify quality:
```
"Con muốn uống nước"
```
If quality is poor, re-record.

---

## 7. Performance Considerations

### 7.1 Device Requirements

| Device Type | RAM | Inference Time | Recommendation |
|-------------|-----|----------------|----------------|
| High-end | 8GB+ | ~200ms | Full quality |
| Mid-range | 4-6GB | ~500ms | Good quality |
| Budget | 2-4GB | ~1000ms | Lite mode |

### 7.2 Model Quantization

For low-end devices, use INT8 quantized models:
- `speaker_encoder_int8.onnx`
- `flow_int8.onnx`
- `hifigan_int8.onnx`

### 7.3 Caching Strategy

```
┌─────────────────────────────────────────────────────┐
│  Cache Manager                                      │
├─────────────────────────────────────────────────────┤
│                                                      │
│  Frequently used sentences:                           │
│  - "Con muốn" → [audio file]                       │
│  - "Uống nước" → [audio file]                     │
│                                                      │
│  LRU cache: 50 recent sentences                     │
│  Max cache size: 10MB                               │
│                                                      │
└─────────────────────────────────────────────────────┘
```

---

## 8. Error Handling

### 8.1 Error Scenarios

| Error | Detection | User Message | Recovery |
|-------|----------|-------------|----------|
| Audio too quiet | Amplitude < 0.1 | "Âm thanh quá nhỏ" | Re-record |
| Model load fail | IOException | "Không thể tải model" | Retry |
| OOM | OutOfMemoryError | "Bộ nhớ không đủ" | Fallback to TTS |
| Synthesis timeout | > 5s | "Xử lý quá lâu" | Cancel + TTS |

### 8.2 Fallback Strategy

```
Synthesize request
       │
       ▼
┌──────────────┐
│ Voice Clone  │ ──success──▶ Play audio
│ available?   │
└──────────────┘
       │ no
       ▼
┌──────────────┐
│  System TTS  │ ──always──▶ Play with Android TTS
│  (fallback)  │
└──────────────┘
```

---

## 9. Files to Create/Modify

### 9.1 New Files

```
app/src/main/java/com/vboard/aac/
├── platform/tts/
│   ├── ValtecTtsEngine.kt           # ONNX inference
│   ├── SpeakerEncoder.kt            # Voice embedding extraction
│   └── VietnameseG2p.kt           # Text preprocessing
├── platform/voice/
│   └── VoiceRecordingManager.kt     # Orchestrates recording + embedding
└── repository/
    └── VoiceProfileRepositoryImpl.kt # Enhanced with embedding
```

### 9.2 Assets

```
app/src/main/assets/
└── valtec/
    ├── speaker_encoder.onnx
    ├── text_encoder.onnx
    ├── flow.onnx
    ├── hifigan.onnx
    └── g2p/
        └── vietnamese.json
```

### 9.3 Files to Modify

- `VBoardDatabase.kt` - Add speaker embedding column
- `VoiceProfileEntity.kt` - Add embedding field
- `TtsManager.kt` - Integrate Valtec-TTS
- `VoiceSettingsActivity.kt` - Add recording UI

---

## 10. Implementation Phases

### Phase 1: Core Engine
- [ ] Add ONNX Runtime dependency
- [ ] Create ValtecTtsEngine
- [ ] Load models from assets
- [ ] Basic synthesis pipeline

### Phase 2: Voice Recording
- [ ] Speaker embedding extraction
- [ ] Recording UI
- [ ] Save to database

### Phase 3: Integration
- [ ] TtsManager integration
- [ ] Fallback to system TTS
- [ ] Cache management

### Phase 4: Polish
- [ ] Performance optimization
- [ ] Error handling
- [ ] Quality verification

---

## 11. License

Valtec-TTS: [CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/)

**Non-commercial use allowed** for VBoard AAC app (free app for children with autism).

---

*Document prepared: 2026-05-21*
