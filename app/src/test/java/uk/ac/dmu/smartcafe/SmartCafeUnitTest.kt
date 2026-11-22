package uk.ac.dmu.smartcafe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SmartCafeUnitTest {

    @Test
    fun testLocalSentiment_positiveInput() {
        val sentiment = LocalSentiment()
        val result = sentiment.predict("great coffee and fast service")
        assertNotNull(result)
        assertEquals("Positive", result?.label)
    }

    @Test
    fun testLocalSentiment_negativeInput() {
        val sentiment = LocalSentiment()
        val result = sentiment.predict("terrible food and slow service")
        assertNotNull(result)
        assertEquals("Negative", result?.label)
    }

    @Test
    fun testLocalSentiment_unknownInput() {
        val sentiment = LocalSentiment()
        // Use words that are completely out of vocabulary and not filtered out by length < 3
        val result = sentiment.predict("xyzabc qwerty uiop")
        assertNull(result)
    }

    @Test
    fun testLocalSentiment_negationLimitation() {
        val sentiment = LocalSentiment()

        // "not good at all" is in the training set, so "not_good" is in the negative vocabulary.
        // Thus, "not good" evaluates to Negative.
        val result = sentiment.predict("not good")
        assertNotNull(result)
        assertEquals("Negative", result?.label)

        // The limitation is that negation is highly coupled to exact phrases in training.
        // For example, "not bad" would logically be positive or neutral, but "not_bad"
        // is completely unknown since it wasn't in the training set.
        val result2 = sentiment.predict("not bad")
        assertNull(result2) // Handled as unknown

        // "not great" would logically be negative, but "not_great" is also unknown.
        val result3 = sentiment.predict("not great")
        assertNull(result3) // Handled as unknown
    }

    @Test
    fun testCartMath_calculateTotal_emptyCart() {
        val cart = emptyList<CartItem>()
        val total = CartMath.calculateTotal(cart)
        assertEquals(0.0, total, 0.001)
    }

    @Test
    fun testCartMath_calculateTotal_singleItem() {
        val cart = listOf(CartItem("1", "Latte", 3.50, 1))
        val total = CartMath.calculateTotal(cart)
        assertEquals(3.50, total, 0.001)
    }

    @Test
    fun testCartMath_calculateTotal_multipleItemsWithQuantities() {
        val cart = listOf(
            CartItem("1", "Latte", 3.50, 2),
            CartItem("2", "Cake", 4.00, 1),
            CartItem("3", "Espresso", 2.50, 3)
        )
        val total = CartMath.calculateTotal(cart)
        // 3.5*2 + 4.0*1 + 2.5*3 = 7.0 + 4.0 + 7.5 = 18.5
        assertEquals(18.5, total, 0.001)
    }
}
