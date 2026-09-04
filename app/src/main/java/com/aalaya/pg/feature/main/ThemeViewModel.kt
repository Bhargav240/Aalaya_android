package com.aalaya.pg.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aalaya.pg.core.data.ThemeMode
import com.aalaya.pg.core.data.ThemeStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val store: ThemeStore,
) : ViewModel() {
    val mode = store.mode

    fun setMode(mode: ThemeMode) {
        viewModelScope.launch { store.setMode(mode) }
    }
}
