package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PipelineStage(val label: String, val badgeColorHex: Long) {
    NEW_LEAD("New Lead", 0xFF2196F3),
    CONTACTED("Contacted", 0xFF00BCD4),
    SURVEY_SCHEDULED("Site Survey", 0xFF9C27B0),
    PROPOSAL_SENT("Proposal Sent", 0xFFFF9800),
    CONTRACT_SIGNED("Contract Signed", 0xFF4CAF50),
    LOST("Lost", 0xFF9E9E9E);

    companion object {
        fun fromString(value: String): PipelineStage {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NEW_LEAD
        }
    }
}

enum class ProjectStage(val label: String, val stepOrder: Int) {
    SITE_AUDIT("Site Audit", 1),
    ENGINEERING_PERMITTING("Engineering & Permits", 2),
    PERMIT_APPROVED("Permits Approved", 3),
    INSTALLATION("Installation Active", 4),
    INSPECTION("City / AHJ Inspection", 5),
    PTO_GRANTED("Permission to Operate (PTO)", 6),
    COMPLETED("Completed & Live", 7);

    companion object {
        fun fromString(value: String): ProjectStage {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SITE_AUDIT
        }
    }
}

@IgnoreExtraProperties
data class Lead(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val customerName: String = "",
    val contact: String = "",
    val phone: String = "",
    val email: String? = null,
    val status: String = PipelineStage.NEW_LEAD.name,
    val pipelineStage: String = PipelineStage.NEW_LEAD.name,
    val propertyDetails: String = "",
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zip: String? = null,
    val monthlyElectricBill: Double? = null,
    val averageKWhUsage: Double? = null,
    val roofType: String? = "Composition Shingle",
    val sunExposure: String? = "High",
    val estimatedSystemSizeKw: Double? = null,
    val estimatedValue: Double? = null,
    val notes: String? = null,
    val nextFollowUp: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val displayName: String
        get() = name.ifBlank { customerName }

    val displayContact: String
        get() = contact.ifBlank { phone }

    val displayStatus: String
        get() = status.ifBlank { pipelineStage }

    val displayPropertyDetails: String
        get() = propertyDetails.ifBlank {
            listOfNotNull(address, city, state, roofType).filter { it.isNotBlank() }.joinToString(", ")
        }
}

@IgnoreExtraProperties
data class Project(
    @DocumentId val id: String = "",
    val userId: String = "",
    val leadId: String? = null,
    val customerName: String = "",
    val phone: String = "",
    val email: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zip: String? = null,
    val systemSizeKw: Double? = 8.0,
    val panelBrand: String? = "REC Alpha Pure-R 420W",
    val panelCount: Int? = 20,
    val inverterType: String? = "Enphase IQ8+",
    val batteryModel: String? = "None",
    val hasBattery: Boolean? = false,
    val totalContractPrice: Double = 0.0,
    val depositPaid: Double? = 0.0,
    val financingType: String? = "Solar Loan",
    val projectStage: String = ProjectStage.SITE_AUDIT.name,
    val utilityProvider: String? = "PG&E",
    val panelUpgradeRequired: Boolean? = false,
    val installationDate: String? = null,
    val targetPtoDate: String? = null,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
