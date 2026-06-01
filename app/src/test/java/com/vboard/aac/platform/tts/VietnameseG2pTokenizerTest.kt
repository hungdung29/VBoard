package com.vboard.aac.platform.tts

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class VietnameseG2pTokenizerTest {
    private val tokenizer = VietnameseG2pTokenizer(
        symbolToId = mapOf(
            "_" to 0,
            "a" to 33,
            "c" to 47,
            "d" to 49,
            "i" to 67,
            "j" to 84,
            "k" to 86,
            "m" to 91,
            "n" to 93,
            "o" to 96,
            "p" to 103,
            "s" to 108,
            "t" to 110,
            "u" to 116,
            "v" to 129,
            "w" to 133,
            "x" to 135,
            "ŋ" to 159,
            "ɔ" to 167,
            "ə" to 172,
            "ɛ" to 174,
            "ɤ" to 179,
            "ɯ" to 188,
            "ʈ" to 197,
            "UNK" to 305
        ),
        vietnameseLanguageId = 7,
        blankId = 0,
        unknownId = 305
    )

    @Test
    fun tokenize_matchesOfficialG2pForPreviewSentence() {
        val result = tokenizer.tokenize("Con muốn uống nước")

        assertArrayEquals(
            longArrayOf(
                0, 0, 0, 86, 0, 167, 0, 93, 0, 91, 0, 116, 0, 172, 0, 93,
                0, 116, 0, 172, 0, 159, 0, 93, 0, 188, 0, 172, 0, 86, 0, 0, 0
            ),
            result.phonemes
        )
        assertArrayEquals(
            longArrayOf(
                0, 16, 0, 16, 0, 16, 0, 16, 0, 17, 0, 17, 0, 17, 0, 17,
                0, 17, 0, 17, 0, 17, 0, 17, 0, 17, 0, 17, 0, 17, 0, 16, 0
            ),
            result.tones
        )
    }

    @Test
    fun tokenize_matchesOfficialG2pForRepresentativeWords() {
        val result = tokenizer.tokenize("được viết người trường nghiệp")

        assertArrayEquals(
            longArrayOf(
                0, 0, 0, 49, 0, 188, 0, 172, 0, 86, 0, 129, 0, 67, 0, 172,
                0, 110, 0, 159, 0, 188, 0, 172, 0, 84, 0, 197, 0, 188, 0, 172,
                0, 159, 0, 159, 0, 67, 0, 172, 0, 103, 0, 0, 0
            ),
            result.phonemes
        )
        assertArrayEquals(
            longArrayOf(
                0, 16, 0, 21, 0, 21, 0, 21, 0, 21, 0, 17, 0, 17, 0, 17,
                0, 17, 0, 18, 0, 18, 0, 18, 0, 18, 0, 18, 0, 18, 0, 18,
                0, 18, 0, 21, 0, 21, 0, 21, 0, 21, 0, 16, 0
            ),
            result.tones
        )
    }
}
