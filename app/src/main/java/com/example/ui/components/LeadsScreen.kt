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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Roofing
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.example.data.model.Lead
import com.example.data.model.PipelineStage
import com.example.ui.theme.SolarEmerald
import com.example.ui.theme.SolarGold
import com.example.ui.theme.SolarGoldDark
import com.example.ui.theme.SolarNavy
import com.example.ui.viewmodel.SolarCrmViewModel
import com.example.ui.viewmodel.UiState
import java.text.NumberFormat
import java.util.Locale

@Composable
fun LeadsScreen(
    viewModel: SolarCrmViewModel,
    leadsState: UiState<List<Lead>>,
    searchQuery: String,
    selectedFilter: PipelineStage?,
    onAddLeadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var editingLead by remember { mutableStateOf<Lead?>(null) }
    var leadToConvert by remember { mutableStateOf<Lead?>(null) }
    var leadToDelete by remember { mutableStateOf<Lead?>(null) }

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
                    onClick = { viewModel.setPipelineFilter(null) },
                    label = { Text("All Leads") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SolarGoldDark,
                        selectedLabelColor = Color.White
                    )
                )

                PipelineStage.entries.forEach { stage ->
                    FilterChip(
                        selected = selectedFilter == stage,
                        onClick = {
                            viewModel.setPipelineFilter(if (selectedFilter == stage) null else stage)
                        },
                        label = { Text(stage.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(stage.badgeColorHex),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Leads Content
            when (leadsState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SolarGoldDark)
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error: ${leadsState.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                is UiState.Success -> {
                    val filteredLeads = leadsState.data.filter { lead ->
                        val matchesFilter = selectedFilter == null || lead.pipelineStage.equals(selectedFilter.name, ignoreCase = true)
                        val matchesSearch = searchQuery.isBlank() ||
                                lead.customerName.contains(searchQuery, ignoreCase = true) ||
                                (lead.address ?: "").contains(searchQuery, ignoreCase = true) ||
                                (lead.city ?: "").contains(searchQuery, ignoreCase = true) ||
                                (lead.phone).contains(searchQuery, ignoreCase = true)
                        matchesFilter && matchesSearch
                    }

                    if (filteredLeads.isEmpty()) {
                        EmptyLeadsView(
                            onSeedDemo = { viewModel.seedDemoDataIfEmpty() },
                            onAddNew = onAddLeadClick
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredLeads, key = { it.id }) { lead ->
                                LeadCard(
                                    lead = lead,
                                    onCall = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${lead.phone}"))
                                        context.startActivity(intent)
                                    },
                                    onConvert = { leadToConvert = lead },
                                    onEdit = { editingLead = lead },
                                    onDelete = { leadToDelete = lead }
                                )
                            }
                        }
                    }
                }
            }
        }

        // FAB to add lead
        FloatingActionButton(
            onClick = onAddLeadClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_lead_fab"),
            containerColor = SolarGoldDark,
            contentColor = Color.White
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add New Lead")
        }
    }

    // Edit Lead Dialog
    if (editingLead != null) {
        LeadFormDialog(
            initialLead = editingLead!!,
            onDismiss = { editingLead = null },
            onSave = { updated ->
                viewModel.saveLead(updated)
                editingLead = null
            }
        )
    }

    // Convert to Project Dialog
    if (leadToConvert != null) {
        val lead = leadToConvert!!
        var contractPriceInput by remember {
            mutableStateOf(lead.estimatedValue?.toInt()?.toString() ?: "28000")
        }
        var systemSizeInput by remember {
            mutableStateOf(lead.estimatedSystemSizeKw?.toString() ?: "8.4")
        }

        AlertDialog(
            onDismissRequest = { leadToConvert = null },
            title = { Text("Convert Lead to Active Installation") },
            text = {
                Column {
                    Text(
                        text = "Create residential solar installation project for ${lead.customerName}.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = contractPriceInput,
                        onValueChange = { contractPriceInput = it },
                        label = { Text("Total Contract Price ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = systemSizeInput,
                        onValueChange = { systemSizeInput = it },
                        label = { Text("System Size (kW)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = contractPriceInput.toDoubleOrNull() ?: 25000.0
                        val size = systemSizeInput.toDoubleOrNull() ?: 8.0
                        viewModel.convertLeadToProject(lead, price, size)
                        leadToConvert = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SolarEmerald)
                ) {
                    Text("Confirm & Create Project")
                }
            },
            dismissButton = {
                TextButton(onClick = { leadToConvert = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (leadToDelete != null) {
        AlertDialog(
            onDismissRequest = { leadToDelete = null },
            title = { Text("Delete Lead") },
            text = { Text("Are you sure you want to delete lead '${leadToDelete?.customerName}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        leadToDelete?.id?.let { viewModel.deleteLead(it) }
                        leadToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { leadToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LeadCard(
    lead: Lead,
    onCall: () -> Unit,
    onConvert: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 } }
    val stage = PipelineStage.fromString(lead.pipelineStage)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lead_card_${lead.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Customer Name & Pipeline Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lead.customerName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!lead.city.isNullOrBlank() || !lead.state.isNullOrBlank()) {
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
                                text = listOfNotNull(lead.address, lead.city, lead.state).joinToString(", "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Stage Badge
                Surface(
                    color = Color(stage.badgeColorHex),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stage.label,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metrics Row: kW, Value, Electric Bill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (lead.estimatedValue != null) {
                    MetricPill(
                        label = "Est. Value",
                        value = currencyFormat.format(lead.estimatedValue),
                        color = SolarGoldDark
                    )
                }
                if (lead.estimatedSystemSizeKw != null) {
                    MetricPill(
                        label = "System Size",
                        value = "${lead.estimatedSystemSizeKw} kW",
                        color = SolarNavy
                    )
                }
                if (lead.monthlyElectricBill != null) {
                    MetricPill(
                        label = "Electric Bill",
                        value = "$${lead.monthlyElectricBill.toInt()}/mo",
                        color = SolarEmerald
                    )
                }
            }

            // Specs badges: Roof & Sun Exposure
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!lead.roofType.isNullOrBlank()) {
                    SpecTag(icon = Icons.Default.Roofing, label = lead.roofType)
                }
                if (!lead.sunExposure.isNullOrBlank()) {
                    SpecTag(icon = Icons.Default.WbSunny, label = "${lead.sunExposure} Sun")
                }
            }

            if (!lead.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = lead.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!lead.nextFollowUp.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Next: ",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SolarGoldDark
                    )
                    Text(
                        text = lead.nextFollowUp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                            contentDescription = "Call Homeowner",
                            tint = SolarEmerald
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Lead",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Lead",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Convert to Project Action
                Button(
                    onClick = onConvert,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SolarGoldDark,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Transform,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "To Project",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}

@Composable
private fun SpecTag(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyLeadsView(
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
                .background(SolarGold.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = SolarGoldDark,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Leads in Pipeline",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Track residential consultations, rooftop surveys, and proposals.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onSeedDemo) {
                Text("Load Demo Leads")
            }
            Button(
                onClick = onAddNew,
                colors = ButtonDefaults.buttonColors(containerColor = SolarGoldDark)
            ) {
                Text("Add Lead")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadFormDialog(
    initialLead: Lead,
    onDismiss: () -> Unit,
    onSave: (Lead) -> Unit
) {
    var customerName by remember { mutableStateOf(initialLead.customerName) }
    var phone by remember { mutableStateOf(initialLead.phone) }
    var email by remember { mutableStateOf(initialLead.email ?: "") }
    var address by remember { mutableStateOf(initialLead.address ?: "") }
    var city by remember { mutableStateOf(initialLead.city ?: "") }
    var state by remember { mutableStateOf(initialLead.state ?: "TX") }
    var zip by remember { mutableStateOf(initialLead.zip ?: "") }
    var monthlyBill by remember { mutableStateOf(initialLead.monthlyElectricBill?.toString() ?: "250") }
    var kwUsage by remember { mutableStateOf(initialLead.averageKWhUsage?.toString() ?: "1100") }
    var roofType by remember { mutableStateOf(initialLead.roofType ?: "Composition Shingle") }
    var sunExposure by remember { mutableStateOf(initialLead.sunExposure ?: "High") }
    var stage by remember { mutableStateOf(PipelineStage.fromString(initialLead.pipelineStage)) }
    var estimatedKw by remember { mutableStateOf(initialLead.estimatedSystemSizeKw?.toString() ?: "8.4") }
    var estimatedVal by remember { mutableStateOf(initialLead.estimatedValue?.toString() ?: "26000") }
    var notes by remember { mutableStateOf(initialLead.notes ?: "") }
    var nextFollowUp by remember { mutableStateOf(initialLead.nextFollowUp ?: "") }

    var stageDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialLead.id.isBlank()) "New Solar Lead" else "Edit Lead") },
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
                    modifier = Modifier.fillMaxWidth().testTag("lead_name_input")
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f).testTag("lead_phone_input")
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
                    label = { Text("Street Address") },
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
                    OutlinedTextField(
                        value = zip,
                        onValueChange = { zip = it },
                        label = { Text("ZIP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        label = { Text("Pipeline Stage") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stageDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = stageDropdownExpanded,
                        onDismissRequest = { stageDropdownExpanded = false }
                    ) {
                        PipelineStage.entries.forEach { entry ->
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
                        value = monthlyBill,
                        onValueChange = { monthlyBill = it },
                        label = { Text("Monthly Bill ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = kwUsage,
                        onValueChange = { kwUsage = it },
                        label = { Text("Avg kWh/mo") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = estimatedKw,
                        onValueChange = { estimatedKw = it },
                        label = { Text("Est. System (kW)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = estimatedVal,
                        onValueChange = { estimatedVal = it },
                        label = { Text("Est. Value ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = nextFollowUp,
                    onValueChange = { nextFollowUp = it },
                    label = { Text("Next Follow-up (e.g. Thu 3pm)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Lead Notes & Roof Details") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (customerName.isNotBlank() && phone.isNotBlank()) {
                        val toSave = initialLead.copy(
                            customerName = customerName.trim(),
                            phone = phone.trim(),
                            email = email.trim().ifEmpty { null },
                            address = address.trim().ifEmpty { null },
                            city = city.trim().ifEmpty { null },
                            state = state.trim().ifEmpty { null },
                            zip = zip.trim().ifEmpty { null },
                            monthlyElectricBill = monthlyBill.toDoubleOrNull(),
                            averageKWhUsage = kwUsage.toDoubleOrNull(),
                            roofType = roofType.trim().ifEmpty { null },
                            sunExposure = sunExposure.trim().ifEmpty { null },
                            pipelineStage = stage.name,
                            estimatedSystemSizeKw = estimatedKw.toDoubleOrNull(),
                            estimatedValue = estimatedVal.toDoubleOrNull(),
                            notes = notes.trim().ifEmpty { null },
                            nextFollowUp = nextFollowUp.trim().ifEmpty { null }
                        )
                        onSave(toSave)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SolarGoldDark),
                enabled = customerName.isNotBlank() && phone.isNotBlank()
            ) {
                Text("Save Lead")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
