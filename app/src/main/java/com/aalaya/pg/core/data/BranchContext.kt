package com.aalaya.pg.core.data

import com.aalaya.pg.core.network.dto.BranchDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** The branch the user is currently working in. */
data class BranchSelection(val id: String, val name: String)

/**
 * App-wide "which branch am I working in?" context.
 *
 * Occupancy, Payments and the dashboard used to each own an independent branch
 * picker, so the three could disagree — you could be reading Koramangala's
 * occupancy while Payments was still scoped to HSR. That is a real hazard on a
 * screen that records cash against a named resident, so scope is chosen once
 * and shared.
 *
 * A manager is pinned to their own branch server-side; for them this is set
 * once at sign-in and never offered as a choice.
 */
@Singleton
class BranchContext @Inject constructor() {

    private val _selected = MutableStateFlow<BranchSelection?>(null)
    val selected: StateFlow<BranchSelection?> = _selected.asStateFlow()

    private val _branches = MutableStateFlow<List<BranchDto>>(emptyList())
    val branches: StateFlow<List<BranchDto>> = _branches.asStateFlow()

    /** True once the branch list has been fetched, so screens can tell
     *  "still loading" apart from "this org genuinely has no branches". */
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    fun setBranches(list: List<BranchDto>) {
        _branches.value = list
        _loaded.value = true
        // If the current selection vanished (deleted branch), drop it rather
        // than leaving screens pointed at something that no longer exists.
        val current = _selected.value
        if (current != null && list.none { it.id == current.id }) {
            _selected.value = null
        }
        // A branch scope is always active once branches are available. This
        // keeps Home, Occupancy and Payments focused on the same property
        // instead of presenting an empty "Select branch" state.
        if (_selected.value == null) {
            list.firstOrNull()?.let { _selected.value = BranchSelection(it.id, it.name) }
        }
    }

    fun select(id: String, name: String) {
        _selected.value = BranchSelection(id, name)
    }

    fun clear() {
        _selected.value = null
    }
}
