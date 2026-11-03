package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Lead
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository interface for managing solar project prospect leads in Firestore.
 */
interface LeadRepository {
    fun getLeads(userId: String): Flow<List<Lead>>
    suspend fun getLeadById(userId: String, leadId: String): Lead?
    suspend fun addLead(userId: String, lead: Lead): Result<String>
    suspend fun updateLead(userId: String, lead: Lead): Result<Unit>
    suspend fun deleteLead(userId: String, leadId: String): Result<Unit>
}

/**
 * Firestore implementation of [LeadRepository].
 * Interacts with Cloud Firestore subcollection: /users/{userId}/leads/{leadId}
 */
class LeadRepositoryImpl(
    private val db: FirebaseFirestore
) : LeadRepository {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    override fun getLeads(userId: String): Flow<List<Lead>> {
        val path = "users/$userId/leads"
        return db.collection("users").document(userId).collection("leads")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Lead::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
    }

    override suspend fun getLeadById(userId: String, leadId: String): Lead? {
        val path = "users/$userId/leads/$leadId"
        return try {
            db.collection("users").document(userId)
                .collection("leads").document(leadId)
                .get()
                .await()
                .toObject(Lead::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    override suspend fun addLead(userId: String, lead: Lead): Result<String> {
        val leadId = if (lead.id.isNotBlank()) lead.id else UUID.randomUUID().toString()
        val docRef = db.collection("users").document(userId).collection("leads").document(leadId)
        return try {
            val resolvedName = lead.name.ifBlank { lead.customerName }
            val resolvedContact = lead.contact.ifBlank { lead.phone }
            val resolvedStatus = lead.status.ifBlank { lead.pipelineStage }
            val resolvedProperty = lead.propertyDetails.ifBlank {
                listOfNotNull(lead.address, lead.city, lead.state, lead.roofType).joinToString(", ")
            }

            val toSave = lead.copy(
                id = leadId,
                userId = userId,
                name = resolvedName,
                customerName = resolvedName,
                contact = resolvedContact,
                phone = resolvedContact,
                status = resolvedStatus,
                pipelineStage = resolvedStatus,
                propertyDetails = resolvedProperty,
                createdAt = null,
                updatedAt = null
            )
            docRef.set(toSave).await()
            Result.success(leadId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    override suspend fun updateLead(userId: String, lead: Lead): Result<Unit> {
        require(lead.id.isNotBlank()) { "Lead ID cannot be blank for update" }
        val docRef = db.collection("users").document(userId).collection("leads").document(lead.id)
        return try {
            val resolvedName = lead.name.ifBlank { lead.customerName }
            val resolvedContact = lead.contact.ifBlank { lead.phone }
            val resolvedStatus = lead.status.ifBlank { lead.pipelineStage }
            val resolvedProperty = lead.propertyDetails.ifBlank {
                listOfNotNull(lead.address, lead.city, lead.state, lead.roofType).joinToString(", ")
            }

            val toSave = lead.copy(
                userId = userId,
                name = resolvedName,
                customerName = resolvedName,
                contact = resolvedContact,
                phone = resolvedContact,
                status = resolvedStatus,
                pipelineStage = resolvedStatus,
                propertyDetails = resolvedProperty,
                updatedAt = null
            )
            docRef.set(toSave).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    override suspend fun deleteLead(userId: String, leadId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("leads").document(leadId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
