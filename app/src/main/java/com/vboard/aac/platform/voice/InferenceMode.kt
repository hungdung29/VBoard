package com.vboard.aac.platform.voice

enum class InferenceMode {
    FULL_XTTS,       // Tablet/high-end: 4GB+ RAM
    QUANTIZED_XTTS,  // Mid-range: 3-4GB RAM
    LITE_PIPER       // Low-end: 2-3GB RAM
}
