package uk.ac.dmu.smartcafe

object CartMath {
    fun calculateTotal(cart: List<CartItem>): Double {
        return cart.sumOf { it.price * it.quantity }
    }
}
