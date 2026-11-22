package uk.ac.dmu.smartcafe

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest
import java.security.SecureRandom

class CafeDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "smart_cafe.db"
        const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE users (
                uid TEXT PRIMARY KEY,
                email TEXT UNIQUE,
                password_hash TEXT,
                salt TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE menu_items (
                id TEXT PRIMARY KEY,
                name TEXT,
                category TEXT,
                price REAL,
                description TEXT,
                available INTEGER
            )
        """)

        db.execSQL("""
            CREATE TABLE orders (
                order_id TEXT PRIMARY KEY,
                customer_uid TEXT,
                branch_name TEXT,
                items_summary TEXT,
                total_price REAL,
                status TEXT,
                created_at_ms INTEGER
            )
        """)

        db.execSQL("""
            CREATE TABLE feedback (
                feedback_id TEXT PRIMARY KEY,
                order_id TEXT,
                rating INTEGER,
                comment TEXT,
                sentiment_label TEXT,
                created_at_ms INTEGER
            )
        """)

        db.execSQL("""
            CREATE TABLE events (
                event_id TEXT PRIMARY KEY,
                kind TEXT,
                at_ms INTEGER
            )
        """)

        seedMenuItems(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database upgrade
    }

    private fun seedMenuItems(db: SQLiteDatabase) {
        val items = listOf(
            arrayOf("item1", "Espresso", "Drinks", 2.50, "Strong black coffee", 1),
            arrayOf("item2", "Latte", "Drinks", 3.50, "Milky coffee", 1),
            arrayOf("item3", "Cappuccino", "Drinks", 3.50, "Frothy coffee", 1),
            arrayOf("item4", "Tea", "Drinks", 2.00, "English breakfast tea", 1),
            arrayOf("item5", "Chocolate Cake", "Cakes", 4.00, "Rich chocolate layer cake", 1),
            arrayOf("item6", "Carrot Cake", "Cakes", 4.00, "Spiced carrot cake with frosting", 1),
            arrayOf("item7", "Lemon Drizzle", "Cakes", 3.50, "Zesty lemon cake", 1),
            arrayOf("item8", "Blueberry Muffin", "Cakes", 3.00, "Fresh baked muffin", 0) // Unavailable
        )

        db.beginTransaction()
        try {
            val sql = "INSERT OR IGNORE INTO menu_items (id, name, category, price, description, available) VALUES (?, ?, ?, ?, ?, ?)"
            val statement = db.compileStatement(sql)
            for (item in items) {
                statement.bindString(1, item[0] as String)
                statement.bindString(2, item[1] as String)
                statement.bindString(3, item[2] as String)
                statement.bindDouble(4, item[3] as Double)
                statement.bindString(5, item[4] as String)
                statement.bindLong(6, (item[5] as Int).toLong())
                statement.executeInsert()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun purgeExpiredOrders() {
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val threshold = System.currentTimeMillis() - thirtyDaysMs

        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("orders", "created_at_ms < ?", arrayOf(threshold.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun clearEvents() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("events", null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun registerUser(uid: String, email: String, passwordPlain: String) {
        val secureRandom = SecureRandom()
        val saltBytes = ByteArray(16)
        secureRandom.nextBytes(saltBytes)

        val saltHex = saltBytes.joinToString("") { "%02x".format(it) }

        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(saltHex.toByteArray(Charsets.UTF_8))
        val hashBytes = digest.digest(passwordPlain.toByteArray(Charsets.UTF_8))
        val hashHex = hashBytes.joinToString("") { "%02x".format(it) }

        val db = writableDatabase
        val values = ContentValues().apply {
            put("uid", uid)
            put("email", email)
            put("password_hash", hashHex)
            put("salt", saltHex)
        }
        db.insert("users", null, values)
    }

    fun loginUser(email: String, passwordPlain: String): Boolean {
        val db = readableDatabase
        db.query("users", arrayOf("password_hash", "salt"), "email = ?", arrayOf(email), null, null, null).use { cursor ->
            if (cursor.moveToFirst()) {
                val hashIndex = cursor.getColumnIndexOrThrow("password_hash")
                val saltIndex = cursor.getColumnIndexOrThrow("salt")

                val storedHash = cursor.getString(hashIndex)
                val storedSalt = cursor.getString(saltIndex)

                val digest = MessageDigest.getInstance("SHA-256")
                digest.update(storedSalt.toByteArray(Charsets.UTF_8))
                val hashBytes = digest.digest(passwordPlain.toByteArray(Charsets.UTF_8))
                val hashHex = hashBytes.joinToString("") { "%02x".format(it) }

                return hashHex == storedHash
            }
        }
        return false
    }

    data class MenuItem(val id: String, val name: String, val category: String, val price: Double, val description: String, val available: Boolean)
    fun getMenuItems(): List<MenuItem> {
        val list = mutableListOf<MenuItem>()
        val db = readableDatabase
        db.query("menu_items", null, null, null, null, null, null).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(MenuItem(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                    price = cursor.getDouble(cursor.getColumnIndexOrThrow("price")),
                    description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                    available = cursor.getInt(cursor.getColumnIndexOrThrow("available")) == 1
                ))
            }
        }
        return list
    }

    data class Order(val orderId: String, val branchName: String, val itemsSummary: String, val totalPrice: Double, val status: String, val createdAt: Long)
    fun getOrders(uid: String): List<Order> {
        val list = mutableListOf<Order>()
        val db = readableDatabase
        db.query("orders", null, "customer_uid = ?", arrayOf(uid), null, null, "created_at_ms DESC").use { cursor ->
            while (cursor.moveToNext()) {
                list.add(Order(
                    orderId = cursor.getString(cursor.getColumnIndexOrThrow("order_id")),
                    branchName = cursor.getString(cursor.getColumnIndexOrThrow("branch_name")),
                    itemsSummary = cursor.getString(cursor.getColumnIndexOrThrow("items_summary")),
                    totalPrice = cursor.getDouble(cursor.getColumnIndexOrThrow("total_price")),
                    status = cursor.getString(cursor.getColumnIndexOrThrow("status")),
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at_ms"))
                ))
            }
        }
        return list
    }

    fun saveOrder(order: Order, uid: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("order_id", order.orderId)
            put("customer_uid", uid)
            put("branch_name", order.branchName)
            put("items_summary", order.itemsSummary)
            put("total_price", order.totalPrice)
            put("status", order.status)
            put("created_at_ms", order.createdAt)
        }
        db.insert("orders", null, values)
    }

    fun updateOrderStatus(orderId: String, status: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("status", status)
        }
        db.update("orders", values, "order_id = ?", arrayOf(orderId))
    }

    data class Review(val feedbackId: String, val rating: Int, val comment: String, val sentimentLabel: String)
    fun saveReview(review: Review) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("feedback_id", review.feedbackId)
            put("rating", review.rating)
            put("comment", review.comment)
            put("sentiment_label", review.sentimentLabel)
            put("created_at_ms", System.currentTimeMillis())
        }
        db.insert("feedback", null, values)
    }

    fun getReviews(): List<Review> {
        val list = mutableListOf<Review>()
        val db = readableDatabase
        db.query("feedback", null, null, null, null, null, "created_at_ms DESC").use { cursor ->
            while (cursor.moveToNext()) {
                list.add(Review(
                    feedbackId = cursor.getString(cursor.getColumnIndexOrThrow("feedback_id")),
                    rating = cursor.getInt(cursor.getColumnIndexOrThrow("rating")),
                    comment = cursor.getString(cursor.getColumnIndexOrThrow("comment")),
                    sentimentLabel = cursor.getString(cursor.getColumnIndexOrThrow("sentiment_label"))
                ))
            }
        }
        return list
    }
}
