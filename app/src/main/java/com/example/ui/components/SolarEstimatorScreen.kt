package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Lead
import com.example.data.model.PipelineStage
import com.example.data.model.Project
import com.example.data.model.ProjectStage
import com.example.ui.theme.SolarCyan
import com.example.ui.theme.SolarEmerald
import com.example.ui.theme.SolarGold
import com.example.ui.theme.SolarGoldDark
import com.example.ui.theme.SolarNavy
import com.example.ui.viewmodel.SolarCrmViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun SolarEstimatorScreen(
    viewModel: SolarCrmViewModel,
    modifier: Modifier = Modifier
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 } }

    var monthlyBill by remember { mutableFloatStateOf(260f) }
    var utilityRate by remember { mutableFloatStateOf(0.24f) }
    var peakSunHours by remember { mutableFloatStateOf(5.0f) }

    // Derived Solar Sizing Calculations
    val monthlyKwhNeeded by remember {
        derivedStateOf { monthlyBill / utilityRate }
    }

    val recommendedKw by remember {
        derivedStateOf {
            // (Monthly kWh / 30 days / peak hours) * 1.15 (for 115% clean offset & inverter clipping)
            val kw = (monthlyKwhNeeded / 30.0 / peakSunHours) * 1.15
            (kw * 10).roundToInt() / 10.0
        }
    }

    val panelCount by remember {
        derivedStateOf {
            ceil(recommendedKw * 1000 / 400).toInt().coerceAtLeast(4)
        }
    }

    val annualProductionKwh by remember {
        derivedStateOf {
            (recommendedKw * peakSunHours * 365 * 0.82).roundToInt()
        }
    }

    val grossSolarPrice by remember {
        derivedStateOf {
            // Residential average ~$2.90 per watt
            (recommendedKw * 1000 * 2.90).roundToInt().toDouble()
        }
    }

    val federalTaxCredit30 by remember {
        derivedStateOf {
            grossSolarPrice * 0.30
        }
    }

    val netSolarCost by remember {
        derivedStateOf {
            grossSolarPrice - federalTaxCredit30
        }
    }

    val estimated25YearSavings by remember {
        derivedStateOf {
            // 25-yr baseline utility payments with 3.5% historical escalation
            var utilityCumulative = 0.0
            var currentAnnualBill = monthlyBill * 12.0
            for (i in 1..25) {
                utilityCumulative += currentAnnualBill
                currentAnnualBill *= 1.035
            }
            (utilityCumulative - netSolarCost).coerceAtLeast(0.0)
        }
    }

    val paybackPeriodYears by remember {
        derivedStateOf {
            val annualElectricityValue = monthlyBill * 12
            val years = netSolarCost / annualElectricityValue
            (years * 10).roundToInt() / 10.0
        }
    }

    var showCreateLeadDialog by remember { mutableStateOf(false) }
    var showCreateProjectDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = SolarNavy
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SolarGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = SolarGold,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Field Solar Estimator",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Calculate residential PV sizing, battery requirements & ROI",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Homeowner Input Sliders Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Homeowner Energy Inputs",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Monthly Electric Bill Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Average Monthly Electric Bill",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${monthlyBill.toInt()}/mo",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = SolarGoldDark
                    )
                }
                Slider(
                    value = monthlyBill,
                    onValueChange = { monthlyBill = it },
                    valueRange = 60f..800f,
                    steps = 36,
                    colors = SliderDefaults.colors(
                        thumbColor = SolarGoldDark,
                        activeTrackColor = SolarGoldDark
                    ),
                    modifier = Modifier.testTag("bill_slider")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Utility Rate ($/kWh)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Utility Rate ($/kWh)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${(utilityRate * 100).roundToInt() / 100.0} / kWh",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = SolarNavy
                    )
                }
                Slider(
                    value = utilityRate,
                    onValueChange = { utilityRate = it },
                    valueRange = 0.12f..0.50f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = SolarNavy,
                        activeTrackColor = SolarNavy
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Peak Sun Hours (h/day)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Peak Sun Hours / Day (Solar Irradiance)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(peakSunHours * 10).roundToInt() / 10.0} hrs/day",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = SolarEmerald
                    )
                }
                Slider(
                    value = peakSunHours,
                    onValueChange = { peakSunHours = it },
                    valueRange = 3.5f..6.5f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = SolarEmerald,
                        activeTrackColor = SolarEmerald
                    )
                )
            }
        }

        // Calculated System Results
        Text(
            text = "Recommended Solar System",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CalculationCard(
                title = "System Size",
                value = "$recommendedKw kW",
                subtitle = "$panelCount panels (400W)",
                icon = Icons.Default.SolarPower,
                color = SolarGoldDark,
                modifier = Modifier.weight(1f)
            )

            CalculationCard(
                title = "Annual Output",
                value = "$annualProductionKwh kWh",
                subtitle = "115% Clean Offset",
                icon = Icons.Default.WbSunny,
                color = SolarNavy,
                modifier = Modifier.weight(1f)
            )
        }

        // Financial & ROI Breakdown Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Financial & Tax Credit Analysis",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                FinancialRow(label = "Estimated Gross System Price", value = currencyFormat.format(grossSolarPrice))
                FinancialRow(
                    label = "Federal Solar Tax Credit (30% ITC)",
                    value = "-${currencyFormat.format(federalTaxCredit30)}",
                    isHighlight = true,
                    highlightColor = SolarEmerald
                )
                FinancialRow(label = "Net Solar Investment", value = currencyFormat.format(netSolarCost), isBold = true)

                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(modifier = Modifier.height(10.dp))

                FinancialRow(label = "Estimated 25-Year Utility Savings", value = currencyFormat.format(estimated25YearSavings), isHighlight = true, highlightColor = SolarGoldDark)
                FinancialRow(label = "Estimated Payback Period", value = "$paybackPeriodYears years")
            }
        }

        // Battery Storage Recommendation Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SolarEmerald.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        tint = SolarEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (monthlyBill >= 250f) "Recommended: 13.5 kWh Battery Backup" else "Optional: 5 - 10 kWh Storage",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (monthlyBill >= 250f) "Tesla Powerwall 3 / Enphase IQ 5P for TOU rate arbitrage & storm backup" else "Grid-tied solar without battery meets daytime household loads",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Action Buttons: Save to CRM
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { showCreateLeadDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save as Lead")
            }

            Button(
                onClick = { showCreateProjectDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SolarGoldDark)
            ) {
                Text("Create Project")
            }
        }
    }

    if (showCreateLeadDialog) {
        LeadFormDialog(
            initialLead = Lead(
                monthlyElectricBill = monthlyBill.toDouble(),
                averageKWhUsage = monthlyKwhNeeded.toDouble(),
                estimatedSystemSizeKw = recommendedKw,
                estimatedValue = grossSolarPrice,
                notes = "Calculated in Field Sizer: $recommendedKw kW ($panelCount panels, 30% ITC = ${currencyFormat.format(federalTaxCredit30)})"
            ),
            onDismiss = { showCreateLeadDialog = false },
            onSave = { lead ->
                viewModel.saveLead(lead)
                showCreateLeadDialog = false
            }
        )
    }

    if (showCreateProjectDialog) {
        ProjectFormDialog(
            initialProject = Project(
                systemSizeKw = recommendedKw,
                panelCount = panelCount,
                panelBrand = "400W High-Efficiency Monocrystalline",
                inverterType = "Enphase IQ8+ Microinverters",
                hasBattery = monthlyBill >= 250f,
                batteryModel = if (monthlyBill >= 250f) "Tesla Powerwall 3 (13.5 kWh)" else "None",
                totalContractPrice = grossSolarPrice,
                depositPaid = (grossSolarPrice * 0.1).roundToInt().toDouble(),
                notes = "Generated from Field Sizer: $recommendedKw kW, est. annual generation $annualProductionKwh kWh"
            ),
            onDismiss = { showCreateProjectDialog = false },
            onSave = { project ->
                viewModel.saveProject(project)
                showCreateProjectDialog = false
            }
        )
    }
}

@Composable
private fun CalculationCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Composable
private fun FinancialRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    isHighlight: Boolean = false,
    highlightColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isBold) {
                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal
                )
            },
            color = if (isHighlight) highlightColor else MaterialTheme.colorScheme.onSurface
        )
    }
}
