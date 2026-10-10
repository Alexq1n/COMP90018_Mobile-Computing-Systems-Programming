package com.roammate.logic.engine

import com.roammate.logic.models.Coordinates
import com.roammate.logic.models.POI
import com.roammate.logic.utils.GeoUtils
import kotlin.math.min

/**
 * Service for geographic clustering of POIs
 */
object GeoClusteringService {

    private const val DEFAULT_CLUSTER_RADIUS_METERS = 5000.0 // 5km radius per cluster

    /**
     * Group POIs into geographic clusters based on spatial proximity
     * Uses a simple radius-based clustering approach
     */
    fun clusterPOIs(
        pois: List<POI>,
        maxClustersPerDay: Int = 3,
        clusterRadiusMeters: Double = DEFAULT_CLUSTER_RADIUS_METERS
    ): List<List<POI>> {
        if (pois.isEmpty()) return emptyList()

        val clusters = mutableListOf<MutableList<POI>>()
        val assigned = mutableSetOf<String>()

        // Sort POIs by base score descending to prioritize high-value locations
        val sortedPOIs = pois.sortedByDescending { it.baseScore }

        for (poi in sortedPOIs) {
            if (poi.id in assigned) continue

            // Create new cluster centered on this POI
            val cluster = mutableListOf(poi)
            assigned.add(poi.id)

            // Find all unassigned POIs within radius
            for (otherPoi in sortedPOIs) {
                if (otherPoi.id in assigned) continue

                val distance = GeoUtils.calculateDistance(
                    poi.coordinates.latitude,
                    poi.coordinates.longitude,
                    otherPoi.coordinates.latitude,
                    otherPoi.coordinates.longitude
                )

                if (distance <= clusterRadiusMeters) {
                    cluster.add(otherPoi)
                    assigned.add(otherPoi.id)
                }
            }

            clusters.add(cluster)
        }

        return clusters
    }

    /**
     * Distribute clusters across multiple days
     * Returns a list where each element is a day's cluster of POIs
     */
    fun distributeClustersAcrossDays(
        allPOIs: List<POI>,
        numberOfDays: Int,
        clusterRadiusMeters: Double = DEFAULT_CLUSTER_RADIUS_METERS
    ): List<List<POI>> {
        // First, cluster all POIs
        val clusters = clusterPOIs(allPOIs, numberOfDays, clusterRadiusMeters)

        // If we have fewer clusters than days, some days will have no initial cluster
        // If we have more clusters than days, merge smaller clusters
        return when {
            clusters.size <= numberOfDays -> {
                // Pad with empty lists if needed
                clusters + List(numberOfDays - clusters.size) { emptyList() }
            }
            else -> {
                // Merge smaller clusters - keep top N clusters, merge the rest into the last one
                val topClusters = clusters.take(numberOfDays - 1)
                val remainingPOIs = clusters.drop(numberOfDays - 1).flatten()
                topClusters + listOf(remainingPOIs)
            }
        }
    }

    /**
     * Find the centroid (average center point) of a cluster
     */
    fun findClusterCentroid(pois: List<POI>): Coordinates? {
        if (pois.isEmpty()) return null

        val avgLat = pois.map { it.coordinates.latitude }.average()
        val avgLon = pois.map { it.coordinates.longitude }.average()

        return Coordinates(avgLat, avgLon)
    }

    /**
     * Find the POI closest to a given coordinate within a cluster
     */
    fun findClosestPOI(
        targetLat: Double,
        targetLon: Double,
        pois: List<POI>
    ): POI? {
        if (pois.isEmpty()) return null

        return pois.minByOrNull { poi ->
            GeoUtils.calculateDistance(
                targetLat,
                targetLon,
                poi.coordinates.latitude,
                poi.coordinates.longitude
            )
        }
    }

    /**
     * Calculate the diameter (max distance between any two points) of a cluster
     */
    fun calculateClusterDiameter(pois: List<POI>): Double {
        if (pois.size < 2) return 0.0

        var maxDistance = 0.0
        for (i in pois.indices) {
            for (j in i + 1 until pois.size) {
                val distance = GeoUtils.calculateDistance(
                    pois[i].coordinates.latitude,
                    pois[i].coordinates.longitude,
                    pois[j].coordinates.latitude,
                    pois[j].coordinates.longitude
                )
                if (distance > maxDistance) {
                    maxDistance = distance
                }
            }
        }

        return maxDistance
    }
}
