package com.example.fitnessrepcounter.data.supabase

import com.example.fitnessrepcounter.data.supabase.models.RepLogDto
import com.example.fitnessrepcounter.data.supabase.models.WorkoutSessionDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class SupabaseRepository {
    
    private val client: SupabaseClient = SupabaseClientConfig.client
    
    suspend fun signInAnonymously(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Check if already logged in
            if (client.auth.currentSessionOrNull() != null) {
                return@withContext Result.success(Unit)
            }
            
            client.auth.signInAnonymously()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getCurrentUserId(): String? {
        return client.auth.currentSessionOrNull()?.user?.id
    }
    
    suspend fun saveWorkoutSession(
        sessionDto: WorkoutSessionDto,
        repLogs: List<RepLogDto>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Ensure user is logged in
            val userId = getCurrentUserId() ?: throw Exception("User not authenticated")
            
            // 2. We generate an ID for the session if it doesn't have one, or just let DB generate it.
            // Since we need the ID to insert RepLogs with the foreign key, we can generate a UUID locally.
            val sessionId = sessionDto.id ?: UUID.randomUUID().toString()
            
            val finalSessionDto = sessionDto.copy(
                id = sessionId,
                userId = userId
            )
            
            // 3. Insert Session
            client.postgrest["workout_sessions"]
                .insert(finalSessionDto)
                
            // 4. Insert Rep Logs with the session ID
            if (repLogs.isNotEmpty()) {
                val finalRepLogs = repLogs.map { 
                    it.copy(sessionId = sessionId)
                }
                client.postgrest["rep_logs"]
                    .insert(finalRepLogs)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
