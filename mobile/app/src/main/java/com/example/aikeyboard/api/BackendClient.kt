package com.example.aikeyboard.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class MemoryRequest(
    val device_id: String,
    val contact_name: String,
    val memory_text: String
)

data class MemoryResponse(
    val id: Int,
    val memory_text: String
)

data class SuggestionRequest(
    val device_id: String,
    val contact_name: String,
    val current_topic: String
)

data class SuggestionResponse(
    val hint: String,
    val relevant_memories: List<String>
)

interface KeyboardApi {
    @POST("/memory")
    suspend fun saveMemory(@Body request: MemoryRequest): MemoryResponse
    
    @GET("/memory/person/{contact_name}")
    suspend fun getMemories(
        @Path("contact_name") contactName: String,
        @Query("device_id") deviceId: String
    ): List<MemoryResponse>

    @POST("/suggestions")
    suspend fun getSuggestions(@Body request: SuggestionRequest): SuggestionResponse
}

object BackendClient {
    // Note: use 10.0.2.2 for Android emulator to access localhost
    private const val BASE_URL = "http://10.0.2.2:8000"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: KeyboardApi = retrofit.create(KeyboardApi::class.java)
}
