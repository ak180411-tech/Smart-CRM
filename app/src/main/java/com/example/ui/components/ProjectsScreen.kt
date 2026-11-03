package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Project
import com.example.data.model.ProjectStage
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SolarCyan
import com.example.ui.theme.SolarEmerald
import com.example.ui.theme.SolarGold
import com.example.ui.theme.SolarGoldDark
import com.example.ui.theme.SolarNavy
import com.example.ui.viewmodel.SolarCrmViewModel
import com.example.ui.viewmodel.UiState
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ProjectsScreen(
    viewModel: SolarCrmViewModel,
    projectsState: UiState<List<Project>>,
    searchQuery: String,
    selectedFilter: ProjectStage?,
    onAddProjectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var editingProject by remember { mutableStateOf<Project?>(null) }
    var projectToDelete by remember { mutableStateOf<Project?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Stage Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { viewModel.setProjectFilter(null) },
                    label = { Text("All Installations") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SolarNavy,
                        selectedLabelColor = Color.White
                    )
                )

                ProjectStage.entries.forEach { stage ->
                    FilterChip(
                        selected = selectedFilter == stage,
                        onClick = {
                            viewModel.setProjectFilter(if (selectedFilter == stage) null else stage)
                        },
                        label = { Text(stage.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (stage) {
                                ProjectStage.COMPLETED -> SolarEmerald
                                ProjectStage.PTO_GRANTED -> SolarCyan
                                ProjectStage.INSTALLATION -> SolarGoldDark
                                else -> SolarNavy
                            },
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Projects List Content
            when (projectsState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SolarNavy)
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error: ${projectsState.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                is UiState.Success -> {
                    val filteredProjects = projectsState.data.filter { project ->
                        val matchesFilter = selectedFilter == null || project.projectStage.equals(selectedFilter.name, ignoreCase = true)
                        val matchesSearch = searchQuery.isBlank() ||
                                project.customerName.contains(searchQuery, ignoreCase = true) ||
                                (project.address ?: "").contains(searchQuery, ignoreCase = true) ||
                                (project.city ?: "").contains(searchQuery, ignoreCase = true) ||
                                (project.utilityProvider ?: "").contains(searchQuery, ignoreCase = true)
                        matchesFilter && matchesSearch
                    }

                    if (filteredProjects.isEmpty()) {
                        EmptyProjectsView(
                            onSeedDemo = { viewModel.seedDemoDataIfEmpty() },
                            onAddNew = onAddProjectClick
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(filteredProjects, key = { it.id }) { project ->
                                ProjectCard(
                                    project = project,
                                    onAdvanceStage = { viewModel.advanceProjectStage(project) },
                                    onCall = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${project.phone}"))
                                        context.startActivity(intent)
                                    },
                                    onEdit = { editingProject = project },
                                    onDelete = { projectToDelete = project }
                                )
                            }
                        }
                    }
                }
            }
        }

        // FAB to add project
        FloatingActionButton(
            onClick = onAddProjectClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_project_fab"),
            containerColor = SolarNavy,
            contentColor = Color.White
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add New Installation Project")
        }
    }

    if (editingProject != null) {
        ProjectFormDialog(
            initialProject = editingProject!!,
            onDismiss = { editingProject = null },
            onSave = { updated ->
                viewModel.saveProject(updated)
                editingProject = null
            }
        )
    }

    if (projectToDelete != null) {
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Delete Project") },
            text = { Text("Are you sure you want to delete installation project for '${projectToDelete?.customerName}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        projectToDelete?.id?.let { viewModel.deleteProject(it) }
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProjectCard(
    project: Project,
    onAdvanceStage: () -> Unit,
    onCall: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 } }
    val currentStage = ProjectStage.fromString(project.projectStage)
    val stages = ProjectStage.entries
    val currentStep = currentStage.stepOrder
    val totalSteps = stages.size
    val progress = currentStep.toFloat() / totalSteps.toFloat()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("project_card_${project.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Customer Name & System Size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.customerName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!project.address.isNullOrBlank() || !project.city.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = listOfNotNull(project.address, project.city, project.state).joinToString(", "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // System Size Badge
                Surface(
                    color = SolarGold.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SolarPower,
                            contentDescription = null,
                            tint = SolarGoldDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${project.systemSizeKw ?: 0.0} kW",
                            color = SolarGoldDark,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stage Milestone Progress Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stage $currentStep of $totalSteps: ${currentStage.label}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = when (currentStage) {
                            ProjectStage.COMPLETED -> SolarEmerald
                            ProjectStage.PTO_GRANTED -> SolarCyan
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when (currentStage) {
                        ProjectStage.COMPLETED -> SolarEmerald
                        ProjectStage.PTO_GRANTED -> SolarCyan
                        else -> SolarGoldDark
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Equipment Specs Summary
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Panels: ${project.panelCount ?: 20}x ${project.panelBrand ?: "Solar Panels"}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Inverter: ${project.inverterType ?: "Microinverters"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (project.hasBattery == true) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BatteryChargingFull,
                                    contentDescription = null,
                                    tint = SolarEmerald,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = project.batteryModel ?: "Battery Storage",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SolarEmerald
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Checklist & Utility Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!project.utilityProvider.isNullOrBlank()) {
                    StatusBadge(
                        icon = Icons.Default.ElectricBolt,
                        text = "Utility: ${project.utilityProvider}",
                        color = SolarNavy
                    )
                }
                if (project.panelUpgradeRequired == true) {
                    StatusBadge(
                        icon = Icons.Default.Warning,
                        text = "200A MPU Needed",
                        color = SolarAmber
                    )
                }
            }

            // Target Dates
            if (!project.installationDate.isNullOrBlank() || !project.targetPtoDate.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!project.installationDate.isNullOrBlank()) {
                        Text(
                            text = "Install: ${project.installationDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!project.targetPtoDate.isNullOrBlank()) {
                        Text(
                            text = "Target PTO: ${project.targetPtoDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Financial Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Contract Price",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currencyFormat.format(project.totalContractPrice),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (project.depositPaid != null && project.depositPaid > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Deposit Paid",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currencyFormat.format(project.depositPaid),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = SolarEmerald
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onCall, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call Customer",
                            tint = SolarEmerald
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Project",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Project",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Advance Stage Button
                if (currentStage != ProjectStage.COMPLETED) {
                    Button(
                        onClick = onAdvanceStage,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SolarNavy,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Advance Stage",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Surface(
                        color = SolarEmerald.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SolarEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Project Live",
                                color = SolarEmerald,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Composable
private fun EmptyProjectsView(
    onSeedDemo: () -> Unit,
    onAddNew: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SolarNavy.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                tint = SolarNavy,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Installation Projects",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Manage residential installation workflows, site audits, permits, and PTO.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onSeedDemo) {
                Text("Load Demo Projects")
            }
            Button(
                onClick = onAddNew,
                colors = ButtonDefaults.buttonColors(containerColor = SolarNavy)
            ) {
                Text("Add Project")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectFormDialog(
    initialProject: Project,
    onDismiss: () -> Unit,
    onSave: (Project) -> Unit
) {
    var customerName by remember { mutableStateOf(initialProject.customerName) }
    var phone by remember { mutableStateOf(initialProject.phone) }
    var email by remember { mutableStateOf(initialProject.email ?: "") }
    var address by remember { mutableStateOf(initialProject.address ?: "") }
    var city by remember { mutableStateOf(initialProject.city ?: "") }
    var state by remember { mutableStateOf(initialProject.state ?: "TX") }
    var zip by remember { mutableStateOf(initialProject.zip ?: "") }
    var systemSizeKw by remember { mutableStateOf(initialProject.systemSizeKw?.toString() ?: "9.6") }
    var panelBrand by remember { mutableStateOf(initialProject.panelBrand ?: "REC Alpha Pure-R 420W") }
    var panelCount by remember { mutableStateOf(initialProject.panelCount?.toString() ?: "24") }
    var inverterType by remember { mutableStateOf(initialProject.inverterType ?: "Enphase IQ8+ Microinverters") }
    var hasBattery by remember { mutableStateOf(initialProject.hasBattery ?: false) }
    var batteryModel by remember { mutableStateOf(initialProject.batteryModel ?: "Tesla Powerwall 3") }
    var totalContractPrice by remember { mutableStateOf(initialProject.totalContractPrice.toString()) }
    var depositPaid by remember { mutableStateOf(initialProject.depositPaid?.toString() ?: "5000") }
    var financingType by remember { mutableStateOf(initialProject.financingType ?: "Solar Loan") }
    var stage by remember { mutableStateOf(ProjectStage.fromString(initialProject.projectStage)) }
    var utilityProvider by remember { mutableStateOf(initialProject.utilityProvider ?: "Oncor Electric") }
    var panelUpgradeRequired by remember { mutableStateOf(initialProject.panelUpgradeRequired ?: false) }
    var installationDate by remember { mutableStateOf(initialProject.installationDate ?: "") }
    var targetPtoDate by remember { mutableStateOf(initialProject.targetPtoDate ?: "") }
    var notes by remember { mutableStateOf(initialProject.notes ?: "") }

    var stageDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialProject.id.isBlank()) "New Installation Project" else "Edit Project") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("project_name_input")
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Installation Address") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = state,
                        onValueChange = { state = it },
                        label = { Text("State") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Stage Dropdown
                ExposedDropdownMenuBox(
                    expanded = stageDropdownExpanded,
                    onExpandedChange = { stageDropdownExpanded = !stageDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = stage.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Project Workflow Stage") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stageDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = stageDropdownExpanded,
                        onDismissRequest = { stageDropdownExpanded = false }
                    ) {
                        ProjectStage.entries.forEach { entry ->
                            DropdownMenuItem(
                                text = { Text(entry.label) },
                                onClick = {
                                    stage = entry
                                    stageDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = systemSizeKw,
                        onValueChange = { systemSizeKw = it },
                        label = { Text("System Size (kW)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = panelCount,
                        onValueChange = { panelCount = it },
                        label = { Text("Panel Count") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = panelBrand,
                    onValueChange = { panelBrand = it },
                    label = { Text("Panel Brand / Model") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = inverterType,
                    onValueChange = { inverterType = it },
                    label = { Text("Inverter (e.g. Enphase IQ8+)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Battery Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Includes Battery Storage")
                    Switch(
                        checked = hasBattery,
                        onCheckedChange = { hasBattery = it }
                    )
                }

                if (hasBattery) {
                    OutlinedTextField(
                        value = batteryModel,
                        onValueChange = { batteryModel = it },
                        label = { Text("Battery Model (e.g. Tesla Powerwall 3)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalContractPrice,
                        onValueChange = { totalContractPrice = it },
                        label = { Text("Contract Price ($) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = depositPaid,
                        onValueChange = { depositPaid = it },
                        label = { Text("Deposit Paid ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = utilityProvider,
                    onValueChange = { utilityProvider = it },
                    label = { Text("Utility Company (Interconnection)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("200A Main Panel Upgrade (MPU)")
                    Switch(
                        checked = panelUpgradeRequired,
                        onCheckedChange = { panelUpgradeRequired = it }
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = installationDate,
                        onValueChange = { installationDate = it },
                        label = { Text("Install Date (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = targetPtoDate,
                        onValueChange = { targetPtoDate = it },
                        label = { Text("Target PTO Date") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Installation Notes & Crew Info") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = totalContractPrice.toDoubleOrNull() ?: 0.0
                    if (customerName.isNotBlank() && phone.isNotBlank() && price >= 0.0) {
                        val toSave = initialProject.copy(
                            customerName = customerName.trim(),
                            phone = phone.trim(),
                            email = email.trim().ifEmpty { null },
                            address = address.trim().ifEmpty { null },
                            city = city.trim().ifEmpty { null },
                            state = state.trim().ifEmpty { null },
                            zip = zip.trim().ifEmpty { null },
                            systemSizeKw = systemSizeKw.toDoubleOrNull() ?: 8.0,
                            panelBrand = panelBrand.trim().ifEmpty { null },
                            panelCount = panelCount.toIntOrNull() ?: 20,
                            inverterType = inverterType.trim().ifEmpty { null },
                            hasBattery = hasBattery,
                            batteryModel = if (hasBattery) batteryModel.trim().ifEmpty { null } else "None",
                            totalContractPrice = price,
                            depositPaid = depositPaid.toDoubleOrNull() ?: 0.0,
                            financingType = financingType.trim().ifEmpty { null },
                            projectStage = stage.name,
                            utilityProvider = utilityProvider.trim().ifEmpty { null },
                            panelUpgradeRequired = panelUpgradeRequired,
                            installationDate = installationDate.trim().ifEmpty { null },
                            targetPtoDate = targetPtoDate.trim().ifEmpty { null },
                            notes = notes.trim().ifEmpty { null }
                        )
                        onSave(toSave)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SolarNavy),
                enabled = customerName.isNotBlank() && phone.isNotBlank() && totalContractPrice.isNotBlank()
            ) {
                Text("Save Project")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
