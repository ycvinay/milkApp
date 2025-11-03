package com.example.farmermilkcollectionapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.farmermilkcollectionapp.data.MilkCollection
import com.example.farmermilkcollectionapp.data.MilkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar // <-- 1. IMPORT
import javax.inject.Inject

@HiltViewModel
class MilkViewModel @Inject constructor(
    private val milkRepository: MilkRepository
) : ViewModel() {

    // --- 2. HELPER FUNCTIONS TO GET TODAY'S DATE RANGE ---
    private fun getStartOfToday(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getEndOfToday(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    // --- 3. DATA FOR THE DASHBOARD ---

    // Get a flow of records just for today
    private val todayRecords = milkRepository.getCollectionsForDay(getStartOfToday(), getEndOfToday())
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Find the Morning record for today
    val morningRecord = todayRecords.map { records ->
        records.find { it.session == "Morning" }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Find the Evening record for today
    val eveningRecord = todayRecords.map { records ->
        records.find { it.session == "Evening" }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Get the latest 10 records for the list
    val latestRecords = milkRepository.getLatestRecords()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())


    // --- 4. EXISTING DATA (FOR MILKSCREEN) ---
    val collections = milkRepository.getAllCollections()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addCollection(collection: MilkCollection) {
        viewModelScope.launch {
            milkRepository.insert(collection)
        }
    }

    fun updateCollection(collection: MilkCollection) {
        viewModelScope.launch {
            milkRepository.update(collection)
        }
    }

    // --- 5. FIXED: Changed id to Long ---
    fun deleteCollection(id: Int) {
        viewModelScope.launch {
            milkRepository.delete(id)
        }
    }
}