package com.example.data.repository

import com.example.data.model.Report
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ReportRepository {

    private val firestore: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }

    suspend fun createReport(report: Report): Result<String> {
        return try {
            val db = firestore ?: throw Exception("قاعدة البيانات غير متاحة حالياً.")
            val docRef = db.collection("reports").document()
            val reportWithId = report.copy(id = docRef.id)
            docRef.set(reportWithId).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "حدث خطأ أثناء نشر البلاغ."))
        }
    }
}
