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

    init {
        // Initialize ONNX runtime environment
        env = OrtEnvironment.getEnvironment()
        // In a real scenario, you'd load model bytes from res/raw or assets
        // val modelBytes = context.resources.openRawResource(R.raw.ner_model).readBytes()
        // session = env?.createSession(modelBytes, OrtSession.SessionOptions())
    }

    /**
     * Extracts Name and Topic from a given piece of conversational text.
     */
    fun extractContext(text: String): ExtractedContext {
        // Mock implementation for demonstration.
        // In reality, you would tokenize the text into a float buffer, create an OnnxTensor,
        // and run model inference to extract entities.
        
        var topic = "unknown"
        var person = "unknown"

        if (text.contains("interview", ignoreCase = true)) {
            topic = "interview"
        } else if (text.contains("book", ignoreCase = true)) {
            topic = "books"
        }

        if (text.contains("Sam", ignoreCase = true)) {
            person = "Sam"
        } else if (text.contains("Alice", ignoreCase = true)) {
            person = "Alice"
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
