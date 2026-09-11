package com.aalaya.pg.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.common.SnackbarManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class ExploreUiState(
    val query: String = "",
    val loading: Boolean = true,
    val listings: List<Listing> = emptyList(),
    val error: String? = null,
    val userLocation: Pair<Double, Double>? = null,
    val locationStatus: LocationStatus = LocationStatus.Available,
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    internal val repo: DiscoveryRepository,
    internal val enquiries: com.aalaya.pg.feature.enquiries.EnquiryRepository,
    private val snackbar: SnackbarManager,
) : ViewModel() {

    private val _state = MutableStateFlow(ExploreUiState())
    val state: StateFlow<ExploreUiState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun setLocationStatus(status: LocationStatus) {
        _state.update { it.copy(locationStatus = status) }
    }

    fun onLocationAvailable(lat: Double, lng: Double) {
        val current = _state.value.userLocation
        _state.update { it.copy(userLocation = lat to lng, locationStatus = LocationStatus.Available) }
        if (current?.first == lat && current?.second == lng) return
        if (_state.value.query.isBlank()) {
            loadDefaultListings(lat, lng)
        } else {
            _state.update { it.copy(listings = sortWithDistance(it.listings, lat, lng)) }
        }
    }

    fun onLocationUnavailable(status: LocationStatus = LocationStatus.ServicesDisabled) {
        _state.update { it.copy(userLocation = null, locationStatus = status) }
        if (_state.value.listings.isEmpty()) {
            loadListings { repo.list() }
        }
    }

    fun onQuery(v: String) {
        _state.update { it.copy(query = v) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(250)
            val q = v.trim()
            if (q.isBlank()) {
                val loc = _state.value.userLocation
                if (loc != null) {
                    loadDefaultListings(loc.first, loc.second)
                } else {
                    refresh()
                }
            } else {
                executeSearch(query = q)
            }
        }
    }

    fun refresh() {
        val loc = _state.value.userLocation
        if (loc != null) {
            loadDefaultListings(loc.first, loc.second)
        } else {
            loadListings { repo.list() }
        }
    }

    private fun loadDefaultListings(lat: Double, lng: Double) {
        loadListings {
            val nearbyResult = repo.nearby(lat, lng)
            if (nearbyResult is ApiResult.Success && nearbyResult.data.isNotEmpty()) {
                nearbyResult
            } else {
                when (val all = repo.list()) {
                    is ApiResult.Success -> ApiResult.Success(sortWithDistance(all.data, lat, lng))
                    is ApiResult.Failure -> all
                }
            }
        }
    }

    private fun executeSearch(query: String) {
        loadListings {
            when (val r = repo.search(query, null)) {
                is ApiResult.Success -> {
                    val loc = _state.value.userLocation
                    val sorted = if (loc != null) sortWithDistance(r.data, loc.first, loc.second) else r.data
                    ApiResult.Success(sorted)
                }
                is ApiResult.Failure -> r
            }
        }
    }

    private fun loadListings(block: suspend () -> ApiResult<List<Listing>>) {
        loadJob?.cancel()
        _state.update { it.copy(loading = true, error = null) }
        loadJob = viewModelScope.launch {
            when (val r = block()) {
                is ApiResult.Success -> {
                    val loc = _state.value.userLocation
                    val processed = if (loc != null) sortWithDistance(r.data, loc.first, loc.second) else r.data
                    _state.update { it.copy(loading = false, listings = processed, error = null) }
                }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = r.error.message) }
            }
        }
    }

    private fun sortWithDistance(items: List<Listing>, userLat: Double, userLng: Double): List<Listing> {
        return items.map { listing ->
            if (listing.distanceKm != null) {
                listing
            } else if (listing.lat != null && listing.lng != null) {
                val d = calculateDistanceKm(userLat, userLng, listing.lat, listing.lng)
                listing.copy(distanceKm = d)
            } else {
                listing
            }
        }.sortedWith(
            compareBy<Listing> { it.distanceKm == null }
                .thenBy { it.distanceKm }
        )
    }

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c * 10.0).roundToInt() / 10.0
    }
}
