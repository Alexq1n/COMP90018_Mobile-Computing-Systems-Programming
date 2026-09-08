package com.roammate.logic.utils

import com.roammate.logic.interfaces.IPoiRepository
import com.roammate.logic.models.*
import kotlin.math.*

/**
 * Mock in-memory POI repository for testing
 */
class MockPoiRepository : IPoiRepository {

    private val allPOIs: List<POI> = createMelbournePOIs()

    override fun getPOIsByCity(city: String): List<POI> {
        return allPOIs.filter { it.city.equals(city, ignoreCase = true) && !it.isFiller }
    }

    override fun getPOIsByCategory(city: String, category: POICategory): List<POI> {
        return allPOIs.filter {
            it.city.equals(city, ignoreCase = true) &&
            it.category == category &&
            !it.isFiller
        }
    }

    override fun getFillerPOIs(city: String): List<POI> {
        return allPOIs.filter { it.city.equals(city, ignoreCase = true) && it.isFiller }
    }

    override fun getPOIsWithinRadius(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double,
        city: String
    ): List<POI> {
        return allPOIs.filter { poi ->
            poi.city.equals(city, ignoreCase = true) &&
            GeoUtils.calculateDistance(latitude, longitude, poi.coordinates.latitude, poi.coordinates.longitude) <= radiusMeters
        }
    }

    override fun getPOIById(id: String): POI? {
        return allPOIs.find { it.id == id }
    }

    /**
     * Create mock Melbourne POIs dataset
     */
    private fun createMelbournePOIs(): List<POI> {
        return listOf(
            // Museums
            POI(
                id = "poi_001",
                name = "National Gallery of Victoria",
                baseScore = 9.2,
                category = POICategory.MUSEUM,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8226, 144.9692),
                recommendedVisitDuration = 120,
                operatingHours = OperatingHours("10:00", "17:00"),
                city = "Melbourne",
                description = "Australia's oldest and most visited art museum"
            ),
            POI(
                id = "poi_002",
                name = "Melbourne Museum",
                baseScore = 8.8,
                category = POICategory.MUSEUM,
                budgetLevel = BudgetLevel.MEDIUM,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8033, 144.9717),
                recommendedVisitDuration = 150,
                operatingHours = OperatingHours("10:00", "17:00"),
                city = "Melbourne",
                description = "Natural and cultural history museum"
            ),

            // Parks
            POI(
                id = "poi_003",
                name = "Royal Botanic Gardens",
                baseScore = 9.5,
                category = POICategory.PARK,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.OUTDOOR,
                coordinates = Coordinates(-37.8304, 144.9800),
                recommendedVisitDuration = 90,
                operatingHours = OperatingHours("07:30", "17:30"),
                city = "Melbourne",
                description = "Beautiful botanical gardens along the Yarra River"
            ),
            POI(
                id = "poi_004",
                name = "Fitzroy Gardens",
                baseScore = 8.3,
                category = POICategory.PARK,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.OUTDOOR,
                coordinates = Coordinates(-37.8136, 144.9798),
                recommendedVisitDuration = 60,
                operatingHours = OperatingHours("06:00", "20:00"),
                city = "Melbourne",
                description = "Historic gardens with Cook's Cottage"
            ),

            // Landmarks
            POI(
                id = "poi_005",
                name = "Flinders Street Station",
                baseScore = 8.7,
                category = POICategory.LANDMARK,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.OUTDOOR,
                coordinates = Coordinates(-37.8183, 144.9671),
                recommendedVisitDuration = 30,
                operatingHours = OperatingHours("00:00", "23:59"),
                city = "Melbourne",
                description = "Iconic railway station and meeting place"
            ),
            POI(
                id = "poi_006",
                name = "Federation Square",
                baseScore = 8.5,
                category = POICategory.LANDMARK,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.MIXED,
                coordinates = Coordinates(-37.8179, 144.9690),
                recommendedVisitDuration = 45,
                operatingHours = OperatingHours("00:00", "23:59"),
                city = "Melbourne",
                description = "Modern public square and cultural precinct"
            ),

            // Shopping
            POI(
                id = "poi_007",
                name = "Queen Victoria Market",
                baseScore = 9.0,
                category = POICategory.SHOPPING,
                budgetLevel = BudgetLevel.MEDIUM,
                environment = Environment.MIXED,
                coordinates = Coordinates(-37.8076, 144.9568),
                recommendedVisitDuration = 120,
                operatingHours = OperatingHours("06:00", "15:00"),
                city = "Melbourne",
                description = "Historic open-air market"
            ),
            POI(
                id = "poi_008",
                name = "Bourke Street Mall",
                baseScore = 7.8,
                category = POICategory.SHOPPING,
                budgetLevel = BudgetLevel.MEDIUM,
                environment = Environment.MIXED,
                coordinates = Coordinates(-37.8136, 144.9631),
                recommendedVisitDuration = 90,
                operatingHours = OperatingHours("10:00", "19:00"),
                city = "Melbourne",
                description = "Premier shopping destination"
            ),

            // Entertainment
            POI(
                id = "poi_009",
                name = "Crown Casino",
                baseScore = 8.2,
                category = POICategory.ENTERTAINMENT,
                budgetLevel = BudgetLevel.HIGH,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8226, 144.9581),
                recommendedVisitDuration = 120,
                operatingHours = OperatingHours("00:00", "23:59"),
                city = "Melbourne",
                description = "Entertainment complex with casino and dining"
            ),
            POI(
                id = "poi_010",
                name = "Melbourne Aquarium",
                baseScore = 8.0,
                category = POICategory.ENTERTAINMENT,
                budgetLevel = BudgetLevel.MEDIUM,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8204, 144.9580),
                recommendedVisitDuration = 90,
                operatingHours = OperatingHours("09:30", "18:00"),
                city = "Melbourne",
                description = "Oceanarium featuring Antarctic and Australian aquatic life"
            ),

            // Restaurants
            POI(
                id = "poi_011",
                name = "Chinatown Melbourne",
                baseScore = 8.6,
                category = POICategory.RESTAURANT,
                budgetLevel = BudgetLevel.MEDIUM,
                environment = Environment.MIXED,
                coordinates = Coordinates(-37.8103, 144.9684),
                recommendedVisitDuration = 75,
                operatingHours = OperatingHours("11:00", "22:00"),
                city = "Melbourne",
                description = "Historic dining precinct with Asian cuisine"
            ),
            POI(
                id = "poi_012",
                name = "Lygon Street Italian Precinct",
                baseScore = 8.4,
                category = POICategory.RESTAURANT,
                budgetLevel = BudgetLevel.MEDIUM,
                environment = Environment.MIXED,
                coordinates = Coordinates(-37.7989, 144.9669),
                recommendedVisitDuration = 90,
                operatingHours = OperatingHours("11:00", "23:00"),
                city = "Melbourne",
                description = "Famous Italian restaurant strip"
            ),

            // Cultural
            POI(
                id = "poi_013",
                name = "Arts Centre Melbourne",
                baseScore = 8.9,
                category = POICategory.CULTURAL,
                budgetLevel = BudgetLevel.MEDIUM,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8216, 144.9685),
                recommendedVisitDuration = 120,
                operatingHours = OperatingHours("09:00", "23:00"),
                city = "Melbourne",
                description = "Premier performing arts venue"
            ),
            POI(
                id = "poi_014",
                name = "State Library Victoria",
                baseScore = 8.5,
                category = POICategory.CULTURAL,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8098, 144.9652),
                recommendedVisitDuration = 60,
                operatingHours = OperatingHours("10:00", "18:00"),
                city = "Melbourne",
                description = "Historic library with stunning reading room"
            ),

            // Beach
            POI(
                id = "poi_015",
                name = "St Kilda Beach",
                baseScore = 8.8,
                category = POICategory.BEACH,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.OUTDOOR,
                coordinates = Coordinates(-37.8679, 144.9738),
                recommendedVisitDuration = 120,
                operatingHours = OperatingHours("00:00", "23:59"),
                city = "Melbourne",
                description = "Popular beach with pier and penguins"
            ),

            // Historical
            POI(
                id = "poi_016",
                name = "Shrine of Remembrance",
                baseScore = 8.7,
                category = POICategory.HISTORICAL,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.MIXED,
                coordinates = Coordinates(-37.8304, 144.9733),
                recommendedVisitDuration = 60,
                operatingHours = OperatingHours("10:00", "17:00"),
                city = "Melbourne",
                description = "War memorial with museum and observation deck"
            ),
            POI(
                id = "poi_017",
                name = "Old Melbourne Gaol",
                baseScore = 8.2,
                category = POICategory.HISTORICAL,
                budgetLevel = BudgetLevel.MEDIUM,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8076, 144.9651),
                recommendedVisitDuration = 90,
                operatingHours = OperatingHours("09:30", "17:00"),
                city = "Melbourne",
                description = "Historic prison museum"
            ),

            // Nature
            POI(
                id = "poi_018",
                name = "Yarra River Walk",
                baseScore = 8.3,
                category = POICategory.NATURE,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.OUTDOOR,
                coordinates = Coordinates(-37.8197, 144.9648),
                recommendedVisitDuration = 75,
                operatingHours = OperatingHours("00:00", "23:59"),
                city = "Melbourne",
                description = "Scenic riverside walking path"
            ),
            POI(
                id = "poi_019",
                name = "Dandenong Ranges",
                baseScore = 9.1,
                category = POICategory.NATURE,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.OUTDOOR,
                coordinates = Coordinates(-37.8339, 145.3508),
                recommendedVisitDuration = 180,
                operatingHours = OperatingHours("08:00", "17:00"),
                city = "Melbourne",
                description = "Mountain range with lush forests and villages"
            ),

            // Sports
            POI(
                id = "poi_020",
                name = "Melbourne Cricket Ground (MCG)",
                baseScore = 9.3,
                category = POICategory.SPORTS,
                budgetLevel = BudgetLevel.MEDIUM,
                environment = Environment.OUTDOOR,
                coordinates = Coordinates(-37.8200, 144.9834),
                recommendedVisitDuration = 90,
                operatingHours = OperatingHours("10:00", "17:00"),
                city = "Melbourne",
                description = "Iconic cricket and sports stadium"
            ),

            // Cafes
            POI(
                id = "poi_021",
                name = "Degraves Street Cafes",
                baseScore = 8.4,
                category = POICategory.CAFE,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.MIXED,
                coordinates = Coordinates(-37.8165, 144.9687),
                recommendedVisitDuration = 45,
                operatingHours = OperatingHours("07:00", "17:00"),
                city = "Melbourne",
                description = "Famous laneway cafe culture"
            ),
            POI(
                id = "poi_022",
                name = "Centre Place",
                baseScore = 8.1,
                category = POICategory.CAFE,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.MIXED,
                coordinates = Coordinates(-37.8144, 144.9660),
                recommendedVisitDuration = 40,
                operatingHours = OperatingHours("07:00", "17:00"),
                city = "Melbourne",
                description = "Charming laneway with cafes and street art"
            ),

            // Scenic Spots
            POI(
                id = "poi_023",
                name = "Eureka Skydeck",
                baseScore = 9.0,
                category = POICategory.SCENIC_SPOT,
                budgetLevel = BudgetLevel.MEDIUM,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8216, 144.9648),
                recommendedVisitDuration = 60,
                operatingHours = OperatingHours("12:00", "22:00"),
                city = "Melbourne",
                description = "Observation deck with panoramic city views"
            ),

            // Nightlife
            POI(
                id = "poi_024",
                name = "Chapel Street Nightlife",
                baseScore = 8.0,
                category = POICategory.NIGHTLIFE,
                budgetLevel = BudgetLevel.MEDIUM,
                environment = Environment.MIXED,
                coordinates = Coordinates(-37.8515, 144.9943),
                recommendedVisitDuration = 120,
                operatingHours = OperatingHours("18:00", "03:00"),
                city = "Melbourne",
                description = "Vibrant nightlife and entertainment district"
            ),

            // More variety
            POI(
                id = "poi_025",
                name = "Royal Exhibition Building",
                baseScore = 8.6,
                category = POICategory.HISTORICAL,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8047, 144.9717),
                recommendedVisitDuration = 60,
                operatingHours = OperatingHours("10:00", "16:00"),
                city = "Melbourne",
                description = "UNESCO World Heritage listed exhibition building"
            ),
            POI(
                id = "poi_026",
                name = "Melbourne Zoo",
                baseScore = 8.9,
                category = POICategory.ENTERTAINMENT,
                budgetLevel = BudgetLevel.MEDIUM,
                environment = Environment.OUTDOOR,
                coordinates = Coordinates(-37.7839, 144.9514),
                recommendedVisitDuration = 180,
                operatingHours = OperatingHours("09:00", "17:00"),
                city = "Melbourne",
                description = "Historic zoo with diverse animal exhibits"
            ),
            POI(
                id = "poi_027",
                name = "Luna Park Melbourne",
                baseScore = 8.3,
                category = POICategory.ENTERTAINMENT,
                budgetLevel = BudgetLevel.MEDIUM,
                environment = Environment.OUTDOOR,
                coordinates = Coordinates(-37.8679, 144.9770),
                recommendedVisitDuration = 150,
                operatingHours = OperatingHours("11:00", "18:00"),
                city = "Melbourne",
                description = "Historic amusement park by the beach"
            ),
            POI(
                id = "poi_028",
                name = "Hosier Lane",
                baseScore = 8.7,
                category = POICategory.CULTURAL,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.OUTDOOR,
                coordinates = Coordinates(-37.8171, 144.9692),
                recommendedVisitDuration = 30,
                operatingHours = OperatingHours("00:00", "23:59"),
                city = "Melbourne",
                description = "Famous street art laneway"
            ),
            POI(
                id = "poi_029",
                name = "Southbank Promenade",
                baseScore = 8.5,
                category = POICategory.SCENIC_SPOT,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.OUTDOOR,
                coordinates = Coordinates(-37.8226, 144.9631),
                recommendedVisitDuration = 60,
                operatingHours = OperatingHours("00:00", "23:59"),
                city = "Melbourne",
                description = "Riverside dining and entertainment precinct"
            ),
            POI(
                id = "poi_030",
                name = "Brighton Beach Boxes",
                baseScore = 8.8,
                category = POICategory.LANDMARK,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.OUTDOOR,
                coordinates = Coordinates(-37.9206, 144.9858),
                recommendedVisitDuration = 45,
                operatingHours = OperatingHours("00:00", "23:59"),
                city = "Melbourne",
                description = "Iconic colorful bathing boxes"
            ),

            // ======= FILLER POIs (Hidden Gems) =======
            POI(
                id = "filler_001",
                name = "Gelato Messina",
                baseScore = 7.5,
                category = POICategory.CAFE,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8155, 144.9655),
                recommendedVisitDuration = 15,
                operatingHours = OperatingHours("12:00", "23:00"),
                isFiller = true,
                city = "Melbourne",
                description = "Popular gelato shop"
            ),
            POI(
                id = "filler_002",
                name = "Hopetoun Tea Rooms",
                baseScore = 7.8,
                category = POICategory.CAFE,
                budgetLevel = BudgetLevel.MEDIUM,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8150, 144.9657),
                recommendedVisitDuration = 30,
                operatingHours = OperatingHours("08:00", "17:00"),
                isFiller = true,
                city = "Melbourne",
                description = "Historic tea rooms"
            ),
            POI(
                id = "filler_003",
                name = "Block Arcade",
                baseScore = 7.6,
                category = POICategory.SHOPPING,
                budgetLevel = BudgetLevel.MEDIUM,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8153, 144.9666),
                recommendedVisitDuration = 20,
                operatingHours = OperatingHours("09:00", "18:00"),
                isFiller = true,
                city = "Melbourne",
                description = "Heritage shopping arcade"
            ),
            POI(
                id = "filler_004",
                name = "Presgrave Place Street Art",
                baseScore = 7.3,
                category = POICategory.CULTURAL,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.OUTDOOR,
                coordinates = Coordinates(-37.8166, 144.9671),
                recommendedVisitDuration = 10,
                operatingHours = OperatingHours("00:00", "23:59"),
                isFiller = true,
                city = "Melbourne",
                description = "Hidden street art alley"
            ),
            POI(
                id = "filler_005",
                name = "Manchester Unity Building",
                baseScore = 7.4,
                category = POICategory.HISTORICAL,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8138, 144.9643),
                recommendedVisitDuration = 15,
                operatingHours = OperatingHours("09:00", "17:00"),
                isFiller = true,
                city = "Melbourne",
                description = "Art Deco architectural gem"
            ),
            POI(
                id = "filler_006",
                name = "AC/DC Lane",
                baseScore = 7.2,
                category = POICategory.CULTURAL,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.OUTDOOR,
                coordinates = Coordinates(-37.8154, 144.9679),
                recommendedVisitDuration = 10,
                operatingHours = OperatingHours("00:00", "23:59"),
                isFiller = true,
                city = "Melbourne",
                description = "Rock music heritage laneway"
            ),
            POI(
                id = "filler_007",
                name = "Emporium Food Court",
                baseScore = 7.5,
                category = POICategory.RESTAURANT,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8121, 144.9642),
                recommendedVisitDuration = 30,
                operatingHours = OperatingHours("10:00", "21:00"),
                isFiller = true,
                city = "Melbourne",
                description = "Modern food hall"
            ),
            POI(
                id = "filler_008",
                name = "The Causeway",
                baseScore = 7.1,
                category = POICategory.SHOPPING,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8147, 144.9668),
                recommendedVisitDuration = 15,
                operatingHours = OperatingHours("09:00", "18:00"),
                isFiller = true,
                city = "Melbourne",
                description = "Historic shopping arcade"
            ),
            POI(
                id = "filler_009",
                name = "Kirkcaldie & Stains",
                baseScore = 7.0,
                category = POICategory.SHOPPING,
                budgetLevel = BudgetLevel.MEDIUM,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8140, 144.9650),
                recommendedVisitDuration = 20,
                operatingHours = OperatingHours("10:00", "18:00"),
                isFiller = true,
                city = "Melbourne",
                description = "Boutique department store"
            ),
            POI(
                id = "filler_010",
                name = "Nicholas Building Rooftop",
                baseScore = 7.7,
                category = POICategory.SCENIC_SPOT,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.OUTDOOR,
                coordinates = Coordinates(-37.8139, 144.9650),
                recommendedVisitDuration = 20,
                operatingHours = OperatingHours("09:00", "17:00"),
                isFiller = true,
                city = "Melbourne",
                description = "Hidden rooftop with city views"
            ),
            POI(
                id = "filler_011",
                name = "Meyers Place Bar",
                baseScore = 7.3,
                category = POICategory.CAFE,
                budgetLevel = BudgetLevel.MEDIUM,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8162, 144.9683),
                recommendedVisitDuration = 25,
                operatingHours = OperatingHours("16:00", "01:00"),
                isFiller = true,
                city = "Melbourne",
                description = "Hidden laneway bar"
            ),
            POI(
                id = "filler_012",
                name = "Chocolate Buddha",
                baseScore = 7.2,
                category = POICategory.CAFE,
                budgetLevel = BudgetLevel.MEDIUM,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8170, 144.9690),
                recommendedVisitDuration = 20,
                operatingHours = OperatingHours("11:00", "22:00"),
                isFiller = true,
                city = "Melbourne",
                description = "Japanese fusion cafe"
            ),
            POI(
                id = "filler_013",
                name = "Campbell Arcade",
                baseScore = 7.0,
                category = POICategory.SHOPPING,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8181, 144.9668),
                recommendedVisitDuration = 15,
                operatingHours = OperatingHours("08:00", "20:00"),
                isFiller = true,
                city = "Melbourne",
                description = "Underground shopping arcade"
            ),
            POI(
                id = "filler_014",
                name = "Curtin House Rooftop",
                baseScore = 7.6,
                category = POICategory.CAFE,
                budgetLevel = BudgetLevel.MEDIUM,
                environment = Environment.OUTDOOR,
                coordinates = Coordinates(-37.8130, 144.9642),
                recommendedVisitDuration = 30,
                operatingHours = OperatingHours("12:00", "23:00"),
                isFiller = true,
                city = "Melbourne",
                description = "Rooftop bar with city views"
            ),
            POI(
                id = "filler_015",
                name = "Little Collins Street Bookshop",
                baseScore = 7.1,
                category = POICategory.SHOPPING,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8145, 144.9660),
                recommendedVisitDuration = 20,
                operatingHours = OperatingHours("10:00", "18:00"),
                isFiller = true,
                city = "Melbourne",
                description = "Independent bookstore"
            )
        )
    }
}
