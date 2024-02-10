package com.example.smartgymapp.ui.trainee.main

import androidx.lifecycle.ViewModel
import com.example.smartgymapp.util.CommonActivity.NetworkResult
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        firestore.collection(QUOTES_COLLECTION).document(QUOTES_DOCUMENT).get()
            .addOnSuccessListener { document ->
                val quotes = document.toObject(QuoteList::class.java)?.quotesList
                if (!quotes.isNullOrEmpty()) {
                    _quotes.value = NetworkResult.Success(quotes)
                } else {
                    _quotes.value = NetworkResult.Error("No quotes found")
                }
            }.addOnFailureListener { e ->
                _quotes.value = NetworkResult.Error(e.message)
            }
    }

    companion object {
        const val QUOTES_COLLECTION = "Quotes"
        const val QUOTES_DOCUMENT = "quotesList"
    }
}

data class QuoteList(val quotesList: List<String>){
    constructor() : this(emptyList())
}
