package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.data.FamTasksRepository
import com.example.ui.MainApp
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val databaseId = getString(R.string.firestore_database_id)
        val firestore = FirebaseFirestore.getInstance(databaseId)
        val repository = FamTasksRepository(firestore)

        setContent {
            MainApp(repository = repository)
        }
    }
}
