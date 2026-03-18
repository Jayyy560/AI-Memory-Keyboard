package com.example.aikeyboard

import android.content.Context
import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.aikeyboard.api.BackendClient
import com.example.aikeyboard.api.MemoryRequest
import com.example.aikeyboard.api.SuggestionRequest
import com.example.aikeyboard.ml.ExtractedContext
import com.example.aikeyboard.ml.NERExtractor
import com.example.aikeyboard.ui.KeyboardView
import kotlinx.coroutines.*

class MemoryKeyboardService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private var composeView: ComposeKeyboardView? = null
    
    // Core AI states
    private var aiHintState = mutableStateOf("")
    private var currentTypedBuffer = StringBuilder()
    private var lastExtractedContext: ExtractedContext? = null

    // Word Suggestions
    private var wordSuggestions = mutableStateListOf<String>()
    private var currentWord = StringBuilder()

    // ML Engine
    private lateinit var nerExtractor: NERExtractor

    // Local storage for memories (works without backend)
    private lateinit var memoryPrefs: SharedPreferences

    // Lifecycle requirements for Compose inside InputMethodService
    private var mLifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    private var mSavedStateRegistryController: SavedStateRegistryController = SavedStateRegistryController.create(this)
    private var mViewModelStore: ViewModelStore = ViewModelStore()

    override val lifecycle: Lifecycle get() = mLifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = mSavedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = mViewModelStore

    // Basic dictionary for word suggestions
    private val commonWords = listOf(
        "the", "that", "this", "then", "them", "they", "there", "their", "these", "those",
        "have", "has", "had", "having",
        "been", "being", "because", "before", "between", "both",
        "about", "after", "again", "also", "another",
        "can", "could", "come", "came",
        "do", "does", "did", "done", "doing", "down",
        "each", "even", "every",
        "for", "from", "find", "first",
        "get", "give", "go", "going", "good", "great",
        "help", "here", "how", "hello",
        "if", "in", "into", "is", "it",
        "just",
        "know", "keep",
        "like", "look", "long", "let",
        "make", "more", "most", "much", "my", "made",
        "new", "no", "not", "now", "need", "never", "nice",
        "of", "on", "one", "only", "or", "other", "our", "out", "over", "okay",
        "people", "please", "place",
        "really", "right",
        "say", "see", "she", "should", "so", "some", "still",
        "take", "tell", "than", "thank", "thanks", "time", "today", "too",
        "up", "us", "use",
        "very",
        "want", "was", "way", "we", "well", "were", "what", "when", "where", "which", "who", "why", "will", "with", "would", "work",
        "yes", "you", "your",
        "hey", "hi", "sure", "yeah", "yep", "nope", "maybe", "sorry",
        "love", "miss", "meet", "meeting", "happy", "birthday", "dinner", "lunch",
        "tomorrow", "yesterday", "tonight", "morning", "afternoon", "evening",
        "friend", "family", "home", "phone", "call", "message", "text"
    )

    override fun onCreate() {
        super.onCreate()
        Log.d("MemoryKeyboard", "onCreate() called")
        mSavedStateRegistryController.performRestore(null)
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        nerExtractor = NERExtractor(this)
        memoryPrefs = getSharedPreferences("ai_keyboard_memories", Context.MODE_PRIVATE)
    }

    override fun onCreateInputView(): View {
        Log.d("MemoryKeyboard", "onCreateInputView() called")
        
        // Critical for Compose in InputMethodService: The DecorView needs the owners!
        val decorView = window.window?.decorView
        if (decorView != null) {
            decorView.setViewTreeLifecycleOwner(this@MemoryKeyboardService)
            decorView.setViewTreeViewModelStoreOwner(this@MemoryKeyboardService)
            decorView.setViewTreeSavedStateRegistryOwner(this@MemoryKeyboardService)
        }

        composeView = ComposeKeyboardView(this) {
            KeyboardView(
                aiSuggestionState = aiHintState.value,
                suggestions = wordSuggestions.toList(),
                onKeyPress = { key -> handleKey(key) },
                onSaveMemoryClick = { handleSaveMemory() },
                onDeletePress = { handleDelete() },
                onEnterPress = { handleEnter() },
                onSuggestionClick = { word -> handleSuggestionClick(word) }
            )
        }.apply {
            setViewTreeLifecycleOwner(this@MemoryKeyboardService)
            setViewTreeViewModelStoreOwner(this@MemoryKeyboardService)
            setViewTreeSavedStateRegistryOwner(this@MemoryKeyboardService)
        }
        
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        return composeView!!
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        Log.d("MemoryKeyboard", "onStartInputView() called")
        currentTypedBuffer.clear()
        currentWord.clear()
        wordSuggestions.clear()
        aiHintState.value = ""
    }

    private fun handleKey(key: String) {
        currentInputConnection?.commitText(key, 1)
        currentTypedBuffer.append(key)
        
        // Track current word for suggestions
        if (key == " ") {
            currentWord.clear()
            wordSuggestions.clear()
        } else {
            currentWord.append(key)
            updateWordSuggestions(currentWord.toString())
        }
        
        // Every keystroke, evaluate privacy-safe local extraction
        evaluateContextLocally(currentTypedBuffer.toString())
    }

    private fun handleDelete() {
        currentInputConnection?.deleteSurroundingText(1, 0)
        if (currentTypedBuffer.isNotEmpty()) {
            currentTypedBuffer.deleteCharAt(currentTypedBuffer.length - 1)
        }
        if (currentWord.isNotEmpty()) {
            currentWord.deleteCharAt(currentWord.length - 1)
            if (currentWord.isNotEmpty()) {
                updateWordSuggestions(currentWord.toString())
            } else {
                wordSuggestions.clear()
            }
        }
    }

    private fun handleEnter() {
        currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
        currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))
        currentTypedBuffer.clear()
        currentWord.clear()
        wordSuggestions.clear()
    }

    private fun handleSuggestionClick(word: String) {
        // Replace the current partial word with the suggestion
        val partial = currentWord.toString()
        if (partial.isNotEmpty()) {
            currentInputConnection?.deleteSurroundingText(partial.length, 0)
        }
        currentInputConnection?.commitText("$word ", 1)
        currentTypedBuffer.append(word.removePrefix(partial)).append(" ")
        currentWord.clear()
        wordSuggestions.clear()
    }

    private fun updateWordSuggestions(prefix: String) {
        if (prefix.length < 2) {
            wordSuggestions.clear()
            return
        }
        val lowerPrefix = prefix.lowercase()
        val matches = commonWords
            .filter { it.startsWith(lowerPrefix) && it != lowerPrefix }
            .take(3)
        wordSuggestions.clear()
        wordSuggestions.addAll(matches)
    }

    private fun handleSaveMemory() {
        val memoryText = currentTypedBuffer.toString().trim()
        if (memoryText.isBlank()) {
            aiHintState.value = "Type something first to save as a memory."
            return
        }

        // Extract context — try to find a name in the text
        val context = nerExtractor.extractContext(memoryText)
        val contactName = if (context.person != "unknown") context.person else "General"

        // Save locally first (works without backend!)
        saveMemoryLocally(contactName, memoryText)

        // Also try backend in background (non-blocking)
        scope.launch(Dispatchers.IO) {
            try {
                BackendClient.api.saveMemory(
                    MemoryRequest(
                        device_id = "device_123",
                        contact_name = contactName,
                        memory_text = memoryText
                    )
                )
                Log.d("MemoryKeyboard", "Memory also saved to backend for $contactName")
            } catch (e: Exception) {
                Log.d("MemoryKeyboard", "Backend unavailable, memory saved locally only: ${e.message}")
            }
        }
    }

    private fun saveMemoryLocally(contactName: String, memoryText: String) {
        val key = "memories_$contactName"
        val existing = memoryPrefs.getStringSet(key, mutableSetOf()) ?: mutableSetOf()
        val updated = existing.toMutableSet()
        updated.add(memoryText)
        memoryPrefs.edit().putStringSet(key, updated).apply()

        aiHintState.value = "💾 Saved memory for $contactName!"
        Log.d("MemoryKeyboard", "Saved locally: [$contactName] $memoryText")
        currentTypedBuffer.clear()
        currentWord.clear()
        wordSuggestions.clear()
    }

    fun getLocalMemories(contactName: String): Set<String> {
        val key = "memories_$contactName"
        return memoryPrefs.getStringSet(key, emptySet()) ?: emptySet()
    }

    private fun evaluateContextLocally(text: String) {
        // Only trigger hint if we have at least a short phrase
        if (text.length < 5) return

        // LOCAL processing for Privacy: Extract topics/names WITHOUT sending text to server
        val context = nerExtractor.extractContext(text)
        lastExtractedContext = context

        if (context.person != "unknown") {
            // Check local storage for memories about this person
            val localMemories = getLocalMemories(context.person)
            if (localMemories.isNotEmpty()) {
                val recentMemory = localMemories.last()
                aiHintState.value = "🧠 ${context.person}: $recentMemory"
            }

            // Also try backend suggestions in background (non-blocking)
            if (context.topic != "unknown") {
                fetchSuggestions(context.person, context.topic)
            }
        }
    }

    private var suggestionJob: Job? = null
    private fun fetchSuggestions(person: String, topic: String) {
        suggestionJob?.cancel()
        suggestionJob = scope.launch(Dispatchers.IO) {
            try {
                val response = BackendClient.api.getSuggestions(
                    SuggestionRequest(
                        device_id = "device_123",
                        contact_name = person,
                        current_topic = topic
                    )
                )
                withContext(Dispatchers.Main) {
                    if (response.hint.isNotEmpty()) {
                        aiHintState.value = response.hint
                    }
                }
            } catch (e: Exception) {
                // Backend unavailable — local memories are already shown, no need to crash
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        nerExtractor.close()
        job.cancel()
    }
}
