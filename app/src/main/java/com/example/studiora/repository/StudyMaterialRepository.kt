package com.example.studiora.repository

import com.example.studiora.model.StudyMaterial
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class StudyMaterialRepository {
    private val database = FirebaseDatabase.getInstance().reference

    suspend fun addMaterial(material: StudyMaterial): Result<Unit> {
        return try {
            database.child("materials").child(material.materialId).setValue(material).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMaterialsByCourse(courseId: String): Result<List<StudyMaterial>> {
        return try {
            val snapshot = database.child("materials").get().await()
            val materials = mutableListOf<StudyMaterial>()
            snapshot.children.forEach { child ->
                val material = child.getValue(StudyMaterial::class.java)
                if (material != null && material.courseId == courseId) materials.add(material)
            }
            Result.success(materials)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllMaterials(): Result<List<StudyMaterial>> {
        return try {
            val snapshot = database.child("materials").get().await()
            val materials = mutableListOf<StudyMaterial>()
            snapshot.children.forEach { child ->
                val material = child.getValue(StudyMaterial::class.java)
                if (material != null) materials.add(material)
            }
            Result.success(materials)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMaterial(materialId: String): Result<Unit> {
        return try {
            database.child("materials").child(materialId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

