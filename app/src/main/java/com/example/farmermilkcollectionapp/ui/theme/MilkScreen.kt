package com.example.farmermilkcollectionapp.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.farmermilkcollectionapp.data.MilkCollection
import com.example.farmermilkcollectionapp.data.MilkViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MilkScreen(viewModel: MilkViewModel = hiltViewModel()) {

    // --- 1. Get Data ---
    val records by viewModel.collections.collectAsState(initial = emptyList())
    val morningRecord by viewModel.morningRecord.collectAsState()
    val eveningRecord by viewModel.eveningRecord.collectAsState()

    // --- 2. Local State ---
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<MilkCollection?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<MilkCollection?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // --- 3. FILTER LOGIC (Last 3 Days vs All) ---
    var showAllRecords by remember { mutableStateOf(false) }

    // Calculate the cutoff time (3 days ago)
    val threeDaysAgo = remember {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -3)
        calendar.timeInMillis
    }

    // Determine which records to show
    val visibleRecords = remember(records, showAllRecords) {
        if (showAllRecords) {
            records // Show everything
        } else {
            // Show only records where date > 3 days ago
            records.filter { it.date >= threeDaysAgo }
        }
    }

    Scaffold(
        // --- 1. ADDED: A subtle background color for the whole screen ---
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),

        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingRecord = null
                    errorMessage = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Record", tint = Color.White)
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("🐄 Farmer Milk Records") },
                // --- 2. ADDED: Custom colors for the Top App Bar ---
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface, // Lifts it off the background
                    titleContentColor = MaterialTheme.colorScheme.primary // Makes title stand out
                )
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {

            /* =====================================================
                   🔥 SECTION 1: LIVE CLOCK + SUMMARY
            ===================================================== */
            item {
                LiveClockHeader() // This composable is also updated below
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TodaySummaryCard(
                        title = "Morning",
                        icon = Icons.Default.WbSunny,
                        record = morningRecord,
                        modifier = Modifier.weight(1f)
                    )
                    TodaySummaryCard(
                        title = "Evening",
                        icon = Icons.Default.NightsStay,
                        record = eveningRecord,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }

            /* =====================================================
                   🔥 SECTION 2: TABLE HEADER & TOGGLE
            ===================================================== */
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showAllRecords) "All Records" else "Last 3 Days",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    // The "Show More / Show Less" Button
                    TextButton(onClick = { showAllRecords = !showAllRecords }) {
                        Text(if (showAllRecords) "Show Less" else "View All")
                    }
                }
            }

            /* =====================================================
                   🔥 SECTION 3: THE DATA TABLE
            ===================================================== */

            if (visibleRecords.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (records.isEmpty()) {
                            Text("No records yet. Tap + to add.", color = Color.Gray)
                        } else {
                            Text("No records in the last 3 days.", color = Color.Gray)
                        }
                    }
                }
            } else {
                item {
                    Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        Column(
                            modifier = Modifier
                                .width(640.dp) // Fixed width for scrolling
                                .padding(horizontal = 16.dp)
                        ) {
                            MilkTableHeader()

                            visibleRecords.forEachIndexed { index, item ->
                                MilkTableRow(
                                    item = item,
                                    // Alternating row colors
                                    backgroundColor = if (index % 2 == 0) MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    onEdit = {
                                        editingRecord = item
                                        errorMessage = null
                                        showAddEditDialog = true
                                    },
                                    onDelete = {
                                        recordToDelete = it
                                        showDeleteConfirmDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        } // End LazyColumn

        /* =====================================================
            DIALOGS (NO CHANGES)
        ===================================================== */
        // (All dialog logic is unchanged)

        if (showAddEditDialog) {
            AddOrEditMilkDialog(
                existing = editingRecord,
                errorMessage = errorMessage,
                onDismiss = {
                    showAddEditDialog = false
                    errorMessage = null
                },
                onSave = { newRecord ->
                    if (newRecord.quantityLitres == 0.0 || newRecord.pricePerLitre == 0.0) {
                        errorMessage = "Liters and Rate are required."
                        return@AddOrEditMilkDialog
                    }
                    val sdf2 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val newDate = sdf2.format(Date(newRecord.date))
                    val isDuplicate = records.any { r ->
                        val rDate = sdf2.format(Date(r.date))
                        r.session == newRecord.session && rDate == newDate && r.id != newRecord.id
                    }

                    if (isDuplicate) {
                        errorMessage = "A record for this session already exists today."
                        return@AddOrEditMilkDialog
                    }
                    if (newRecord.id == 0L) {
                        viewModel.addCollection(newRecord)
                    } else {
                        viewModel.updateCollection(newRecord)
                    }
                    showAddEditDialog = false
                    errorMessage = null
                }
            )
        }

        if (showDeleteConfirmDialog && recordToDelete != null) {
            DeleteConfirmationDialog(
                record = recordToDelete!!,
                onConfirmDelete = {
                    viewModel.deleteCollection(it.id.toInt())
                    showDeleteConfirmDialog = false
                    recordToDelete = null
                },
                onDismiss = {
                    showDeleteConfirmDialog = false
                    recordToDelete = null
                }
            )
        }
    }
}
/* ================================================================
      LIVE CLOCK HEADER
================================================================ */
@Composable
fun LiveClockHeader() {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormatter = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTime = timeFormatter.format(now)
            currentDate = dateFormatter.format(now)
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            // --- 3. ADDED: A background to lift this section ---
            .background(
                MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            )
            .padding(vertical = 16.dp), // Internal padding
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = currentTime,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            // --- 4. ADDED: A custom color for the time ---
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = currentDate,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
/* ================================================================
      TODAY'S SUMMARY CARD
================================================================ */
@Composable
fun TodaySummaryCard(
    title: String,
    icon: ImageVector,
    record: MilkCollection?,
    modifier: Modifier = Modifier
) {
    val litres = record?.quantityLitres ?: 0.0
    val price = record?.pricePerLitre ?: 0.0
    val amount = record?.totalAmount ?: 0.0

    OutlinedCard(
        modifier = modifier.height(130.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(icon, contentDescription = title, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))

            Text(
                text = if (record == null) "---" else "${String.format("%.1f", litres)} L",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (record == null) Color.Gray else MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (record == null) "No Entry" else "₹ ${String.format("%.0f", price)}",
                style = MaterialTheme.typography.titleSmall,
                color = Color.Gray
            )
            Text(
                text = if (record == null) "No Entry" else "₹ ${String.format("%.0f", amount)}",
                style = MaterialTheme.typography.titleSmall,
                color = Color.Gray
            )
        }
    }
}

/* ================================================================
      TABLE COMPONENTS
================================================================ */

@Composable
fun MilkTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.dp, Color.DarkGray)
    ) {
        TableCell("Date", true, 110.dp)
        TableCell("Session", true, 80.dp)
        TableCell("Litres", true, 80.dp)
        TableCell("Rate", true, 80.dp)
        TableCell("Fat %", true, 80.dp)
        TableCell("Amount", true, 100.dp)
        TableCell("Actions", true, 110.dp)
    }
}

@Composable
fun MilkTableRow(
    item: MilkCollection,
    backgroundColor: Color,
    onEdit: (MilkCollection) -> Unit,
    onDelete: (MilkCollection) -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TableCell(sdf.format(Date(item.date)), width = 110.dp)
        TableCell(item.session, width = 80.dp)
        TableCell(item.quantityLitres.toString(), width = 80.dp)
        TableCell(item.pricePerLitre.toString(), width = 80.dp)
        TableCell(item.fatPercentage.toString(), width = 80.dp)
        TableCell(String.format("%.2f", item.totalAmount), width = 100.dp)

        Box(
            modifier = Modifier
                .width(110.dp)
                .border(0.5.dp, Color.Gray)
                .padding(vertical = 6.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { onEdit(item) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF4CAF50))
                }
                IconButton(onClick = { onDelete(item) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusCell(status: String, width: Dp) {
    val isPaid = status == "Paid"
    val bgColor = if (isPaid) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val textColor = if (isPaid) Color(0xFF1B5E20) else Color(0xFFC62828)

    Box(
        modifier = Modifier
            .width(width)
            .border(0.5.dp, Color.Gray)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Using a simple Box with background instead of Chip for cleaner table look
        Box(
            modifier = Modifier
                .background(bgColor, shape = MaterialTheme.shapes.small)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
fun TableCell(
    text: String,
    isHeader: Boolean = false,
    width: Dp
) {
    val textColor = if (isHeader) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .width(width)
            .border(0.5.dp, Color.Gray)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            textAlign = TextAlign.Center
        )
    }
}

/* ================================================================
      DIALOGS
================================================================ */

@Composable
fun DeleteConfirmationDialog(
    record: MilkCollection,
    onConfirmDelete: (MilkCollection) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Confirm Delete", color = MaterialTheme.colorScheme.error) },
        text = { Text("Delete ${record.session} record?") },
        confirmButton = {
            Button(
                onClick = { onConfirmDelete(record) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete", color = Color.White) }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditMilkDialog(
    existing: MilkCollection?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (MilkCollection) -> Unit
) {
    var selectedDate by remember { mutableStateOf(existing?.date ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Date picker state with validator for future dates
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= System.currentTimeMillis()
            }
        }
    )
    val sdf = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) }

    var liters by remember { mutableStateOf(existing?.quantityLitres?.toString() ?: "") }
    var rate by remember { mutableStateOf(existing?.pricePerLitre?.toString() ?: "") }
    var fat by remember { mutableStateOf(existing?.fatPercentage?.toString() ?: "") }
    var session by remember { mutableStateOf(existing?.session ?: "Morning") }
    val isEditing = existing != null

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis ?: selectedDate
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(if (isEditing) "Edit Milk Record" else "Add Milk Record") },
        confirmButton = {
            Button(onClick = {
                val data = existing?.copy(
                    date = selectedDate,
                    session = session,
                    quantityLitres = liters.toDoubleOrNull() ?: 0.0,
                    fatPercentage = fat.toDoubleOrNull() ?: 0.0,
                    pricePerLitre = rate.toDoubleOrNull() ?: 0.0,
                    updatedAt = System.currentTimeMillis()
                ) ?: MilkCollection(
                    id = 0,
                    date = selectedDate,
                    session = session,
                    quantityLitres = liters.toDoubleOrNull() ?: 0.0,
                    fatPercentage = fat.toDoubleOrNull() ?: 0.0,
                    pricePerLitre = rate.toDoubleOrNull() ?: 0.0
                )
                onSave(data)
            }) { Text(if (isEditing) "Update" else "Add") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                OutlinedTextField(
                    value = sdf.format(Date(selectedDate)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Session Row (Buttons instead of dropdown)
                Column {
                    Text("Session", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { session = "Morning" },
                            modifier = Modifier.weight(1f),
                            colors = if (session == "Morning")
                                ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            else ButtonDefaults.outlinedButtonColors()
                        ) { Text("Morning") }

                        OutlinedButton(
                            onClick = { session = "Evening" },
                            modifier = Modifier.weight(1f),
                            colors = if (session == "Evening")
                                ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            else ButtonDefaults.outlinedButtonColors()
                        ) { Text("Evening") }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = liters,
                        onValueChange = { liters = it },
                        label = { Text("Liters *") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = rate,
                        onValueChange = { rate = it },
                        label = { Text("Rate *") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it },
                    label = { Text("Fat %") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    )
}