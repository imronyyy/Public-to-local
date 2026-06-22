package com.example.ui

import android.app.Application
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.*

class LocalConnectViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "local_connect_db"
    ).build()

    private val repository = LocalRepository(db.businessDao())
    private val geminiService = GeminiService()

    // Location state
    private val locationManager = application.getSystemService(Application.LOCATION_SERVICE) as LocationManager
    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    // Basic UI inputs
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _isOwnerMode = MutableStateFlow(false)
    val isOwnerMode: StateFlow<Boolean> = _isOwnerMode.asStateFlow()

    // Owner specific state: which business they own/manage (defaults to first or newly registered)
    private val _ownedBusinessId = MutableStateFlow<Int?>(null)
    val ownedBusinessId: StateFlow<Int?> = _ownedBusinessId.asStateFlow()

    // Smart Recommendations output state
    private val _aiRecommendationResult = MutableStateFlow<AIRecommendationResult?>(null)
    val aiRecommendationResult: StateFlow<AIRecommendationResult?> = _aiRecommendationResult.asStateFlow()

    private val _isAiSearching = MutableStateFlow(false)
    val isAiSearching: StateFlow<Boolean> = _isAiSearching.asStateFlow()

    // Filter properties
    private val _filterFeatured = MutableStateFlow(false)
    val filterFeatured: StateFlow<Boolean> = _filterFeatured.asStateFlow()

    private val _filterVerified = MutableStateFlow(false)
    val filterVerified: StateFlow<Boolean> = _filterVerified.asStateFlow()

    private val _filterOpenOnly = MutableStateFlow(false)
    val filterOpenOnly: StateFlow<Boolean> = _filterOpenOnly.asStateFlow()

    // Reactive streams from database
    val allBusinesses: StateFlow<List<Business>> = repository.allBusinesses
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allOffers: StateFlow<List<Offer>> = repository.allOffers
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allQueries: StateFlow<List<CustomerInquiry>> = repository.allQueries
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
            startLocationUpdates()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _aiRecommendationResult.value = null
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        _aiRecommendationResult.value = null
    }

    fun toggleOwnerMode(enabled: Boolean) {
        _isOwnerMode.value = enabled
        if (enabled && _ownedBusinessId.value == null) {
            viewModelScope.launch {
                val list = allBusinesses.first()
                val registered = list.firstOrNull { it.registeredByOwner } ?: list.firstOrNull()
                _ownedBusinessId.value = registered?.id
            }
        }
    }

    fun selectOwnedBusiness(id: Int) {
        _ownedBusinessId.value = id
    }

    fun toggleFilterFeatured() { _filterFeatured.value = !_filterFeatured.value }
    fun toggleFilterVerified() { _filterVerified.value = !_filterVerified.value }
    fun toggleFilterOpenOnly() { _filterOpenOnly.value = !_filterOpenOnly.value }

    fun performAiSmartSearch() {
        val query = _searchQuery.value
        if (query.isBlank()) return

        viewModelScope.launch {
            _isAiSearching.value = true
            try {
                val directorySnapshot = allBusinesses.value
                val result = geminiService.getRecommendations(query, directorySnapshot)
                _aiRecommendationResult.value = result
            } catch (e: Exception) {
                Log.e("LocalConnectViewModel", "Failed to get AI recommendation", e)
            } finally {
                _isAiSearching.value = false
            }
        }
    }

    fun registerNewBusiness(
        name: String,
        category: String,
        phone: String,
        address: String,
        fees: String,
        isPremiumFeatured: Boolean,
        isPremiumVerified: Boolean
    ) {
        viewModelScope.launch {
            val baseLat = 28.6139
            val baseLng = 77.2090
            val randomLat = baseLat + (Math.random() - 0.5) * 0.05
            val randomLng = baseLng + (Math.random() - 0.5) * 0.05

            val newBiz = Business(
                name = name,
                category = category,
                phone = phone,
                address = address,
                latitude = randomLat,
                longitude = randomLng,
                isOpen = true,
                rating = 5.0f,
                reviewsCount = 1,
                feesOrPriceRange = fees,
                isFeatured = isPremiumFeatured,
                isVerified = isPremiumVerified,
                registeredByOwner = true
            )
            val newId = repository.registerBusiness(newBiz)
            _ownedBusinessId.value = newId.toInt()
            _selectedCategory.value = category
        }
    }

    fun postNewOffer(title: String, description: String, discount: Int, validUntil: String) {
        val bizId = _ownedBusinessId.value ?: return
        viewModelScope.launch {
            val offer = Offer(
                businessId = bizId,
                title = title,
                description = description,
                discountPercent = discount,
                validUntil = validUntil
            )
            repository.postOffer(offer)
        }
    }

    fun answerQuery(query: CustomerInquiry, answerText: String) {
        viewModelScope.launch {
            repository.answerQuery(query, answerText)
        }
    }

    fun submitQueryToBusiness(businessId: Int, name: String, phone: String, queryText: String) {
        viewModelScope.launch {
            val queryObj = CustomerInquiry(
                businessId = businessId,
                customerName = name,
                customerPhone = phone,
                queryText = queryText
            )
            repository.submitCustomerQuery(queryObj)
        }
    }

    private fun startLocationUpdates() {
        try {
            val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val hasNet = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    _userLocation.value = location
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            if (hasNet) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000L,
                    10f,
                    locationListener
                )
                val lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (lastKnown != null) {
                    _userLocation.value = lastKnown
                }
            }

            if (hasGps) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000L,
                    10f,
                    locationListener
                )
                val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastKnown != null) {
                    _userLocation.value = lastKnown
                }
            }
        } catch (e: SecurityException) {
            Log.w("LocalConnectViewModel", "Location permissions not granted or available yet.", e)
            setDefaultFallbackLocation()
        } catch (e: Exception) {
            Log.e("LocalConnectViewModel", "Error fetching real location", e)
            setDefaultFallbackLocation()
        }
    }

    private fun setDefaultFallbackLocation() {
        val loc = Location("fallback").apply {
            latitude = 28.6139
            longitude = 77.2090
        }
        _userLocation.value = loc
    }

    fun getDistanceToBusiness(business: Business): String {
        val userLoc = _userLocation.value ?: return "Searching location..."
        
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(business.latitude - userLoc.latitude)
        val dLng = Math.toRadians(business.longitude - userLoc.longitude)
        
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(userLoc.latitude)) * cos(Math.toRadians(business.latitude)) *
                sin(dLng / 2).pow(2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distance = earthRadius * c
        
        return if (distance < 1.0) {
            "${(distance * 1000).roundToInt()} m"
        } else {
            "${String.format("%.1f", distance)} km"
        }
    }
}
