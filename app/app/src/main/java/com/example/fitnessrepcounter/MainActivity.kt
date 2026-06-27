package com.example.fitnessrepcounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.fitnessrepcounter.data.prefs.LockPreferences
import com.example.fitnessrepcounter.data.supabase.SupabaseRepository
import com.example.fitnessrepcounter.theme.FitnessRepCounterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Sign in anonymously to Supabase so we have a user_id for RLS
    lifecycleScope.launch {
        val repo = SupabaseRepository()
        repo.signInAnonymously()
    }
    
    val lockPrefs = LockPreferences(this)
    if (lockPrefs.isLockedToday()) {
        try {
            startLockTask()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    enableEdgeToEdge()
    setContent {
      FitnessRepCounterTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }
}
