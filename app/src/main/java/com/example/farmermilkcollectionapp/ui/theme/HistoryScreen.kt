package com.example.farmermilkcollectionapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.farmermilkcollectionapp.data.MilkCollection
import com.example.farmermilkcollectionapp.data.MilkViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: MilkViewModel = hiltViewModel()) {

    val allRecords by viewModel.collections.collectAsState(initial = emptyList())

    var selectedRange by remember { mutableStateOf("30") } // "7","15","30","custom"
    var customFrom by remember { mutableStateOf<Long?>(null) }
    var customTo by remember { mutableStateOf<Long?>(null) }

    var showBarChart by remember { mutableStateOf(true) }
    var metricIsLitres by remember { mutableStateOf(true) }

    val today = System.currentTimeMillis()

    // -----------------------
    // FILTER DATA BASED ON RANGE
    // -----------------------
    val filtered = remember(allRecords, selectedRange, customFrom, customTo) {
        fun daysToMillis(d: Int) = d * 24L * 60 * 60 * 1000

        when (selectedRange) {
            "7" -> allRecords.filter { (today - it.date) <= daysToMillis(7) }
            "15" -> allRecords.filter { (today - it.date) <= daysToMillis(15) }
            "30" -> allRecords.filter { (today - it.date) <= daysToMillis(30) }
            "custom" -> if (customFrom != null && customTo != null)
                allRecords.filter { it.date in customFrom!!..customTo!! }
            else emptyList()
            else -> emptyList()
        }
    }

    // -----------------------
    // GROUP BY DAY
    // -----------------------
    val perDay = remember(filtered, selectedRange, customFrom, customTo) {
        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
        val map = linkedMapOf<String, Pair<Double, Double>>()

        val dayKeys = buildDateKeysForRange(selectedRange, customFrom, customTo, filtered)
        dayKeys.forEach { map[it] = 0.0 to 0.0 }

        filtered.forEach { record ->
            val key = sdf.format(Date(record.date))
            val prev = map[key] ?: (0.0 to 0.0)
            map[key] = prev.first + record.quantityLitres to prev.second + record.totalAmount
        }

        map
    }

    val totalLitres = perDay.values.sumOf { it.first }
    val totalEarnings = perDay.values.sumOf { it.second }
    val avgFat = filtered.map { it.fatPercentage }.average().takeIf { !it.isNaN() } ?: 0.0
    val avgRate = filtered.map { it.pricePerLitre }.average().takeIf { !it.isNaN() } ?: 0.0

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("📅 History & Analytics") })
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(12.dp)
        ) {

            // -----------------------
            // RANGE SELECTOR
            // -----------------------
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RangeChip("7", selectedRange) { selectedRange = "7" }
                    RangeChip("15", selectedRange) { selectedRange = "15" }
                    RangeChip("30", selectedRange) { selectedRange = "30" }
                    RangeChip("Custom", selectedRange) { selectedRange = "custom" }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }

            // -----------------------
            // SUMMARY CARD
            // -----------------------
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Summary", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SummaryMini("Total Milk", "${"%.1f".format(totalLitres)} L")
                            SummaryMini("Total Earnings", "₹${"%.0f".format(totalEarnings)}")
                            SummaryMini("Avg Fat", "${"%.2f".format(avgFat)}%")
                            SummaryMini("Avg Rate", "₹${"%.0f".format(avgRate)}")
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // -----------------------
            // CHART TYPE & METRIC CONTROLS
            // -----------------------
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        FilterToggleButton("Bar", showBarChart) { showBarChart = true }
                        Spacer(Modifier.width(6.dp))
                        FilterToggleButton("Line", !showBarChart) { showBarChart = false }
                    }

                    Row {
                        FilterToggleButton("Litres", metricIsLitres) { metricIsLitres = true }
                        Spacer(Modifier.width(6.dp))
                        FilterToggleButton("₹ Earnings", !metricIsLitres) { metricIsLitres = false }
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }

            // -----------------------
            // CHART (WITH STICKY Y-AXIS)
            // -----------------------
            item {
                val labels = perDay.keys.toList()
                val values = if (metricIsLitres)
                    perDay.values.map { it.first }
                else perDay.values.map { it.second }

                val maxValue = (values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
                val metricLabel = if (metricIsLitres) "Litres" else "₹"

                // --- THIS IS THE FIX ---
                // We use a Row to separate the sticky Y-axis from the scrollable chart
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp, bottom = 8.dp, start = 8.dp)
                    ) {
                        // --- 1. THE STICKY Y-AXIS LABELS ---
                        // Sits outside the scrollable area
                        YAxisLabels(
                            maxValue = maxValue,
                            metricLabel = metricLabel,
                            modifier = Modifier
                                .width(30.dp) // Fixed width for labels
                                .fillMaxHeight()
                        )

                        // --- 2. THE SCROLLABLE CHART AREA ---
                        Box(
                            modifier = Modifier
                                .weight(1f) // Takes remaining space
                                .horizontalScroll(rememberScrollState())
                        ) {
                            // Calculate dynamic width for the chart data
                            val chartWidth = (labels.size * 70).dp // 70dp per bar

                            ChartDataCanvas(
                                labels = labels,
                                values = values,
                                maxValue = maxValue,
                                isBar = showBarChart,
                                metricLabel = metricLabel,
                                originalData = perDay,
                                modifier = Modifier
                                    .width(chartWidth) // The dynamic, wide width
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
                // --- END OF FIX ---
            }


            item { Spacer(Modifier.height(18.dp)) }

            // -----------------------
            // RAW DAILY BREAKDOWN
            // -----------------------
            item {
                Text("Daily Breakdown", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
            }

            items(perDay.size) { idx ->
                val day = perDay.keys.elementAt(idx)
                val (lit, earn) = perDay.values.elementAt(idx)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(day, fontWeight = FontWeight.Bold)
                            Text("Litres: ${"%.1f".format(lit)}")
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("₹${"%.0f".format(earn)}", fontWeight = FontWeight.Bold)
                            Text("Avg Rate: ${if (lit > 0) "₹${"%.0f".format(earn / lit)}" else "-"}")
                        }
                    }
                }
            }
        }
    }
}

/* --------------------------
   Helper UI Components
   (Unchanged)
   -------------------------- */

@Composable
private fun RangeChip(text: String, selectedRange: String, onClick: () -> Unit) {
    val isSelected = (selectedRange == text || (text == "Custom" && selectedRange == "custom"))
    OutlinedButton(
        onClick = onClick,
        colors = if (isSelected) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else ButtonDefaults.outlinedButtonColors()
    ) {
        Text(text)
    }
}

@Composable
private fun FilterToggleButton(text: String, active: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = if (active) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) else ButtonDefaults.buttonColors()
    ) {
        Text(text, color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SummaryMini(title: String, value: String) {
    Column(modifier = Modifier.padding(end = 8.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

/* --------------------------
   NEW: Split Chart Canvas
   -------------------------- */

/**
 * Draws the Y-Axis labels and the horizontal grid lines.
 * This composable is intended to be *fixed* (not scrollable).
 */
@Composable
private fun YAxisLabels(
    maxValue: Double,
    metricLabel: String,
    modifier: Modifier = Modifier
) {
    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 28f
            textAlign = android.graphics.Paint.Align.RIGHT
            isAntiAlias = true
        }
    }

    Canvas(modifier = modifier) {
        val chartTop = 12f // Small top padding
        val chartBottom = size.height - 40f // Padding for X-axis labels (which are in the other composable)
        val chartHeight = chartBottom - chartTop
        val chartRight = size.width // Right edge of this composable

        val steps = 5
        for (i in 0..steps) {
            val y = chartTop + chartHeight * (i.toFloat() / steps)

            // Draw horizontal grid line
            // We draw it across the *entire* screen, assuming the data canvas is underneath
            drawLine(
                color = Color.LightGray,
                start = Offset(chartRight, y), // Start from the right edge
                end = Offset(9999f, y), // Draw line way off-screen
                strokeWidth = 1f
            )

            // Draw Y-axis value label
            val labelValue = maxValue * (1f - i.toFloat() / steps)
            drawContext.canvas.nativeCanvas.drawText(
                String.format("%.0f", labelValue),
                chartRight - 10f,  // offset from the right edge
                y + 8f,
                textPaint
            )
        }

        // Draw the metric label (e.g., "Litres" or "₹")
        drawContext.canvas.nativeCanvas.drawText(
            metricLabel,
            chartRight - 10f,
            chartTop + 8f, // Place it near the top
            textPaint.apply { textSize = 22f }
        )
    }
}

/**
 * Draws the bars/lines and the X-axis labels.
 * This composable is intended to be *scrollable* horizontally.
 */
@Composable
private fun ChartDataCanvas(
    labels: List<String>,
    values: List<Double>,
    maxValue: Double,
    isBar: Boolean,
    metricLabel: String,
    modifier: Modifier = Modifier,
    originalData: Map<String, Pair<Double, Double>> // <-- REQUIRED FIX
) {

    if (labels.isEmpty() || values.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No data available", color = Color.Gray)
        }
        return
    }
    var tappedIndex by remember { mutableStateOf<Int?>(null) }

    val labelPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 26f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    Box(modifier = modifier) {

        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val w = size.width
                        val gap = w / labels.size

                        val index = (offset.x / gap).toInt()
                        if (index in labels.indices) {
                            tappedIndex = index
                        }
                    }
                }
        ) {

            val w = size.width
            val h = size.height

            val chartLeft = 0f
            val chartTop = 12f
            val chartBottom = h - 40f
            val chartHeight = chartBottom - chartTop
            val chartWidth = w - chartLeft

            val n = values.size
            val gap = chartWidth / n
            val barWidth = (gap * 0.6f).coerceAtMost(55f)

            val points = mutableListOf<Offset>()

            for (i in values.indices) {
                val xCenter = chartLeft + gap * i + gap / 2f
                val value = values[i]
                val normalized = (value / maxValue).coerceIn(0.0, 1.0)
                val barTop = chartTop + chartHeight * (1 - normalized)

                // BAR
                if (isBar) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF4CAF50), Color(0xFF81C784))
                        ),
                        topLeft = Offset(xCenter - barWidth / 2f, barTop.toFloat()),
                        size = Size(barWidth, (chartBottom - barTop).toFloat()),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }

                points.add(Offset(xCenter, barTop.toFloat()))

                // X-Axis Label
                val x = xCenter
                val y = chartBottom + 32f
                drawContext.canvas.nativeCanvas.apply {
                    save()
                    rotate(-35f, x, y)
                    drawText(labels[i], x, y, labelPaint)
                    restore()
                }
            }

            // LINE
            if (!isBar && points.size >= 2) {
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = Color(0xFF1976D2),
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
                points.forEach {
                    drawCircle(Color(0xFF1976D2), radius = 5f, center = it)
                }
            }
        }

        // top-right metric label
        Text(
            text = metricLabel,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp),
            style = MaterialTheme.typography.labelSmall
        )

        /* ================================
           POPUP (WITH CORRECT AVG RATE)
         ================================ */
        tappedIndex?.let { idx ->
            val label = labels[idx]
            val (litres, earnings) = originalData[label] ?: (0.0 to 0.0)

            AlertDialog(
                onDismissRequest = { tappedIndex = null },
                confirmButton = {
                    TextButton(onClick = { tappedIndex = null }) { Text("OK") }
                },
                title = {
                    Text("📊 $label", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {

                        if (metricLabel == "Litres") {
                            Text("Litres: ${"%.2f".format(litres)} L")
                            Text("Earnings: ₹${"%.2f".format(earnings)}")
                            if (litres > 0)
                                Text("Avg Rate: ₹${"%.2f".format(earnings / litres)}")
                        }

                        if (metricLabel == "₹") {
                            Text("Earnings: ₹${"%.2f".format(earnings)}")
                            Text("Litres: ${"%.2f".format(litres)} L")
                            if (litres > 0)
                                Text("Avg Rate: ₹${"%.2f".format(earnings / litres)}")
                        }
                    }
                }
            )
        }
    }
}


/* ---------------------------
   Utility: build ordered date keys for range
   (Unchanged)
   --------------------------- */
private fun buildDateKeysForRange(range: String, customFrom: Long?, customTo: Long?, filtered: List<MilkCollection>): List<String> {
    val sdfKey = SimpleDateFormat("dd MMM", Locale.getDefault())
    val today = Calendar.getInstance()

    val days = when (range) {
        "7" -> 7
        "15" -> 15
        "30" -> 30
        "custom" -> {
            if (customFrom != null && customTo != null) {
                val diff = ((customTo - customFrom) / (24L * 60 * 60 * 1000)).toInt()
                (diff + 1).coerceAtLeast(1)
            } else {
                7
            }
        }
        else -> 7
    }

    val cal = Calendar.getInstance()
    cal.time = Date()
    val keys = mutableListOf<String>()

    if (range == "custom" && customFrom != null && customTo != null) {
        val fromCal = Calendar.getInstance().apply { timeInMillis = customFrom!! }
        val toCal = Calendar.getInstance().apply { timeInMillis = customTo!! }
        val list = mutableListOf<String>()
        val tmp = fromCal.clone() as Calendar
        while (!tmp.after(toCal)) {
            list.add(sdfKey.format(tmp.time))
            tmp.add(Calendar.DATE, 1)
        }
        return list
    }

    val tmp = cal.clone() as Calendar
    tmp.add(Calendar.DATE, - (days - 1))
    for (i in 0 until days) {
        keys.add(sdfKey.format(tmp.time))
        tmp.add(Calendar.DATE, 1)
    }
    return keys
}