package com.example.fitnessrepcounter.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest

object SupabaseClientConfig {
    private const val SUPABASE_URL = "https://tbgxwzufmphejwdpzypr.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRiZ3h3enVmbXBoZWp3ZHB6eXByIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODI1NjAzODEsImV4cCI6MjA5ODEzNjM4MX0.UAj-twPiIUb_iKzcwhvb8OWJfF_lWkM98ulQKF3Y03k"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            install(Postgrest)
            install(Auth)
        }
    }
}
