package com.shamsalmaarif.reader.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shamsalmaarif.reader.data.database.entities.ReadEntity
import com.shamsalmaarif.reader.data.repository.ReadsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repo: ReadsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived

    @OptIn(ExperimentalCoroutinesApi::class)
    val reads: StateFlow<List<ReadEntity>> = combine(_searchQuery, _showArchived) { q, archived ->
        Pair(q, archived)
    }.flatMapLatest { (q, archived) ->
        when {
            archived -> repo.getArchivedReads()
            q.isNotBlank() -> repo.searchReads(q)
            else -> repo.getAllReads()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearch(q: String) { _searchQuery.value = q }

    fun toggleArchived() { _showArchived.value = !_showArchived.value }

    fun archive(id: String) = viewModelScope.launch { repo.setArchived(id, true) }

    fun unarchive(id: String) = viewModelScope.launch { repo.setArchived(id, false) }

    fun delete(id: String) = viewModelScope.launch { repo.deleteRead(id) }
}
