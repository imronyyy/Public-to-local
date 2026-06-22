package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class LocalRepository(private val businessDao: BusinessDao) {

    val allBusinesses: Flow<List<Business>> = businessDao.getAllBusinesses()
    val allOffers: Flow<List<Offer>> = businessDao.getAllOffers()
    val allQueries: Flow<List<CustomerInquiry>> = businessDao.getAllQueries()

    fun getBusinessesByCategory(category: String): Flow<List<Business>> {
        return businessDao.getBusinessesByCategory(category)
    }

    fun getOffersByBusiness(businessId: Int): Flow<List<Offer>> {
        return businessDao.getOffersByBusiness(businessId)
    }

    fun getQueriesForBusiness(businessId: Int): Flow<List<CustomerInquiry>> {
        return businessDao.getQueriesByBusiness(businessId)
    }

    suspend fun registerBusiness(business: Business): Long {
        return businessDao.insertBusiness(business)
    }

    suspend fun postOffer(offer: Offer) {
        businessDao.insertOffer(offer)
    }

    suspend fun submitCustomerQuery(query: CustomerInquiry) {
        businessDao.insertQuery(query)
    }

    suspend fun answerQuery(query: CustomerInquiry, answerText: String) {
        val updated = query.copy(isAnswered = true, answer = answerText)
        businessDao.updateQuery(updated)
    }

    suspend fun prepopulateIfEmpty() {
        val current = allBusinesses.first()
        if (current.isEmpty()) {
            val list = listOf(
                // HEALTH (Hospitals, Clinics, Pharmacies)
                Business(
                    name = "Metro Care Superspecialty Hospital",
                    category = "Health",
                    phone = "+91 98765 43210",
                    address = "Sector 15, Near City Park, Metro Hub",
                    latitude = 28.6139,
                    longitude = 77.2090,
                    isOpen = true,
                    rating = 4.8f,
                    reviewsCount = 124,
                    feesOrPriceRange = "₹₹₹",
                    isFeatured = true,
                    isVerified = true
                ),
                Business(
                    name = "Lifesaver 24/7 Pharmacy",
                    category = "Health",
                    phone = "+91 98765 00112",
                    address = "G-4, High Street Avenue, Lane 2",
                    latitude = 28.6180,
                    longitude = 77.2150,
                    isOpen = true,
                    rating = 4.5f,
                    reviewsCount = 89,
                    feesOrPriceRange = "₹",
                    isFeatured = false,
                    isVerified = true
                ),
                Business(
                    name = "Dr. Sharma's Pediatric Clinic",
                    category = "Health",
                    phone = "+91 99112 23344",
                    address = "Flat 102, Blossom Plaza, Sector 12",
                    latitude = 28.6050,
                    longitude = 77.2001,
                    isOpen = false, // Closed
                    rating = 4.6f,
                    reviewsCount = 42,
                    feesOrPriceRange = "₹₹",
                    isFeatured = false,
                    isVerified = false
                ),

                // EDUCATION (Schools, Coaching, Tuition)
                Business(
                    name = "Apex IIT-JEE & NEET Academy",
                    category = "Education",
                    phone = "+91 95555 12345",
                    address = "3rd Floor, Knowledge Tower, Sector 62",
                    latitude = 28.6250,
                    longitude = 77.2210,
                    isOpen = true,
                    rating = 4.9f,
                    reviewsCount = 310,
                    feesOrPriceRange = "₹₹₹₹",
                    isFeatured = true,
                    isVerified = true
                ),
                Business(
                    name = "St. Paul's International School",
                    category = "Education",
                    phone = "+91 91111 22222",
                    address = "Campus Road, Green Valley, Phase I",
                    latitude = 28.5980,
                    longitude = 77.1950,
                    isOpen = true,
                    rating = 4.7f,
                    reviewsCount = 188,
                    feesOrPriceRange = "₹₹₹",
                    isFeatured = true,
                    isVerified = true
                ),
                Business(
                    name = "Saraswati Home Tuitions",
                    category = "Education",
                    phone = "+91 94444 88888",
                    address = "Pocket C, H-124, Rohini Complex",
                    latitude = 28.6300,
                    longitude = 77.1850,
                    isOpen = true,
                    rating = 4.3f,
                    reviewsCount = 18,
                    feesOrPriceRange = "₹",
                    isFeatured = false,
                    isVerified = false
                ),

                // FOOD/LIFESTYLE (Restaurants, Cafes, Bakeries)
                Business(
                    name = "The Gourmet Street Cafe",
                    category = "Food/Lifestyle",
                    phone = "+91 90000 77112",
                    address = "Booth 42, Galleria Market, Phase II",
                    latitude = 28.6110,
                    longitude = 77.2250,
                    isOpen = true,
                    rating = 4.6f,
                    reviewsCount = 205,
                    feesOrPriceRange = "₹₹",
                    isFeatured = true,
                    isVerified = true
                ),
                Business(
                    name = "Urban Spice Fine Dine Restaurant",
                    category = "Food/Lifestyle",
                    phone = "+91 98888 11122",
                    address = "Plot 67, Ring Road Commercial Complex",
                    latitude = 28.6191,
                    longitude = 77.2012,
                    isOpen = true,
                    rating = 4.4f,
                    reviewsCount = 153,
                    feesOrPriceRange = "₹₹₹",
                    isFeatured = false,
                    isVerified = true
                ),
                Business(
                    name = "Golden Crust Premium Bakery",
                    category = "Food/Lifestyle",
                    phone = "+91 96666 55443",
                    address = "Central Plaza Mall, GF Shop 1",
                    latitude = 28.6080,
                    longitude = 77.2188,
                    isOpen = true,
                    rating = 4.5f,
                    reviewsCount = 92,
                    feesOrPriceRange = "₹₹",
                    isFeatured = false,
                    isVerified = false
                ),

                // EMERGENCY (Electricians, Plumbers, Mechanics, Packers)
                Business(
                    name = "24/7 QuickFix Plumbers & Electricians",
                    category = "Emergency",
                    phone = "+91 88888 99999",
                    address = "Mobile Service Van (Prompt Arrival)",
                    latitude = 28.6140,
                    longitude = 77.2085,
                    isOpen = true,
                    rating = 4.8f,
                    reviewsCount = 145,
                    feesOrPriceRange = "₹₹",
                    isFeatured = true,
                    isVerified = true
                ),
                Business(
                    name = "Express Car & Bike Mechanic Hub",
                    category = "Emergency",
                    phone = "+91 87654 32109",
                    address = "Sector 4, Near Indian Oil Fuel Pump",
                    latitude = 28.6222,
                    longitude = 77.2054,
                    isOpen = true,
                    rating = 4.4f,
                    reviewsCount = 67,
                    feesOrPriceRange = "₹₹",
                    isFeatured = false,
                    isVerified = true
                ),
                Business(
                    name = "SafePack Packers and Movers",
                    category = "Emergency",
                    phone = "+91 81234 56789",
                    address = "Godown Area 4C, Highway Bypass Road",
                    latitude = 28.6410,
                    longitude = 77.2350,
                    isOpen = true,
                    rating = 4.7f,
                    reviewsCount = 83,
                    feesOrPriceRange = "₹₹₹",
                    isFeatured = false,
                    isVerified = false
                )
            )

            list.forEach { business ->
                val insertedId = businessDao.insertBusiness(business)
                if (business.isFeatured && business.category == "Food/Lifestyle") {
                    businessDao.insertOffer(
                        Offer(
                            businessId = insertedId.toInt(),
                            title = "Flat 20% Off on Gourmet Mains",
                            description = "Valid on all lunch and dinner gourmet orders above ₹500.",
                            discountPercent = 20,
                            validUntil = "30 Jun 2026"
                        )
                    )
                } else if (business.isFeatured && business.category == "Education") {
                    businessDao.insertOffer(
                        Offer(
                            businessId = insertedId.toInt(),
                            title = "Free Demo Class + 15% Scholarship",
                            description = "Avail interactive JEE/NEET counselor guidance & custom fee discount.",
                            discountPercent = 15,
                            validUntil = "15 Jul 2026"
                        )
                    )
                } else if (business.isFeatured && business.category == "Emergency") {
                    businessDao.insertOffer(
                        Offer(
                            businessId = insertedId.toInt(),
                            title = "Free Home Diagnosis",
                            description = "Pay only for materials. Diagnostic visit and estimation are completely free.",
                            discountPercent = 100,
                            validUntil = "05 Jul 2026"
                        )
                    )
                }
            }
        }
    }
}
