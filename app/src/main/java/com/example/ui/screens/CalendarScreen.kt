package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.ClipboardUiState
import com.example.ui.MainViewModel
import com.example.ui.NavigationTab
import com.example.ui.components.ClipboardItemCard
import com.example.ui.theme.HappyOrange
import com.example.ui.theme.HappyTeal300
import com.example.ui.theme.HappyTeal600
import com.example.ui.theme.HappyTeal900
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    uiState: ClipboardUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Calendar state: Current selected viewing month
    var currentMonthCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        })
    }

    // Selected day timestamp (defaults to today midnight)
    var selectedDayEpoch by remember {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        mutableLongStateOf(today.timeInMillis)
    }

    // Calculate item counts per day for the currently selected month
    val itemsPerDay = remember(uiState.items, currentMonthCalendar) {
        val year = currentMonthCalendar.get(Calendar.YEAR)
        val month = currentMonthCalendar.get(Calendar.MONTH)
        val counts = mutableMapOf<Int, Int>()

        val cal = Calendar.getInstance()
        for (item in uiState.items) {
            cal.timeInMillis = item.lastCopiedAt
            if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month) {
                val day = cal.get(Calendar.DAY_OF_MONTH)
                counts[day] = (counts[day] ?: 0) + 1
            }
        }
        counts
    }

    // Items captured on selected day
    val itemsForSelectedDay = remember(uiState.items, selectedDayEpoch) {
        val start = selectedDayEpoch
        val end = start + (24 * 60 * 60 * 1000L) - 1
        uiState.items.filter { it.lastCopiedAt in start..end }
    }

    val monthFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dayHeaderFormatter = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Month Calendar Card
        val calCardShape = RoundedCornerShape(20.dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(calCardShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, calCardShape),
            shape = calCardShape,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Primary Month Navigation in Dark Teal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = monthFormatter.format(currentMonthCalendar.time),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        ) {
                            IconButton(onClick = {
                                val prev = (currentMonthCalendar.clone() as Calendar).apply {
                                    add(Calendar.MONTH, -1)
                                }
                                currentMonthCalendar = prev
                            }, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous Month",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        ) {
                            IconButton(onClick = {
                                val next = (currentMonthCalendar.clone() as Calendar).apply {
                                    add(Calendar.MONTH, 1)
                                }
                                currentMonthCalendar = next
                            }, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next Month",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Days of week header
                val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (dow in daysOfWeek) {
                        Text(
                            text = dow,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Calendar Grid
                val firstDayOfWeek = currentMonthCalendar.get(Calendar.DAY_OF_WEEK) - 1 // 0-based
                val daysInMonth = currentMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7

                val todayCal = Calendar.getInstance()
                val isCurrentYearMonth = todayCal.get(Calendar.YEAR) == currentMonthCalendar.get(Calendar.YEAR) &&
                        todayCal.get(Calendar.MONTH) == currentMonthCalendar.get(Calendar.MONTH)
                val todayDay = if (isCurrentYearMonth) todayCal.get(Calendar.DAY_OF_MONTH) else -1

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (row in 0 until (totalCells / 7)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (col in 0 until 7) {
                                val cellIndex = row * 7 + col
                                val dayNumber = cellIndex - firstDayOfWeek + 1

                                if (dayNumber in 1..daysInMonth) {
                                    val count = itemsPerDay[dayNumber] ?: 0

                                    // Check if this day is selected
                                    val cellCal = (currentMonthCalendar.clone() as Calendar).apply {
                                        set(Calendar.DAY_OF_MONTH, dayNumber)
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    val isSelected = cellCal.timeInMillis == selectedDayEpoch
                                    val isToday = dayNumber == todayDay

                                    // Selected date using #FFA62B (HappyOrange)
                                    // Days containing items have subtle teal indicators (#489FB5)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                when {
                                                    isSelected -> HappyOrange
                                                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                    count > 0 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .border(
                                                width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                                                color = if (isToday && !isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                selectedDayEpoch = cellCal.timeInMillis
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "$dayNumber",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected || isToday || count > 0) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color(0xFF261200) else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (count > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) Color(0xFF261200) else MaterialTheme.colorScheme.primary)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // When a date is selected, display: "September 21, 2026", then clipboard timeline for that date
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = dayHeaderFormatter.format(Date(selectedDayEpoch)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${itemsForSelectedDay.size} clip${if (itemsForSelectedDay.size == 1) "" else "s"} on this date",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (itemsForSelectedDay.isNotEmpty()) {
                TextButton(onClick = {
                    viewModel.setSelectedDate(selectedDayEpoch)
                    viewModel.setTab(NavigationTab.HISTORY)
                }) {
                    Text("View in History", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // List of items for that day
        if (itemsForSelectedDay.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No clipboard items on this date",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(itemsForSelectedDay, key = { it.id }) { item ->
                    ClipboardItemCard(
                        item = item,
                        onCopy = { viewModel.copyItemToClipboard(context, item) },
                        onToggleFavorite = { viewModel.toggleFavorite(item.id) },
                        onOpenDetails = { viewModel.showDetails(item) },
                        onEdit = { viewModel.showEdit(item) },
                        onAddToCollection = { viewModel.showCollectionPicker(item) },
                        onDelete = { viewModel.deleteItem(item.id) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }
}
