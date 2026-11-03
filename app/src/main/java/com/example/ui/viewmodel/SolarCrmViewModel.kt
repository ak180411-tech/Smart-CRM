package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Lead
import com.example.data.model.PipelineStage
import com.example.data.model.Project
import com.example.data.model.ProjectStage
import com.example.data.repository.SolarCrmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class SolarCrmViewModel(
    private val repository: SolarCrmRepository,
    val currentUserId: String
) : ViewModel() {

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPipelineFilter = MutableStateFlow<PipelineStage?>(null)
    val selectedPipelineFilter: StateFlow<PipelineStage?> = _selectedPipelineFilter.asStateFlow()

    private val _selectedProjectFilter = MutableStateFlow<ProjectStage?>(null)
    val selectedProjectFilter: StateFlow<ProjectStage?> = _selectedProjectFilter.asStateFlow()

    val leads: StateFlow<UiState<List<Lead>>> = repository.observeLeads(currentUserId)
        .map<List<Lead>, UiState<List<Lead>>> { UiState.Success(it) }
        .catch { error ->
            Log.e(TAG, "Error observing leads", error)
            emit(UiState.Error(error.localizedMessage ?: "Failed to observe leads"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val projects: StateFlow<UiState<List<Project>>> = repository.observeProjects(currentUserId)
        .map<List<Project>, UiState<List<Project>>> { UiState.Success(it) }
        .catch { error ->
            Log.e(TAG, "Error observing projects", error)
            emit(UiState.Error(error.localizedMessage ?: "Failed to observe projects"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setPipelineFilter(stage: PipelineStage?) {
        _selectedPipelineFilter.value = stage
    }

    fun setProjectFilter(stage: ProjectStage?) {
        _selectedProjectFilter.value = stage
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun saveLead(lead: Lead, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = if (lead.id.isBlank()) {
                repository.createLead(currentUserId, lead)
            } else {
                repository.updateLead(currentUserId, lead)
            }
            if (result.isSuccess) {
                _statusMessage.value = "Lead saved successfully"
                onComplete(true)
            } else {
                _statusMessage.value = "Failed to save lead: ${result.exceptionOrNull()?.message}"
                onComplete(false)
            }
        }
    }

    fun deleteLead(leadId: String) {
        viewModelScope.launch {
            val result = repository.deleteLead(currentUserId, leadId)
            if (result.isSuccess) {
                _statusMessage.value = "Lead deleted"
            } else {
                _statusMessage.value = "Failed to delete lead: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun saveProject(project: Project, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = if (project.id.isBlank()) {
                repository.createProject(currentUserId, project)
            } else {
                repository.updateProject(currentUserId, project)
            }
            if (result.isSuccess) {
                _statusMessage.value = "Installation project saved"
                onComplete(true)
            } else {
                _statusMessage.value = "Failed to save project: ${result.exceptionOrNull()?.message}"
                onComplete(false)
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            val result = repository.deleteProject(currentUserId, projectId)
            if (result.isSuccess) {
                _statusMessage.value = "Project removed"
            } else {
                _statusMessage.value = "Failed to delete project: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun advanceProjectStage(project: Project) {
        val currentStage = ProjectStage.fromString(project.projectStage)
        val stages = ProjectStage.entries
        val currentIndex = stages.indexOf(currentStage)
        if (currentIndex < stages.size - 1) {
            val nextStage = stages[currentIndex + 1]
            saveProject(project.copy(projectStage = nextStage.name))
        }
    }

    fun convertLeadToProject(lead: Lead, contractPrice: Double, systemSizeKw: Double, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.convertLeadToProject(currentUserId, lead, contractPrice, systemSizeKw)
            if (result.isSuccess) {
                _statusMessage.value = "Lead converted to active installation project!"
                onComplete(true)
            } else {
                _statusMessage.value = "Conversion failed: ${result.exceptionOrNull()?.message}"
                onComplete(false)
            }
        }
    }

    fun seedDemoDataIfEmpty() {
        viewModelScope.launch {
            val currentLeads = (leads.value as? UiState.Success)?.data ?: emptyList()
            val currentProjects = (projects.value as? UiState.Success)?.data ?: emptyList()

            if (currentLeads.isEmpty()) {
                val demoLeads = listOf(
                    Lead(
                        customerName = "Marcus & Elena Vance",
                        phone = "(512) 555-0192",
                        email = "elena.vance@example.com",
                        address = "4208 Whispering Oaks Way",
                        city = "Austin",
                        state = "TX",
                        zip = "78746",
                        monthlyElectricBill = 320.0,
                        averageKWhUsage = 1450.0,
                        roofType = "Composition Shingle",
                        sunExposure = "High",
                        pipelineStage = PipelineStage.PROPOSAL_SENT.name,
                        estimatedSystemSizeKw = 10.4,
                        estimatedValue = 31200.0,
                        notes = "Wants battery backup for power grid resilience. High summer AC load.",
                        nextFollowUp = "Tomorrow 2:00 PM"
                    ),
                    Lead(
                        customerName = "David & Chloe Chen",
                        phone = "(415) 555-8391",
                        email = "david.chen@example.com",
                        address = "742 Skyline Terrace",
                        city = "San Jose",
                        state = "CA",
                        zip = "95123",
                        monthlyElectricBill = 410.0,
                        averageKWhUsage = 1600.0,
                        roofType = "Concrete Tile",
                        sunExposure = "High",
                        pipelineStage = PipelineStage.SURVEY_SCHEDULED.name,
                        estimatedSystemSizeKw = 12.0,
                        estimatedValue = 38500.0,
                        notes = "2 EVs at home charging overnight. Interested in NEM 3.0 battery arbitrage.",
                        nextFollowUp = "Friday Site Audit"
                    ),
                    Lead(
                        customerName = "Robert Sterling",
                        phone = "(602) 555-4720",
                        email = "rsterling@example.com",
                        address = "1840 E Desert Willow Dr",
                        city = "Phoenix",
                        state = "AZ",
                        zip = "85044",
                        monthlyElectricBill = 260.0,
                        averageKWhUsage = 1100.0,
                        roofType = "Tile",
                        sunExposure = "High",
                        pipelineStage = PipelineStage.CONTACTED.name,
                        estimatedSystemSizeKw = 8.8,
                        estimatedValue = 26400.0,
                        notes = "Requested quote comparison against solar lease options.",
                        nextFollowUp = "Next Monday"
                    ),
                    Lead(
                        customerName = "Amara Okonjo",
                        phone = "(720) 555-6188",
                        email = "amara.o@example.com",
                        address = "319 Meadow Creek Circle",
                        city = "Denver",
                        state = "CO",
                        zip = "80209",
                        monthlyElectricBill = 195.0,
                        averageKWhUsage = 850.0,
                        roofType = "Standing Seam Metal",
                        sunExposure = "Moderate",
                        pipelineStage = PipelineStage.NEW_LEAD.name,
                        estimatedSystemSizeKw = 6.4,
                        estimatedValue = 21000.0,
                        notes = "Inquired via web inquiry. Newer roof installed last year."
                    )
                )
                demoLeads.forEach { repository.createLead(currentUserId, it) }
            }

            if (currentProjects.isEmpty()) {
                val demoProjects = listOf(
                    Project(
                        customerName = "Gregory & Linda Miller",
                        phone = "(214) 555-3849",
                        email = "linda.miller@example.com",
                        address = "904 Lakewood Blvd",
                        city = "Dallas",
                        state = "TX",
                        zip = "75214",
                        systemSizeKw = 9.6,
                        panelBrand = "REC Alpha Pure-R 420W",
                        panelCount = 24,
                        inverterType = "Enphase IQ8+ Microinverters",
                        batteryModel = "Tesla Powerwall 3 (13.5 kWh)",
                        hasBattery = true,
                        totalContractPrice = 33800.0,
                        depositPaid = 5000.0,
                        financingType = "Solar Loan (25yr @ 4.99%)",
                        projectStage = ProjectStage.INSTALLATION.name,
                        utilityProvider = "Oncor Electric",
                        panelUpgradeRequired = true,
                        installationDate = "2026-11-04",
                        targetPtoDate = "2026-11-20",
                        notes = "Crew scheduled for 2-day install. 200A MPU scheduled with electrician on day 1."
                    ),
                    Project(
                        customerName = "Dr. Kevin Tran",
                        phone = "(858) 555-7201",
                        email = "ktran@example.com",
                        address = "1124 Torrey Pines Vista",
                        city = "San Diego",
                        state = "CA",
                        zip = "92037",
                        systemSizeKw = 11.2,
                        panelBrand = "Qcells Q.PEAK DUO 405W",
                        panelCount = 28,
                        inverterType = "SolarEdge Energy Hub 10kW",
                        batteryModel = "SolarEdge Home Battery 10kWh",
                        hasBattery = true,
                        totalContractPrice = 39200.0,
                        depositPaid = 15000.0,
                        financingType = "Cash",
                        projectStage = ProjectStage.PTO_GRANTED.name,
                        utilityProvider = "SDG&E",
                        panelUpgradeRequired = false,
                        installationDate = "2026-10-18",
                        targetPtoDate = "2026-11-02",
                        notes = "City inspection passed with zero corrections. Net meter installed by utility!"
                    ),
                    Project(
                        customerName = "Patricia Hayes",
                        phone = "(407) 555-9011",
                        email = "phayes@example.com",
                        address = "523 Lakeview Park Dr",
                        city = "Orlando",
                        state = "FL",
                        zip = "32804",
                        systemSizeKw = 7.6,
                        panelBrand = "Silfab Solar Prime 380W",
                        panelCount = 20,
                        inverterType = "Enphase IQ8M",
                        batteryModel = "None",
                        hasBattery = false,
                        totalContractPrice = 24500.0,
                        depositPaid = 3500.0,
                        financingType = "Solar Loan",
                        projectStage = ProjectStage.ENGINEERING_PERMITTING.name,
                        utilityProvider = "Duke Energy",
                        panelUpgradeRequired = false,
                        installationDate = "2026-11-15",
                        notes = "Structural engineering stamp received. Plan set submitted to City building department."
                    )
                )
                demoProjects.forEach { repository.createProject(currentUserId, it) }
            }
        }
    }

    private companion object {
        const val TAG = "SolarCrmVM"
    }
}
