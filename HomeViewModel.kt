package com.example.myapplication.ui

import androidx.lifecycle.*
import com.example.myapplication.data.Chapter
import com.example.myapplication.data.StudyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: StudyRepository) : ViewModel() {

    val chapters = repository.getAllChapters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createChapter(name: String, subject: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.createChapter(name, subject)
            onCreated(id)
        }
    }

    fun deleteChapter(chapter: Chapter) {
        viewModelScope.launch { repository.deleteChapter(chapter) }
    }

    class Factory(private val repository: StudyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            HomeViewModel(repository) as T
    }
}
