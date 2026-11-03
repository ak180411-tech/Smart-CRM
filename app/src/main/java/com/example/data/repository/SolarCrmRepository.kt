package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Lead
import com.example.data.model.PipelineStage
import com.example.data.model.Project
import com.example.data.model.ProjectStage
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

class SolarCrmRepository(
    private val db: FirebaseFirestore,
    private val leadRepo: LeadRepository = LeadRepositoryImpl(db)
) : LeadRepository by leadRepo {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    // --- LEADS ---

    fun observeLeads(userId: String): Flow<List<Lead>> = leadRepo.getLeads(userId)

    suspend fun getLead(userId: String, leadId: String): Lead? = leadRepo.getLeadById(userId, leadId)

    suspend fun createLead(userId: String, lead: Lead): Result<String> = leadRepo.addLead(userId, lead)

    override suspend fun updateLead(userId: String, lead: Lead): Result<Unit> = leadRepo.updateLead(userId, lead)

    override suspend fun deleteLead(userId: String, leadId: String): Result<Unit> = leadRepo.deleteLead(userId, leadId)

    // --- PROJECTS ---

    fun observeProjects(userId: String): Flow<List<Project>> {
        val path = "users/$userId/projects"
        return db.collection("users").document(userId).collection("projects")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Project::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
    }

    suspend fun getProject(userId: String, projectId: String): Project? {
        val path = "users/$userId/projects/$projectId"
        return try {
            db.collection("users").document(userId)
                .collection("projects").document(projectId)
                .get()
                .await()
                .toObject(Project::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    suspend fun createProject(userId: String, project: Project): Result<String> {
        val projectId = if (project.id.isNotBlank()) project.id else UUID.randomUUID().toString()
        val docRef = db.collection("users").document(userId).collection("projects").document(projectId)
        return try {
            val toSave = project.copy(
                id = projectId,
                userId = userId,
                createdAt = null,
                updatedAt = null
            )
            docRef.set(toSave).await()
            Result.success(projectId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateProject(userId: String, project: Project): Result<Unit> {
        require(project.id.isNotBlank()) { "Project ID cannot be blank for update" }
        val docRef = db.collection("users").document(userId).collection("projects").document(project.id)
        return try {
            val toSave = project.copy(
                userId = userId,
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

    suspend fun deleteProject(userId: String, projectId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("projects").document(projectId)
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

    suspend fun convertLeadToProject(
        userId: String,
        lead: Lead,
        contractPrice: Double,
        systemSizeKw: Double
    ): Result<String> {
        val projectId = UUID.randomUUID().toString()
        val panelCount = (systemSizeKw * 1000 / 400).toInt().coerceAtLeast(1)
        val project = Project(
            id = projectId,
            userId = userId,
            leadId = lead.id,
            customerName = lead.customerName,
            phone = lead.phone,
            email = lead.email,
            address = lead.address,
            city = lead.city,
            state = lead.state,
            zip = lead.zip,
            systemSizeKw = systemSizeKw,
            panelBrand = "Qcells Q.PEAK DUO 405W",
            panelCount = panelCount,
            inverterType = "Enphase IQ8+ Microinverters",
            batteryModel = "None",
            hasBattery = false,
            totalContractPrice = contractPrice,
            depositPaid = (contractPrice * 0.1).coerceAtLeast(1000.0),
            financingType = "Solar Loan",
            projectStage = ProjectStage.SITE_AUDIT.name,
            utilityProvider = "Local Electric Co.",
            panelUpgradeRequired = false,
            notes = "Converted from Lead: ${lead.notes ?: ""}".trim(),
            createdAt = null,
            updatedAt = null
        )

        val projectResult = createProject(userId, project)
        if (projectResult.isSuccess && lead.id.isNotBlank()) {
            updateLead(userId, lead.copy(pipelineStage = PipelineStage.CONTRACT_SIGNED.name))
        }
        return projectResult
    }
}
