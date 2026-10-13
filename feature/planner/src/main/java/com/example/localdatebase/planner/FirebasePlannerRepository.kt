package com.example.localdatebase.planner

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp
import com.google.gson.Gson
import com.roammate.logic.models.BudgetLevel
import com.roammate.logic.models.Itinerary
import com.roammate.logic.models.POICategory
import com.roammate.logic.models.TransportMode
import com.roammate.logic.models.UserProfile

/** Stores planner-specific preferences and generated trips under the authenticated user's UID. */
class FirebasePlannerRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val gson: Gson = Gson()
) {
    fun saveProfile(profile: UserProfile, callback: PlannerResultCallback<Unit>) {
        val user = requireUser()
        val data = mapOf(
            "interests" to profile.interests.map { it.name },
            "budgetPreference" to profile.budgetPreference.name,
            "transportMode" to profile.transportMode.name,
            "transportPreference" to profile.transportPreference.toDouble(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        profileDocument(user).set(data, SetOptions.merge())
            .addOnSuccessListener { callback.onSuccess(Unit) }
            .addOnFailureListener(callback::onError)
    }

    fun loadProfile(callback: PlannerResultCallback<UserProfile>) {
        val user = requireUser()
        profileDocument(user).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    callback.onSuccess(defaultProfile())
                    return@addOnSuccessListener
                }
                val interests = (document.get("interests") as? List<*>)
                    .orEmpty()
                    .mapNotNull { value -> enumValue<POICategory>(value as? String) }
                callback.onSuccess(
                    UserProfile(
                        interests = interests.ifEmpty { defaultProfile().interests },
                        budgetPreference = enumValue<BudgetLevel>(document.getString("budgetPreference"))
                            ?: BudgetLevel.MEDIUM,
                        transportMode = enumValue<TransportMode>(document.getString("transportMode"))
                            ?: TransportMode.WALKING,
                        transportPreference = (document.getDouble("transportPreference") ?: 1.0).toFloat()
                    )
                )
            }
            .addOnFailureListener(callback::onError)
    }

    fun saveTrip(itinerary: Itinerary, callback: PlannerResultCallback<Unit>) {
        val user = requireUser()
        require(itinerary.tripId.isNotBlank() && !itinerary.tripId.contains('/')) {
            "tripId 不能为空或包含 /"
        }
        val data = mapOf(
            "tripId" to itinerary.tripId,
            "payloadJson" to gson.toJson(itinerary),
            "totalEstimatedCost" to itinerary.totalEstimatedCost,
            "totalExpectedValue" to itinerary.totalExpectedValue,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        tripDocument(user, itinerary.tripId).set(data, SetOptions.merge())
            .addOnSuccessListener { callback.onSuccess(Unit) }
            .addOnFailureListener(callback::onError)
    }

    fun loadTrip(tripId: String, callback: PlannerResultCallback<Itinerary>) {
        val user = requireUser()
        require(tripId.isNotBlank() && !tripId.contains('/')) { "tripId 不能为空或包含 /" }
        tripDocument(user, tripId).get()
            .addOnSuccessListener { document ->
                val json = document.getString("payloadJson")
                if (!document.exists() || json.isNullOrBlank()) {
                    callback.onError(NoSuchElementException("没有找到行程 $tripId"))
                } else {
                    try {
                        callback.onSuccess(gson.fromJson(json, Itinerary::class.java))
                    } catch (error: Exception) {
                        callback.onError(error)
                    }
                }
            }
            .addOnFailureListener(callback::onError)
    }

    /** Reads every saved trip owned by the signed-in user for the Saved Trips screen. */
    fun listTrips(callback: PlannerResultCallback<List<SavedTripSummary>>) {
        val user = requireUser()
        userDocument(user).collection("trips").get()
            .addOnSuccessListener { snapshot ->
                val trips = snapshot.documents.mapNotNull { document ->
                    val itinerary = document.getString("payloadJson")
                        ?.takeIf(String::isNotBlank)
                        ?.let { json -> runCatching { gson.fromJson(json, Itinerary::class.java) }.getOrNull() }
                    val firstDay = itinerary?.days?.minByOrNull { it.dayNumber }
                    val destination = document.getString("destination")
                        ?.takeIf(String::isNotBlank)
                        ?: firstDay?.pois?.firstOrNull()?.poi?.city?.takeIf(String::isNotBlank)
                        ?: "Saved trip"
                    val dayCount = itinerary?.days?.size ?: document.getLong("dayCount")?.toInt() ?: 1
                    val dateText = document.getString("dateText")
                        ?.takeIf(String::isNotBlank)
                        ?: firstDay?.date?.takeIf(String::isNotBlank)
                        ?: "Date not set"
                    SavedTripSummary(
                        tripId = document.id,
                        destination = destination,
                        durationText = "$dayCount ${if (dayCount == 1) "day" else "days"}",
                        dateText = dateText,
                    ) to (document.getTimestamp("updatedAt") ?: Timestamp(0, 0))
                }.sortedByDescending { it.second.seconds }
                    .map { it.first }
                callback.onSuccess(trips)
            }
            .addOnFailureListener(callback::onError)
    }

    private fun profileDocument(user: FirebaseUser): DocumentReference =
        firestore.collection("users").document(user.uid)
            .collection("plannerProfile").document("default")

    private fun tripDocument(user: FirebaseUser, tripId: String): DocumentReference =
        firestore.collection("users").document(user.uid)
            .collection("trips").document(tripId)

    private fun userDocument(user: FirebaseUser): DocumentReference =
        firestore.collection("users").document(user.uid)

    private fun requireUser(): FirebaseUser = auth.currentUser
        ?: throw IllegalStateException("请先登录云端账号")

    private inline fun <reified T : Enum<T>> enumValue(value: String?): T? =
        enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) }

    companion object {
        fun defaultProfile() = UserProfile(
            interests = listOf(POICategory.MUSEUM, POICategory.NATURE, POICategory.PARK),
            budgetPreference = BudgetLevel.MEDIUM,
            transportMode = TransportMode.WALKING,
            transportPreference = 1.0f
        )
    }
}
