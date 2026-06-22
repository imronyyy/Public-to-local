package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "businesses")
data class Business(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // Health, Education, Food, Emergency
    val phone: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val isOpen: Boolean = true,
    val rating: Float = 4.0f,
    val reviewsCount: Int = 5,
    val feesOrPriceRange: String = "$$",
    val isFeatured: Boolean = false, // Subscription Model top placement
    val isVerified: Boolean = false, // Premium Verification badge
    val registeredByOwner: Boolean = false
)

@Entity(tableName = "offers")
data class Offer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val businessId: Int,
    val title: String,
    val description: String,
    val discountPercent: Int,
    val validUntil: String
)

@Entity(tableName = "customer_inquiries")
data class CustomerInquiry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val businessId: Int,
    val customerName: String,
    val customerPhone: String,
    val queryText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isAnswered: Boolean = false,
    val answer: String? = null
)

@Dao
interface BusinessDao {
    @Query("SELECT * FROM businesses ORDER BY isFeatured DESC, rating DESC")
    fun getAllBusinesses(): Flow<List<Business>>

    @Query("SELECT * FROM businesses WHERE category = :category ORDER BY isFeatured DESC, rating DESC")
    fun getBusinessesByCategory(category: String): Flow<List<Business>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusiness(business: Business): Long

    @Query("SELECT * FROM offers ORDER BY id DESC")
    fun getAllOffers(): Flow<List<Offer>>

    @Query("SELECT * FROM offers WHERE businessId = :businessId")
    fun getOffersByBusiness(businessId: Int): Flow<List<Offer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffer(offer: Offer)

    @Query("SELECT * FROM customer_inquiries ORDER BY timestamp DESC")
    fun getAllQueries(): Flow<List<CustomerInquiry>>

    @Query("SELECT * FROM customer_inquiries WHERE businessId = :businessId ORDER BY timestamp DESC")
    fun getQueriesByBusiness(businessId: Int): Flow<List<CustomerInquiry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuery(query: CustomerInquiry)

    @Update
    suspend fun updateQuery(query: CustomerInquiry)
}

@Database(entities = [Business::class, Offer::class, CustomerInquiry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessDao(): BusinessDao
}
