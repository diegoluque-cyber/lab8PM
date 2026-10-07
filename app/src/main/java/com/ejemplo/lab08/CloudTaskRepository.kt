package com.ejemplo.lab08

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CloudTaskRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("tasks")

    suspend fun syncTaskToCloud(task: Task) {
        val taskData = hashMapOf(
            "id" to task.id,
            "description" to task.description,
            "isCompleted" to task.isCompleted
        )
        collection.document(task.id.toString()).set(taskData).await()
    }

    suspend fun deleteTaskFromCloud(taskId: Int) {
        collection.document(taskId.toString()).delete().await()
    }
}