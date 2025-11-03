package com.example.farmermilkcollectionapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.farmermilkcollectionapp.data.MilkCollection
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavHostController,
    viewModel: MilkViewModel = hiltViewModel()
) {
    // Collect the data from the ViewModel
    val morningRecord by viewModel.morningRecord.collectAsState()
    val eveningRecord by viewModel.eveningRecord.collectAsState()
    val latestRecords by viewModel.latestRecords.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("📊 Dashboard") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 1. "TODAY'S DETAILS" SECTION ---
            item {
                Text(
                    "Today's Summary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Morning Card
                    SummaryCard(
                        session = "Morning",
                        record = morningRecord,
                        modifier = Modifier.weight(1f)
                    )
                    // Evening Card
                    SummaryCard(
                        session = "Evening",
                        record = eveningRecord,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- 2. "RECENT RECORDS" SECTION ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Records",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { navController.navigate("milk_records") }) {
                        Text("See All")
                    }
                }
            }

            // --- 3. LATEST RECORDS LIST ---
            if (latestRecords.isEmpty()) {
                item {
                    Text(
                        "No recent records found.",
                        color = Color.Gray,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                items(latestRecords) { record ->
                    RecentRecordItem(record = record)
                    Divider()
                }
            }
        }
    }
}

// A simple composable for the "Today's Summary" cards
@Composable
fun SummaryCard(
    session: String,
    record: MilkCollection?,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(session, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            val liters = record?.quantityLitres ?: 0.0
            val rate = record?.pricePerLitre ?: 0.0

            Text(
                text = if (record == null) "---" else "${liters} L",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (record == null) Color.Gray else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (record == null) "No entry" else "Rate: ₹${rate}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

// A simple composable for the recent records list
@Composable
fun RecentRecordItem(record: MilkCollection) {
    val dateFormat = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${dateFormat.format(record.date)} - ${record.session}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                "${record.quantityLitres} Liters (Fat: ${record.fatPercentage}%)",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
        Text(
            "₹${String.format("%.2f", record.totalAmount)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}