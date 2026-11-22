package uk.ac.dmu.smartcafe

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class CartItem(val itemId: String, val name: String, val price: Double, var quantity: Int)

data class OrderState(
    val orderId: String,
    val status: String
)

class CafeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = CafeDatabase(application)
    private val privacyStore = PrivacyStore(application)

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUserUid = MutableStateFlow<String?>(null)
    val currentUserUid: StateFlow<String?> = _currentUserUid.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _cartTotal = MutableStateFlow(0.0)
    val cartTotal: StateFlow<Double> = _cartTotal.asStateFlow()

    private val _currentOrder = MutableStateFlow<OrderState?>(null)
    val currentOrder: StateFlow<OrderState?> = _currentOrder.asStateFlow()
    
    val privacyPreferences = privacyStore.privacyPreferencesFlow


    init {
        viewModelScope.launch {
            privacyStore.privacyPreferencesFlow.collect { prefs ->
                if (!prefs.analytics) {
                    db.clearEvents()
                }
            }
        }
    }

    fun register(email: String, passwordPlain: String) {
        if (email.isBlank() || passwordPlain.length < 6) {
            _authError.value = "Invalid email or password (min 6 chars)"
            return
        }
        val uid = UUID.randomUUID().toString()
        try {
            db.registerUser(uid, email, passwordPlain)
            _authError.value = null
            _currentUserUid.value = uid
            _isLoggedIn.value = true
        } catch (e: Exception) {
            _authError.value = "Registration failed: ${e.message}"
        }
    }

    fun login(email: String, passwordPlain: String) {
        if (email.isBlank() || passwordPlain.isBlank()) {
            _authError.value = "Email and password cannot be empty"
            return
        }
        val success = db.loginUser(email, passwordPlain)
        if (success) {
            _authError.value = null
            // We'd typically get the UID from db, but keeping it simple
            // We should really query the UID based on email.
            _currentUserUid.value = "user-${email}"
            _isLoggedIn.value = true
        } else {
            _authError.value = "Invalid credentials"
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentUserUid.value = null
        _cart.value = emptyList()
        updateCartTotal()
        _currentOrder.value = null
    }

    fun addToCart(itemId: String, name: String, price: Double) {
        val currentCart = _cart.value.toMutableList()
        val existingItem = currentCart.find { it.itemId == itemId }
        if (existingItem != null) {
            existingItem.quantity += 1
        } else {
            currentCart.add(CartItem(itemId, name, price, 1))
        }
        _cart.value = currentCart
        updateCartTotal()
    }

    fun updateQuantity(itemId: String, quantity: Int) {
        val currentCart = _cart.value.toMutableList()
        val existingItemIndex = currentCart.indexOfFirst { it.itemId == itemId }
        if (existingItemIndex != -1) {
            if (quantity <= 0) {
                currentCart.removeAt(existingItemIndex)
            } else {
                currentCart[existingItemIndex].quantity = quantity
            }
        }
        _cart.value = currentCart
        updateCartTotal()
    }

    fun removeFromCart(itemId: String) {
        val currentCart = _cart.value.toMutableList()
        currentCart.removeAll { it.itemId == itemId }
        _cart.value = currentCart
        updateCartTotal()
    }

    private fun updateCartTotal() {
        val total = CartMath.calculateTotal(_cart.value)
        _cartTotal.value = total
    }

    fun placeOrder(branchName: String) {
        if (_cart.value.isEmpty()) return
        val uid = _currentUserUid.value ?: return

        val orderId = UUID.randomUUID().toString()
        _currentOrder.value = OrderState(orderId, "Pending")
        
        val summary = _cart.value.joinToString { "${it.quantity}x ${it.name}" }
        val total = _cartTotal.value
        db.saveOrder(CafeDatabase.Order(orderId, branchName, summary, total, "Pending", System.currentTimeMillis()), uid)
        refreshOrders()

        _cart.value = emptyList()
        updateCartTotal()

        simulateOrderLifecycle(orderId)
    }

    private val _orders = MutableStateFlow<List<CafeDatabase.Order>>(emptyList())
    val orders: StateFlow<List<CafeDatabase.Order>> = _orders.asStateFlow()

    fun refreshOrders() {
        val uid = _currentUserUid.value ?: return
        _orders.value = db.getOrders(uid)
    }

    private val _reviews = MutableStateFlow<List<CafeDatabase.Review>>(emptyList())
    val reviews: StateFlow<List<CafeDatabase.Review>> = _reviews.asStateFlow()

    fun refreshReviews() {
        _reviews.value = db.getReviews()
    }
    
    fun saveReview(rating: Int, comment: String, sentiment: String) {
        db.saveReview(CafeDatabase.Review(UUID.randomUUID().toString(), rating, comment, sentiment))
        refreshReviews()
    }
    
    fun getMenuItems(): List<CafeDatabase.MenuItem> {
        return db.getMenuItems()
    }


    private fun simulateOrderLifecycle(orderId: String) {
        viewModelScope.launch {
            val statuses = listOf("Preparing", "Ready for Collection", "Collected")
            for (status in statuses) {
                delay(7000L)
                if (_currentOrder.value?.orderId == orderId) {
                    _currentOrder.value = OrderState(orderId, status)
                    db.updateOrderStatus(orderId, status)
                    refreshOrders()
                } else {
                    break // Another order was placed or cleared
                }
            }
        }
    }

    fun acceptTerms() {
        viewModelScope.launch {
            privacyStore.acceptTerms()
        }
    }

    fun updatePrivacyPreference(keyString: String, enabled: Boolean) {
        viewModelScope.launch {
            val key = when (keyString) {
                "analytics" -> PrivacyStore.ANALYTICS_KEY
                "localAi" -> PrivacyStore.LOCAL_AI_KEY
                "location" -> PrivacyStore.LOCATION_KEY
                "photos" -> PrivacyStore.PHOTOS_KEY
                else -> null
            }
            if (key != null) {
                privacyStore.updatePreference(key, enabled)
            }
            if (keyString.equals("analytics", ignoreCase = true) && !enabled) {
                db.clearEvents()
            }
        }
    }
}
