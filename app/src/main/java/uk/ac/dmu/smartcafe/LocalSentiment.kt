package uk.ac.dmu.smartcafe

import java.util.Locale
import kotlin.math.ln

data class SentimentPrediction(val label: String, val uncalibratedScore: Double)

class LocalSentiment {

    private val positiveSentences = listOf(
        "great coffee fast friendly service",
        "amazing food",
        "excellent taste",
        "love this place",
        "best cafe",
        "delicious cakes and drinks"
    )

    private val negativeSentences = listOf(
        "cold drink slow service",
        "terrible food",
        "worst cafe",
        "hate this place",
        "bad experience",
        "not good at all"
    )

    private val vocab = mutableSetOf<String>()
    private val posWordCounts = mutableMapOf<String, Int>()
    private val negWordCounts = mutableMapOf<String, Int>()
    private var totalPosWords = 0
    private var totalNegWords = 0

    init {
        train(positiveSentences, posWordCounts) { totalPosWords = it }
        train(negativeSentences, negWordCounts) { totalNegWords = it }
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z\\s]"), "")
            .split(Regex("\\s+"))
            .filter { it.length >= 3 }
    }

    private fun train(sentences: List<String>, counts: MutableMap<String, Int>, setTotal: (Int) -> Unit) {
        var total = 0
        for (sentence in sentences) {
            // handle negation basically
            var isNegated = false
            for (word in tokenize(sentence)) {
                if (word == "not") {
                    isNegated = true
                    continue
                }
                val finalWord = if (isNegated) "not_$word" else word
                counts[finalWord] = counts.getOrDefault(finalWord, 0) + 1
                vocab.add(finalWord)
                total++
                isNegated = false // Reset after applying to one word, though crude
            }
        }
        setTotal(total)
    }

    fun predict(text: String): SentimentPrediction? {
        val tokens = tokenize(text)
        var isNegated = false
        val processedTokens = mutableListOf<String>()
        for (word in tokens) {
            if (word == "not") {
                isNegated = true
                continue
            }
            processedTokens.add(if (isNegated) "not_$word" else word)
            isNegated = false
        }

        var knownVocabTokens = 0
        for (token in processedTokens) {
            if (vocab.contains(token)) {
                knownVocabTokens++
            }
        }

        if (knownVocabTokens < 1) {
            return null
        }

        // Laplace smoothing (add-1)
        val v = vocab.size
        var logProbPos = ln(0.5) // Prior
        var logProbNeg = ln(0.5)

        for (token in processedTokens) {
            if (vocab.contains(token)) {
                val posCount = posWordCounts.getOrDefault(token, 0)
                val negCount = negWordCounts.getOrDefault(token, 0)

                logProbPos += ln((posCount + 1.0) / (totalPosWords + v))
                logProbNeg += ln((negCount + 1.0) / (totalNegWords + v))
            }
        }

        return if (logProbPos > logProbNeg) {
            SentimentPrediction("Positive", logProbPos)
        } else {
            SentimentPrediction("Negative", logProbNeg)
        }
    }
}
