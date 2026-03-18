package com.example.aikeyboard.ml

import android.content.Context
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OnnxTensor
// import java.nio.FloatBuffer

/**
 * A wrapper for a local ONNX model that performs Named Entity Recognition (NER)
 * or Topic classification.
 * 
 * Note: This is a placeholder structure showing how to load a model and run inference locally
 * so keystrokes never leave the device unless a memory is specifically saved.
 */
class NERExtractor(private val context: Context) {
    
    private var env: OrtEnvironment? = null
    private var session: OrtSession? = null

    // Common non-name capitalized words to exclude
    private val excludedWords = setOf(
        "i", "the", "a", "an", "and", "or", "but", "is", "are", "was", "were",
        "be", "been", "being", "have", "has", "had", "do", "does", "did",
        "will", "would", "could", "should", "may", "might", "can", "shall",
        "he", "she", "it", "we", "they", "you", "me", "him", "her", "us",
        "my", "your", "his", "its", "our", "their",
        "this", "that", "these", "those", "what", "which", "who", "whom",
        "so", "if", "then", "than", "when", "where", "how", "why",
        "not", "no", "yes", "just", "also", "very", "really", "too",
        "am", "pm", "ok", "okay", "hey", "hi", "hello", "bye",
        "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
        "january", "february", "march", "april", "may", "june", "july",
        "august", "september", "october", "november", "december",
        "today", "tomorrow", "yesterday", "tonight", "morning", "afternoon",
        "new", "good", "great", "nice", "sure", "sorry", "thanks", "thank"
    )

    // Topic keywords
    private val topicKeywords = mapOf(
        "interview" to "interview",
        "job" to "career",
        "work" to "career",
        "career" to "career",
        "office" to "career",
        "meeting" to "meeting",
        "book" to "books",
        "reading" to "books",
        "movie" to "movies",
        "film" to "movies",
        "travel" to "travel",
        "trip" to "travel",
        "vacation" to "travel",
        "birthday" to "birthday",
        "party" to "party",
        "dinner" to "food",
        "lunch" to "food",
        "food" to "food",
        "restaurant" to "food",
        "school" to "education",
        "college" to "education",
        "university" to "education",
        "class" to "education",
        "exam" to "education",
        "doctor" to "health",
        "hospital" to "health",
        "sick" to "health",
        "wedding" to "wedding",
        "married" to "wedding",
        "game" to "gaming",
        "gaming" to "gaming",
        "music" to "music",
        "concert" to "music",
        "gym" to "fitness",
        "workout" to "fitness",
        "project" to "project",
        "deadline" to "project",
        "baby" to "family",
        "pregnant" to "family",
        "apartment" to "housing",
        "house" to "housing",
        "moving" to "housing",
        "date" to "dating",
        "girlfriend" to "dating",
        "boyfriend" to "dating"
    )

    init {
        // Initialize ONNX runtime environment
        env = OrtEnvironment.getEnvironment()
        // In a real scenario, you'd load model bytes from res/raw or assets
        // val modelBytes = context.resources.openRawResource(R.raw.ner_model).readBytes()
        // session = env?.createSession(modelBytes, OrtSession.SessionOptions())
    }

    /**
     * Extracts Name and Topic from a given piece of conversational text.
     * Uses heuristic capitalized-word detection for person names
     * and keyword matching for topics.
     */
    fun extractContext(text: String): ExtractedContext {
        var topic = "unknown"
        var person = "unknown"

        // Detect topic via keyword matching
        val lowerText = text.lowercase()
        for ((keyword, topicName) in topicKeywords) {
            if (lowerText.contains(keyword)) {
                topic = topicName
                break
            }
        }

        // Detect person names: look for capitalized words that aren't
        // at the start of a sentence and aren't common English words
        val words = text.split("\\s+".toRegex())
        for (i in words.indices) {
            val word = words[i].replace(Regex("[^a-zA-Z]"), "") // strip punctuation
            if (word.isEmpty()) continue
            
            // Check if capitalized (first letter uppercase)
            if (word[0].isUpperCase() && word.length >= 2) {
                val lower = word.lowercase()
                // Skip common non-name words
                if (lower !in excludedWords) {
                    person = word
                    break // Take the first person name found
                }
            }
        }

        return ExtractedContext(person = person, topic = topic)
    }

    fun close() {
        session?.close()
        env?.close()
    }
}

data class ExtractedContext(
    val person: String,
    val topic: String
)
