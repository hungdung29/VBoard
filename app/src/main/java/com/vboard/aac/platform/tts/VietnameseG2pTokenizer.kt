package com.vboard.aac.platform.tts

import java.util.Locale

internal data class TokenizedText(
    val phonemes: LongArray,
    val tones: LongArray,
    val languages: LongArray
)

/**
 * Kotlin port of Valtec's official browser G2P converter:
 * https://huggingface.co/spaces/valtecAI-team/valtec-vietnamese-tts-web
 */
internal class VietnameseG2pTokenizer(
    private val symbolToId: Map<String, Int>,
    private val vietnameseLanguageId: Int,
    private val blankId: Int,
    private val unknownId: Int
) {
    fun tokenize(text: String): TokenizedText {
        val phonemes = mutableListOf<Int>()
        val tones = mutableListOf<Int>()
        val languages = mutableListOf<Int>()

        text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.forEach { rawWord ->
            val trailingPunctuation = rawWord.takeLastWhile { it in PUNCTUATION }
            val cleanWord = rawWord.dropLast(trailingPunctuation.length)

            if (cleanWord.isNotBlank()) {
                val transcription = transcribe(cleanWord)
                if (transcription.isOov) {
                    phonemes += unknownId
                    tones += 0
                    languages += vietnameseLanguageId
                } else {
                    val tone = VIPHONEME_TONE_MAP[transcription.tone] ?: 0
                    tokenizeIpa(transcription.onset + transcription.nucleus + transcription.coda).forEach { symbol ->
                        phonemes += symbolToId[symbol] ?: unknownId
                        tones += tone
                        languages += vietnameseLanguageId
                    }
                }
            }

            trailingPunctuation.forEach { punctuation ->
                phonemes += symbolToId[punctuation.toString()] ?: unknownId
                tones += 0
                languages += vietnameseLanguageId
            }
        }

        phonemes.add(0, blankId)
        phonemes += blankId
        tones.add(0, 0)
        tones += 0
        languages.add(0, vietnameseLanguageId)
        languages += vietnameseLanguageId

        return addBlanks(
            phonemes = phonemes,
            tones = tones.map { it + VI_TONE_OFFSET },
            languages = languages
        )
    }

    private fun transcribe(rawWord: String): Transcription {
        val word = rawWord.lowercase(Locale.ROOT)
        if (word.isEmpty()) return Transcription()

        var onset = ""
        var nucleus = ""
        var coda = ""
        var tone = 1
        var onsetOffset = 0
        var codaOffset = 0

        for (length in 3 downTo 1) {
            if (word.length < length) continue
            val candidate = word.take(length)
            val mapped = ONSETS[candidate]
            if (mapped != null) {
                onset = mapped
                onsetOffset = length
                break
            }
        }

        for (length in 2 downTo 1) {
            if (word.length < length) continue
            val candidate = word.takeLast(length)
            val mapped = CODAS[candidate]
            if (mapped != null) {
                coda = mapped
                codaOffset = length
                break
            }
        }

        var nucleusText = word.substring(onsetOffset, word.length - codaOffset)
        if (
            word.first() == 'g' &&
            word.length == 3 &&
            word[1] in GI_I_VARIANTS &&
            coda.isNotEmpty()
        ) {
            nucleusText = "i"
            onset = "z"
        }

        when {
            NUCLEI[nucleusText] != null -> {
                nucleus = NUCLEI.getValue(nucleusText)
            }

            ONGLIDES[nucleusText] != null -> {
                nucleus = ONGLIDES.getValue(nucleusText)
                if (onset != "kw") {
                    onset = if (onset.isEmpty()) "w" else onset + "w"
                }
            }

            ONOFFGLIDES[nucleusText] != null -> {
                val glide = ONOFFGLIDES.getValue(nucleusText)
                coda = glide.takeLast(1)
                nucleus = glide.dropLast(1)
                if (onset != "kw") {
                    onset = if (onset.isEmpty()) "w" else onset + "w"
                }
            }

            OFFGLIDES[nucleusText] != null -> {
                val glide = OFFGLIDES.getValue(nucleusText)
                coda = glide.takeLast(1)
                nucleus = glide.dropLast(1)
            }

            GI_WORDS[word] != null -> {
                val special = GI_WORDS.getValue(word)
                onset = special.take(1)
                nucleus = special.drop(1)
            }

            QU_WORDS[word] != null -> {
                val special = QU_WORDS.getValue(word)
                onset = special.dropLast(1)
                nucleus = special.takeLast(1)
            }

            else -> return Transcription(nucleus = word, isOov = true)
        }

        tone = word.firstNotNullOfOrNull { TONES[it] } ?: tone

        if (nucleus == "a" && coda == "ɲ") nucleus = "ɛ"
        if (nucleus == "a" && coda == "k" && codaOffset == 2) nucleus = "ɛ"
        if (nucleus in setOf("u", "o", "ɔ")) {
            if (coda == "ŋ") coda = "ŋ͡m"
            if (coda == "k") coda = "k͡p"
        }

        return Transcription(onset, nucleus, coda, tone)
    }

    private fun tokenizeIpa(ipa: String): List<String> {
        val symbols = mutableListOf<String>()
        ipa.forEach { character ->
            when {
                character.isCombiningMark() -> Unit
                character in MODIFIER_LETTERS && symbols.isNotEmpty() -> {
                    symbols[symbols.lastIndex] += character
                }

                character in TIE_BARS -> Unit
                else -> symbols += character.toString()
            }
        }
        return symbols
    }

    private fun addBlanks(
        phonemes: List<Int>,
        tones: List<Int>,
        languages: List<Int>
    ): TokenizedText {
        val withBlanks = mutableListOf<Long>()
        val tonesWithBlanks = mutableListOf<Long>()
        val languagesWithBlanks = mutableListOf<Long>()

        phonemes.indices.forEach { index ->
            withBlanks += blankId.toLong()
            tonesWithBlanks += 0L
            languagesWithBlanks += vietnameseLanguageId.toLong()
            withBlanks += phonemes[index].toLong()
            tonesWithBlanks += tones[index].toLong()
            languagesWithBlanks += languages[index].toLong()
        }

        withBlanks += blankId.toLong()
        tonesWithBlanks += 0L
        languagesWithBlanks += vietnameseLanguageId.toLong()

        return TokenizedText(
            phonemes = withBlanks.toLongArray(),
            tones = tonesWithBlanks.toLongArray(),
            languages = languagesWithBlanks.toLongArray()
        )
    }

    private fun Char.isCombiningMark(): Boolean {
        val code = code
        return code in 0x0300..0x036F ||
            code in 0x1AB0..0x1AFF ||
            code in 0x1DC0..0x1DFF ||
            code in 0x20D0..0x20FF ||
            code in 0xFE20..0xFE2F
    }

    private data class Transcription(
        val onset: String = "",
        val nucleus: String = "",
        val coda: String = "",
        val tone: Int = 1,
        val isOov: Boolean = false
    )

    private companion object {
        private const val VI_TONE_OFFSET = 16
        private const val GI_I_VARIANTS = "iíìĩị"
        private val PUNCTUATION = setOf(',', '.', '!', '?', ';', ':', '\'', '"', '(', ')', '[', ']', '{', '}')
        private val MODIFIER_LETTERS = setOf('ʷ', 'ʰ', 'ː')
        private val TIE_BARS = setOf('\u0361', '\u035C')
        private val VIPHONEME_TONE_MAP = mapOf(1 to 0, 2 to 2, 3 to 3, 4 to 4, 5 to 1, 6 to 5)

        private val ONSETS = mapOf(
            "b" to "b", "t" to "t", "th" to "tʰ", "đ" to "d", "ch" to "c",
            "kh" to "x", "g" to "ɣ", "l" to "l", "m" to "m", "n" to "n",
            "ngh" to "ŋ", "nh" to "ɲ", "ng" to "ŋ", "ph" to "f", "v" to "v",
            "x" to "s", "d" to "z", "h" to "h", "p" to "p", "qu" to "kw",
            "gi" to "j", "tr" to "ʈ", "k" to "k", "c" to "k", "gh" to "ɣ",
            "r" to "ʐ", "s" to "ʂ"
        )

        private val CODAS = mapOf(
            "p" to "p", "t" to "t", "c" to "k", "m" to "m",
            "n" to "n", "ng" to "ŋ", "nh" to "ɲ", "ch" to "tʃ"
        )

        private val NUCLEI = buildMap {
            variants("a", "a", "á", "à", "ả", "ã", "ạ")
            variants("ɤ̆", "â", "ấ", "ầ", "ẩ", "ẫ", "ậ")
            variants("ă", "ă", "ắ", "ằ", "ẳ", "ẵ", "ặ")
            variants("ɛ", "e", "é", "è", "ẻ", "ẽ", "ẹ")
            variants("e", "ê", "ế", "ề", "ể", "ễ", "ệ")
            variants("i", "i", "í", "ì", "ỉ", "ĩ", "ị")
            variants("ɔ", "o", "ó", "ò", "ỏ", "õ", "ọ")
            variants("o", "ô", "ố", "ồ", "ổ", "ỗ", "ộ")
            variants("ɤ", "ơ", "ớ", "ờ", "ở", "ỡ", "ợ")
            variants("u", "u", "ú", "ù", "ủ", "ũ", "ụ")
            variants("ɯ", "ư", "ứ", "ừ", "ử", "ữ", "ự")
            variants("i", "y", "ý", "ỳ", "ỷ", "ỹ", "ỵ")
            variants("eo", "eo", "éo", "èo", "ẻo", "ẽo", "ẹo")
            variants("ɛu", "êu", "ếu", "ều", "ểu", "ễu", "ệu")
            variants("iə", "ia", "ía", "ìa", "ỉa", "ĩa", "ịa", "iá", "ià", "iả", "iã", "iạ")
            variants("iə", "iê", "iế", "iề", "iể", "iễ", "iệ")
            variants("ɔ", "oo", "óo", "òo", "ỏo", "õo", "ọo", "oó", "oò", "oỏ", "oõ", "oọ")
            variants("uə", "ua", "úa", "ùa", "ủa", "ũa", "ụa")
            variants("uə", "uô", "uố", "uồ", "uổ", "uỗ", "uộ")
            variants("ɯə", "ưa", "ứa", "ừa", "ửa", "ữa", "ựa")
            variants("ɯə", "ươ", "ướ", "ườ", "ưở", "ưỡ", "ượ")
            variants("iɛ", "yê", "yế", "yề", "yể", "yễ", "yệ")
            variants("uə", "uơ", "uở", "uờ", "uỡ", "uợ")
        }

        private val OFFGLIDES = buildMap {
            variants("aj", "ai", "ái", "ài", "ải", "ãi", "ại")
            variants("ăj", "ay", "áy", "ày", "ảy", "ãy", "ạy")
            variants("aw", "ao", "áo", "ào", "ảo", "ão", "ạo")
            variants("ăw", "au", "áu", "àu", "ảu", "ãu", "ạu")
            variants("ɤ̆j", "ây", "ấy", "ầy", "ẩy", "ẫy", "ậy")
            variants("ɤ̆w", "âu", "ấu", "ầu", "ẩu", "ẫu", "ậu")
            variants("ew", "eo", "éo", "èo", "ẻo", "ẽo", "ẹo")
            variants("iw", "iu", "íu", "ìu", "ỉu", "ĩu", "ịu")
            variants("ɔj", "oi", "ói", "òi", "ỏi", "õi", "ọi")
            variants("oj", "ôi", "ối", "ồi", "ổi", "ỗi", "ội")
            variants("uj", "ui", "úi", "ùi", "ủi", "ũi", "ụi")
            variants("ʷi", "uy", "uý", "uỳ", "uỷ", "uỹ", "uỵ")
            variants("uj", "úy", "ùy", "ủy", "ũy", "ụy")
            variants("ɤj", "ơi", "ới", "ời", "ởi", "ỡi", "ợi")
            variants("ɯj", "ưi", "ứi", "ừi", "ửi", "ữi", "ựi")
            variants("ɯw", "ưu", "ứu", "ừu", "ửu", "ữu", "ựu")
            variants("iəw", "iêu", "iếu", "iều", "iểu", "iễu", "iệu")
            variants("iəw", "yêu", "yếu", "yều", "yểu", "yễu", "yệu")
            variants("uəj", "uôi", "uối", "uồi", "uổi", "uỗi", "uội")
            variants("ɯəj", "ươi", "ưới", "ười", "ưởi", "ưỡi", "ượi")
            variants("ɯəw", "ươu", "ướu", "ườu", "ưởu", "ưỡu", "ượu")
        }

        private val ONGLIDES = buildMap {
            variants("ʷa", "oa", "oá", "oà", "oả", "oã", "oạ", "óa", "òa", "ỏa", "õa", "ọa")
            variants("ʷă", "oă", "oắ", "oằ", "oẳ", "oẵ", "oặ")
            variants("ʷɛ", "oe", "oé", "oè", "oẻ", "oẽ", "oẹ", "óe", "òe", "ỏe", "õe", "ọe")
            variants("ʷa", "ua", "uá", "uà", "uả", "uã", "uạ")
            variants("ʷă", "uă", "uắ", "uằ", "uẳ", "uẵ", "uặ")
            variants("ʷɤ̆", "uâ", "uấ", "uầ", "uẩ", "uẫ", "uậ")
            variants("ʷɛ", "ue", "ué", "uè", "uẻ", "uẽ", "uẹ")
            variants("ʷe", "uê", "uế", "uề", "uể", "uễ", "uệ")
            variants("ʷɤ", "uơ")
            variants("ʷɔ", "uớ", "uờ", "uở", "uỡ", "uợ")
            variants("ʷi", "uy", "uý", "uỳ", "uỷ", "uỹ", "uỵ")
            variants("ʷiə", "uya", "uyá", "uyà", "uyả", "uyã", "uyạ")
            variants("ʷiə", "uyê", "uyế", "uyề", "uyể", "uyễ", "uyệ")
        }

        private val ONOFFGLIDES = buildMap {
            variants("aj", "oai", "oái", "oài", "oải", "oãi", "oại")
            variants("ăj", "oay", "oáy", "oày", "oảy", "oãy", "oạy")
            variants("aw", "oao", "oáo", "oào", "oảo", "oão", "oạo")
            variants("ew", "oeo", "oéo", "oèo", "oẻo", "oẽo", "oẹo")
            variants("aj", "uai", "uái", "uài", "uải", "uãi", "uại")
            variants("ăj", "uay", "uáy", "uày", "uảy", "uãy", "uạy")
            variants("ɤ̆j", "uây", "uấy", "uầy", "uẩy", "uẫy", "uậy")
        }

        private val TONES = buildMap {
            tone(5, "áấắéếíóốớúứý")
            tone(2, "àầằèềìòồờùừỳ")
            tone(4, "ảẩẳẻểỉỏổởủửỷ")
            tone(3, "ãẫẵẽễĩõỗỡũữỹ")
            tone(6, "ạậặẹệịọộợụựỵ")
        }

        private val GI_WORDS = buildMap {
            variants("zi", "gi", "gí", "gì", "gỉ", "gĩ", "gị")
        }

        private val QU_WORDS = buildMap {
            variants("kwi", "quy", "qúy", "qùy", "qủy", "qũy", "qụy")
        }

        private fun MutableMap<String, String>.variants(value: String, vararg keys: String) {
            keys.forEach { key -> put(key, value) }
        }

        private fun MutableMap<Char, Int>.tone(value: Int, characters: String) {
            characters.forEach { character -> put(character, value) }
        }
    }
}
