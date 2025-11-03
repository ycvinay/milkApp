package com.example.farmermilkcollectionapp.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.farmermilkcollectionapp.data.MilkCollection // No Farmer import
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MilkScreen(viewModel: MilkViewModel = hiltViewModel()) {
    // --- 1. SIMPLIFIED: Only need to get collections ---
    val records by viewModel.collections.collectAsState(initial = emptyList())

    // State for Add/Edit Dialog
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<MilkCollection?>(null) }

    // State for Delete Confirmation Dialog
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<MilkCollection?>(null) }

    var dialogErrorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingRecord = null
                    dialogErrorMessage = null // Clear error on open
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Record", tint = Color.White)
            }
        },
        topBar = {
            CenterAlignedTopAppBar(title = { Text("🐄 Farmer Milk Records") })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (records.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No milk records yet. Tap + to add.", color = Color.Gray)
                }
            } else {
                Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxHeight()
                            // --- 2. UPDATED WIDTH: Removed Farmer column ---
                            .width(720.dp)
                            .padding(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        stickyHeader {
                            MilkTableHeader()
                        }
                        items(records) { item ->
                            MilkTableRow(
                                item = item,
                                onEdit = {
                                    editingRecord = item
                                    dialogErrorMessage = null
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

            // --- DIALOGS ---
            if (showAddEditDialog) {
                AddOrEditMilkDialog(
                    existing = editingRecord,
                    errorMessage = dialogErrorMessage,
                    onDismiss = {
                        showAddEditDialog = false
                        dialogErrorMessage = null
                    },
                    // --- 3. UPDATED VALIDATION LOGIC ---
                    onSave = { record ->
                        // Validation 1: Check required fields
                        if (record.quantityLitres == 0.0 || record.pricePerLitre == 0.0) {
                            dialogErrorMessage = "Please fill in Liters and Rate."
                            return@AddOrEditMilkDialog // Stop here
                        }

                        // Validation 2: Check for duplicates
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val recordDateStr = sdf.format(Date(record.date))

                        val isDuplicate = records.any { existingRecord ->
                            val existingRecordDateStr = sdf.format(Date(existingRecord.date))
                            val isSameDay = existingRecordDateStr == recordDateStr
                            val isSameSession = existingRecord.session == record.session
                            val isDifferentRecord = existingRecord.id != record.id

                            if (record.id == 0L) {
                                isSameDay && isSameSession
                            } else {
                                isSameDay && isSameSession && isDifferentRecord
                            }
                        }

                        if (isDuplicate) {
                            dialogErrorMessage = "A record for this ${record.session} session already exists today."
                            return@AddOrEditMilkDialog
                        }

                        // --- All checks passed, save the record ---
                        if (record.id == 0L) {
                            viewModel.addCollection(record)
                        } else {
                            viewModel.updateCollection(record)
                        }
                        showAddEditDialog = false
                        dialogErrorMessage = null
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
}


// --- DIALOG COMPOSABLES ---

@Composable
fun DeleteConfirmationDialog(
    record: MilkCollection,
    onConfirmDelete: (MilkCollection) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Confirm Deletion", color = MaterialTheme.colorScheme.error) },
        text = {
            Column {
                Text(text = "Are you sure you want to delete this milk record?")
                Spacer(modifier = Modifier.height(8.dp))
                // --- 4. SIMPLIFIED: No farmer name ---
                Text(text = "**Liters:** ${record.quantityLitres} (${record.session})", fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmDelete(record) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete", color = Color.White) }
        },
        dismissButton = { OutlinedButton(onClick = { onDismiss() }) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditMilkDialog(
    existing: MilkCollection? = null,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSave: (MilkCollection) -> Unit
) {
    // --- State for Date ---
    var selectedDate by remember { mutableStateOf(existing?.date ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // --- THIS IS THE FIX ---
    // The date validation logic moves inside rememberDatePickerState
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate,
        selectableDates = object : SelectableDates {
            // This disables all future dates
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= System.currentTimeMillis()
            }
        }
    )
    // --- END OF FIX ---

    val dateFormat = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) }

    // --- Other states ---
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
                    selectedDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            // The DatePicker composable itself no longer needs the validator
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            Button(
                onClick = {
                    val record = existing?.copy(
                        date = selectedDate,
                        quantityLitres = liters.toDoubleOrNull() ?: 0.0,
                        fatPercentage = fat.toDoubleOrNull() ?: 0.0,
                        pricePerLitre = rate.toDoubleOrNull() ?: 0.0,
                        session = session,
                        updatedAt = System.currentTimeMillis()
                    ) ?: MilkCollection(
                        id = 0L,
                        date = selectedDate,
                        session = session,
                        quantityLitres = liters.toDoubleOrNull() ?: 0.0,
                        fatPercentage = fat.toDoubleOrNull() ?: 0.0,
                        pricePerLitre = rate.toDoubleOrNull() ?: 0.0
                    )
                    onSave(record)
                }
            ) { Text(if (isEditing) "Update" else "Add") }
        },
        dismissButton = { OutlinedButton(onClick = { onDismiss() }) { Text("Cancel") } },
        title = { Text(if (isEditing) "Edit Milk Record" else "Add Milk Record") },
        text = {
            Column {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }

                // --- Date Picker Field ---
                OutlinedTextField(
                    value = dateFormat.format(Date(selectedDate)),
                    onValueChange = { /* Read Only */ },
                    label = { Text("Date") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Select Date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // --- Session Button Row ---
                Text("Session", style = MaterialTheme.typography.labelMedium)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { session = "Morning" },
                        colors = if (session == "Morning")
                            ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        else
                            ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Morning") }

                    OutlinedButton(
                        onClick = { session = "Evening" },
                        colors = if (session == "Evening")
                            ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        else
                            ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Evening") }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = liters,
                    onValueChange = { liters = it },
                    label = { Text("Liters *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it },
                    label = { Text("Rate (₹ per liter) *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it },
                    label = { Text("Fat Percentage") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    )
}

// --- TABLE COMPOSABLES ---
@Composable
fun MilkTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.dp, Color.DarkGray)
    ) {
        // --- 8. REMOVED "Farmer" COLUMN ---
        TableCell("Liters", true, width = 80.dp)
        TableCell("Rate", true, width = 80.dp)
        TableCell("Fat %", true, width = 80.dp)
        TableCell("Amount", true, width = 100.dp)
        TableCell("Date", true, width = 110.dp)
        TableCell("Session", true, width = 80.dp)
        TableCell("Status", true, width = 80.dp)
        TableCell("Actions", true, width = 110.dp) // Total = 720.dp
    }
}

@Composable
fun MilkTableRow(
    item: MilkCollection,
    onEdit: (MilkCollection) -> Unit,
    onDelete: (MilkCollection) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) }
    val formattedDate = dateFormat.format(Date(item.date))

    val statusColor = if (item.paymentStatus == "Paid") Color(0xFF008000) // Dark Green
    else Color(0xFFD32F2F) // Dark Red

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- 9. REMOVED "Farmer" CELL ---
        TableCell(item.quantityLitres.toString(), width = 80.dp)
        TableCell(item.pricePerLitre.toString(), width = 80.dp)
        TableCell(item.fatPercentage.toString(), width = 80.dp)
        TableCell(String.format("%.2f", item.totalAmount), width = 100.dp)
        TableCell(formattedDate, width = 110.dp)
        TableCell(item.session, width = 80.dp)

        TableCell(
            text = item.paymentStatus,
            width = 80.dp,
            textColor = statusColor
        )

        Box(
            modifier = Modifier
                .width(110.dp)
                .border(0.5.dp, Color.Gray)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
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

@Composable
fun TableCell(
    text: String,
    isHeader: Boolean = false,
    width: Dp,
    textColor: Color = Color.Black
) {
    Box(
        modifier = Modifier
            .width(width)
            .border(0.5.dp, Color.Gray)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
            color = if (isHeader)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                textColor,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2
        )
    }
}