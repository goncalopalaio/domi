package com.tri10.domi

import StateViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ViewModelFactory(): ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            StateViewModel::class.java -> StateViewModel() as T
            else -> throw IllegalArgumentException("Not implemented | modelClass:$modelClass")
        }
    }
}
