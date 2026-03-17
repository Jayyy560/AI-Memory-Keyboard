package com.example.aikeyboard

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
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

    // ML Engine
    private lateinit var nerExtractor: NERExtractor

    // Lifecycle requirements for Compose inside InputMethodService
    private var mLifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    private var mSavedStateRegistryController: SavedStateRegistryController = SavedStateRegistryController.create(this)
    private var mViewModelStore: ViewModelStore = ViewModelStore()

    override val lifecycle: Lifecycle get() = mLifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = mSavedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = mViewModelStore

    override fun onCreate() {
        super.onCreate()
        Log.d("MemoryKeyboard", "onCreate() called")
        mSavedStateRegistryController.performRestore(null)
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        nerExtractor = NERExtractor(this)
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
                onKeyPress = { key -> handleKey(key) },
                onSaveMemoryClick = { handleSaveMemory() },
                onDeletePress = { handleDelete() },
                onEnterPress = { handleEnter() }
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
        aiHintState.value = ""
    }

    private fun handleKey(key: String) {
        currentInputConnection?.commitText(key, 1)
        currentTypedBuffer.append(key)
        
        // Every keystroke, evaluate privacy-safe local extraction
        // In reality, you'd likely debounce this or look at specific trigger words
        evaluateContextLocally(currentTypedBuffer.toString())
    }

    private fun handleDelete() {
        currentInputConnection?.deleteSurroundingText(1, 0)
        if (currentTypedBuffer.isNotEmpty()) {
            currentTypedBuffer.deleteCharAt(currentTypedBuffer.length - 1)
        }
    }

    private fun handleEnter() {
        currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
        currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))
        currentTypedBuffer.clear()
    }

    private fun handleSaveMemory() {
        val context = lastExtractedContext
        val memoryText = currentTypedBuffer.toString()
        if (context == null || context.person == "unknown" || memoryText.isBlank()) {
            aiHintState.value = "Need a name and text to save."
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                // Send explicit "Saved Memory" to Backend
                BackendClient.api.saveMemory(
                    MemoryRequest(
                        device_id = "device_123", // Example hardcoded device id
                        contact_name = context.person,
                        memory_text = memoryText
                    )
                )
                withContext(Dispatchers.Main) {
                    aiHintState.value = "Saved memory for ${context.person}!"
                    currentTypedBuffer.clear()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    aiHintState.value = "Failed to save memory."
                }
            }
        }
    }

    private fun evaluateContextLocally(text: String) {
        // Only trigger hint if we have at least a short phrase
        if (text.length < 5) return

        // LOCAL processing for Privacy: Extract topics/names WITHOUT sending text to server
        val context = nerExtractor.extractContext(text)
        lastExtractedContext = context

        if (context.person != "unknown" && context.topic != "unknown") {
            // Once locally detected, fetch hints about that specific person & topic
            fetchSuggestions(context.person, context.topic)
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
                // Ignore network failures for typing performance
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
