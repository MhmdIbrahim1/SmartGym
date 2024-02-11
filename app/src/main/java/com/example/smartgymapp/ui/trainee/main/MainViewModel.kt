package com.example.smartgymapp.ui.trainee.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartgymapp.util.CommonActivity.NetworkResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel
@Inject constructor(
    private val firestore: FirebaseFirestore
): ViewModel() {

    private val _quotes = MutableStateFlow<NetworkResult<List<String>>>(NetworkResult.UnSpecified())
    val quotes: StateFlow<NetworkResult<List<String>>> = _quotes

    init {
        getQuotes()
    }

    private fun getQuotes() {
        _quotes.value = NetworkResult.Loading()
        viewModelScope.launch {
            try {
                val document = withContext(Dispatchers.IO) {
                    firestore.collection(QUOTES_COLLECTION).document(QUOTES_DOCUMENT).get().await()
                }
                val quotes = document.toObject(QuoteList::class.java)?.quotesList
                if (!quotes.isNullOrEmpty()) {
                    _quotes.value = NetworkResult.Success(quotes)
                } else {
                    _quotes.value = NetworkResult.Error("No quotes found")
                }
            } catch (e: Exception) {
                _quotes.value = NetworkResult.Error(e.message)
            }
        }
    }

    companion object {
        const val QUOTES_COLLECTION = "Quotes"
        const val QUOTES_DOCUMENT = "quotesList"
    }
}
