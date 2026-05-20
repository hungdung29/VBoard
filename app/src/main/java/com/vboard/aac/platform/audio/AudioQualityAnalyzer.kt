package com.vboard.aac.platform.audio

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioQualityAnalyzer @Inject constructor() {

    data class QualityResult(
        val isPass: Boolean,
        val score: Float,  // 0.0 - 1.0
        val issues: List<QualityIssue>
    )

    enum class QualityIssue(val message: String) {
        TOO_QUIET("Âm thanh quá nhỏ. Hãy nói to hơn."),
        TOO_LOUD("Âm thanh quá lớn. Hãy nói vừa phải."),
        TOO_SHORT("Ghi âm quá ngắn."),
        TOO_LONG("Ghi âm quá dài."),
        BACKGROUND_NOISE("Có tiếng ồn. Hãy chọn nơi yên tĩnh.")
    }

    companion object {
        const val MIN_AMPLITUDE = 0.1f
        const val MAX_AMPLITUDE = 0.95f
        const val MIN_DURATION_MS = 20_000L  // 20 seconds minimum
        const val MAX_DURATION_MS = 35_000L  // 35 seconds maximum
    }

    fun analyze(recording: AudioRecording): QualityResult {
        val issues = mutableListOf<QualityIssue>()
        var score = 1.0f

        // Check amplitude
        when {
            recording.amplitude < MIN_AMPLITUDE -> {
                issues.add(QualityIssue.TOO_QUIET)
                score -= 0.3f
            }
            recording.amplitude > MAX_AMPLITUDE -> {
                issues.add(QualityIssue.TOO_LOUD)
                score -= 0.2f
            }
        }

        // Check duration
        when {
            recording.durationMs < MIN_DURATION_MS -> {
                issues.add(QualityIssue.TOO_SHORT)
                score -= 0.4f
            }
            recording.durationMs > MAX_DURATION_MS -> {
                issues.add(QualityIssue.TOO_LONG)
                score -= 0.1f
            }
        }

        // Duration bonus (longer recordings = better quality)
        if (recording.durationMs in MIN_DURATION_MS..MAX_DURATION_MS) {
            val durationScore = (recording.durationMs - MIN_DURATION_MS).toFloat() /
                               (MAX_DURATION_MS - MIN_DURATION_MS)
            score += durationScore * 0.2f
        }

        return QualityResult(
            isPass = issues.isEmpty() && score >= 0.6f,
            score = score.coerceIn(0f, 1f),
            issues = issues
        )
    }
}
