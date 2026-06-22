package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AIRecommendationResult(
    val recommendedIds: List<Int>,
    val explanationText: String
)

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun getRecommendations(
        query: String,
        allBusinesses: List<Business>
    ): AIRecommendationResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        
        // Safety Fallback check
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY") {
            Log.w("GeminiService", "API key is placeholder or empty. Using local smart fallback parser.")
            return@withContext getLocalFallbackRecommendations(query, allBusinesses)
        }

        try {
            // Build business list snapshot to send to Gemini
            val businessSnapshot = allBusinesses.joinToString("\n") { b ->
                "ID: ${b.id} | Name: ${b.name} | Category: ${b.category} | Address: ${b.address} | Rating: ${b.rating} | Verified: ${b.isVerified} | Price: ${b.feesOrPriceRange}"
            }

            // Prompt engineering
            val systemInstruction = "You are a local neighborhood finder and service concierge in the 'Local Connect' app. " +
                    "Your job is to recommend 1-3 best businesses from the provided local directory that match the user's intent. " +
                    "You must output a raw valid JSON object with EXACTLY keys 'ids' (a JSON array of integers matching selected recommended shop IDs) and 'explanation' (a brief friendly 3-line explanation in Hinglish/English explaining why they are recommended and suggesting context like 'urgent' or 'reviews'). Do not output any markdown formatting delimiters like ```json or anything else. Just pure raw JSON."

            val prompt = """
                User Query: "$query"
                
                Available Directory Businesses:
                $businessSnapshot
                
                Pick the most appropriate recommendation from the directory list. If no exact match exists, pick the 1-2 closest matching or helpful ones. Return raw JSON.
            """.trimIndent()

            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstruction)
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("GeminiService", "API Error: ${response.code} - ${response.message}")
                    return@withContext getLocalFallbackRecommendations(query, allBusinesses)
                }

                val bodyStr = response.body?.string() ?: ""
                try {
                    val root = JSONObject(bodyStr)
                    val candidate = root.getJSONArray("candidates").getJSONObject(0)
                    val textOutput = candidate.getJSONObject("content")
                        .getJSONArray("parts").getJSONObject(0).getString("text")
                        
                    // Clean possible markdown wrapper
                    val cleanedJson = textOutput.trim()
                        .removePrefix("```json")
                        .removeSuffix("```")
                        .trim()

                    val resultObj = JSONObject(cleanedJson)
                    val idsArray = resultObj.getJSONArray("ids")
                    val recIds = mutableListOf<Int>()
                    for (i in 0 until idsArray.length()) {
                        recIds.add(idsArray.getInt(i))
                    }
                    val explanation = resultObj.getString("explanation")
                    
                    return@withContext AIRecommendationResult(recIds, explanation)
                } catch (e: Exception) {
                    Log.e("GeminiService", "Parsing failed, attempting substring recovery: $bodyStr", e)
                    // If parsing custom format fails, try standard fallback parsing on textual output
                    return@withContext getLocalFallbackRecommendations(query, allBusinesses)
                }
            }

        } catch (e: Exception) {
            Log.e("GeminiService", "Network or General Call error", e)
            return@withContext getLocalFallbackRecommendations(query, allBusinesses)
        }
    }

    private fun getLocalFallbackRecommendations(
        query: String,
        allBusinesses: List<Business>
    ): AIRecommendationResult {
        val q = query.lowercase()
        val matched = mutableListOf<Business>()
        
        val explanationText = when {
            // Health Category matches
            q.contains("doctor") || q.contains("hospital") || q.contains("clinic") || q.contains("child") || q.contains("pharmacy") || q.contains("medical") || q.contains("medicine") || q.contains("fever") -> {
                matched.addAll(allBusinesses.filter { it.category == "Health" })
                "Hamein aapki health care request mili. Hamare directory mein verified Medical hub, critical care hospitals, aur 24/7 pharmacies hain jo emergency services ke liye open hain."
            }
            // Education Catalog matches
            q.contains("school") || q.contains("academy") || q.contains("tuition") || q.contains("coaching") || q.contains("study") || q.contains("class") || q.contains("kids") || q.contains("best school") -> {
                matched.addAll(allBusinesses.filter { it.category == "Education" })
                "Education options filter ho chuke hain. St. Paul's school aur Apex academy unke high rating aur scholarship offers ke liye custom recommendation hain."
            }
            // Emergency Utility matches
            q.contains("plumber") || q.contains("electrician") || q.contains("mechanic") || q.contains("repair") || q.contains("urgent") || q.contains("leak") || q.contains("wiring") || q.contains("emergency") || q.contains("packer") -> {
                matched.addAll(allBusinesses.filter { it.category == "Emergency" })
                "Urgent response utilities active! 24/7 QuickFix Plumbers aur mechanics onsite support ke liye verified partners hain jo turant help ke liye avail-able hain."
            }
            // Food/Lifestyle matches
            q.contains("cafe") || q.contains("restaurant") || q.contains("food") || q.contains("bakery") || q.contains("hungry") || q.contains("dine") || q.contains("eat") || q.contains("cake") -> {
                matched.addAll(allBusinesses.filter { it.category == "Food/Lifestyle" })
                "Delicious neighborhood food hubs detected! Elite rating ke visual card par 'Gourmet Cafe' aur 'Urban Spice' unke running discount coupons ke sath suggest kiye gaye hain."
            }
            // General best/verified queries
            q.contains("best") || q.contains("featured") || q.contains("verified") || q.contains("top") -> {
                matched.addAll(allBusinesses.filter { it.isFeatured || it.isVerified }.take(3))
                "Aapke neighborhood ke top premium aur highly-rated, verified local establishments ki recommendation list niche share ki gayi hai."
            }
            // Catch all default keywords match
            else -> {
                val words = q.split(" ")
                val candidates = allBusinesses.filter { b ->
                    words.any { word -> 
                        word.length > 2 && (
                            b.name.lowercase().contains(word) || b.address.lowercase().contains(word) || b.category.lowercase().contains(word)
                        )
                    }
                }
                if (candidates.isNotEmpty()) {
                    matched.addAll(candidates)
                    "Aapke keyword search ke relative high matches suggest kiye gaye hain. Detail rating aur directory details check karein."
                } else {
                    matched.addAll(allBusinesses.filter { it.isFeatured }.take(2))
                    "Humne hamara Local Connect Directory check kiya. Yahan aapko high review vale trusted local providers aur discount active businesses milenge."
                }
            }
        }

        val recIds = matched.map { it.id }
        return AIRecommendationResult(recIds, explanationText)
    }
}
