package com.example.sensors

import org.junit.Test

class SearchAlgorithmTest {
    @Test
    fun searchPlaces_returnsMatchingPlaces() {

        val places = listOf(
            // =========================
            // Attractions
            // =========================

            Place("Federation Square", "Attraction", "Melbourne"),
            Place("Melbourne Skydeck", "Attraction", "Southbank"),
            Place("SEA LIFE Melbourne Aquarium", "Attraction", "Melbourne"),
            Place("Queen Victoria Market", "Market", "Melbourne"),
            Place("Old Melbourne Gaol", "Historic Site", "Melbourne"),
            Place("St Paul's Cathedral", "Landmark", "Melbourne"),
            Place("Royal Exhibition Building", "Landmark", "Carlton"),
            Place("Flinders Street Station", "Landmark", "Melbourne"),
            Place("Melbourne Town Hall", "Landmark", "Melbourne"),
            Place("Hosier Lane", "Street Art", "Melbourne"),
            Place("Degraves Street", "Laneway", "Melbourne"),
            Place("Chinatown Melbourne", "Cultural Area", "Melbourne"),

            // =========================
            // Museums & Galleries
            // =========================

            Place("Melbourne Museum", "Museum", "Carlton"),
            Place("Immigration Museum", "Museum", "Melbourne"),
            Place("ACMI", "Museum", "Melbourne"),
            Place("NGV International", "Gallery", "Melbourne"),
            Place("Ian Potter Centre: NGV Australia", "Gallery", "Melbourne"),
            Place("Australian Centre for Contemporary Art", "Gallery", "Southbank"),
            Place("Scienceworks", "Science Museum", "Spotswood"),
            Place("Bunjilaka Aboriginal Cultural Centre", "Cultural Centre", "Carlton"),
            Place("Victoria Police Museum", "Museum", "Melbourne"),
            Place("Australian Music Vault", "Museum", "Southbank"),

            // =========================
            // Universities
            // =========================

            Place("University of Melbourne", "University", "Parkville"),
            Place("RMIT University", "University", "Melbourne"),
            Place("Monash University", "University", "Clayton"),
            Place("Deakin University", "University", "Burwood"),
            Place("La Trobe University", "University", "Bundoora"),
            Place("Victoria University", "University", "Footscray"),
            Place("Swinburne University of Technology", "University", "Hawthorn"),
            Place("Australian Catholic University", "University", "Fitzroy"),

            // =========================
            // Libraries
            // =========================

            Place("State Library Victoria", "Library", "Melbourne"),
            Place("City Library", "Library", "Melbourne"),
            Place("North Melbourne Library", "Library", "North Melbourne"),
            Place("Kathleen Syme Library and Community Centre", "Library", "Carlton"),
            Place("East Melbourne Library", "Library", "East Melbourne"),
            Place("Brunswick Library", "Library", "Brunswick"),
            Place("Fitzroy Library", "Library", "Fitzroy"),

            // =========================
            // Parks & Gardens
            // =========================

            Place("Royal Botanic Gardens", "Park", "Melbourne"),
            Place("Fitzroy Gardens", "Park", "East Melbourne"),
            Place("Treasury Gardens", "Park", "East Melbourne"),
            Place("Carlton Gardens", "Park", "Carlton"),
            Place("Flagstaff Gardens", "Park", "West Melbourne"),
            Place("Yarra Park", "Park", "Richmond"),
            Place("Alexandra Gardens", "Park", "Melbourne"),
            Place("Birrarung Marr", "Park", "Melbourne"),
            Place("Fawkner Park", "Park", "South Yarra"),
            Place("Albert Park", "Park", "Albert Park"),
            Place("Princes Park", "Park", "Carlton North"),

            // =========================
            // Beaches
            // =========================

            Place("St Kilda Beach", "Beach", "St Kilda"),
            Place("Brighton Beach", "Beach", "Brighton"),
            Place("Elwood Beach", "Beach", "Elwood"),
            Place("Port Melbourne Beach", "Beach", "Port Melbourne"),
            Place("Williamstown Beach", "Beach", "Williamstown"),

            // =========================
            // Sports
            // =========================

            Place("Melbourne Cricket Ground", "Sports", "Richmond"),
            Place("Melbourne Park", "Sports", "Melbourne"),
            Place("Rod Laver Arena", "Sports", "Melbourne"),
            Place("AAMI Park", "Sports", "Melbourne"),
            Place("Marvel Stadium", "Sports", "Docklands"),
            Place("John Cain Arena", "Sports", "Melbourne"),

            // =========================
            // Restaurants
            // =========================

            Place("Maha", "Restaurant", "Melbourne"),
            Place("Tipo 00", "Restaurant", "Melbourne"),
            Place("Osteria Ilaria", "Restaurant", "Melbourne"),
            Place("Chin Chin", "Restaurant", "Melbourne"),
            Place("Supernormal", "Restaurant", "Melbourne"),
            Place("Cumulus Inc.", "Restaurant", "Melbourne"),
            Place("Vue de monde", "Restaurant", "Melbourne"),
            Place("Flower Drum", "Restaurant", "Melbourne"),
            Place("Gimlet at Cavendish House", "Restaurant", "Melbourne"),
            Place("NOMAD Melbourne", "Restaurant", "Melbourne"),

            // =========================
            // Cafes
            // =========================

            Place("Higher Ground", "Cafe", "Melbourne"),
            Place("Patricia Coffee Brewers", "Cafe", "Melbourne"),
            Place("Axil Coffee Roasters", "Cafe", "Melbourne"),
            Place("Market Lane Coffee", "Cafe", "Melbourne"),
            Place("Industry Beans", "Cafe", "Fitzroy"),
            Place("Seven Seeds", "Cafe", "Carlton"),
            Place("Brother Baba Budan", "Cafe", "Melbourne"),
            Place("Proud Mary Coffee", "Cafe", "Collingwood"),
            Place("Auction Rooms", "Cafe", "North Melbourne"),
            Place("Operator25", "Cafe", "Melbourne"),

            // =========================
            // Markets
            // =========================

            Place("Queen Victoria Market", "Market", "Melbourne"),
            Place("South Melbourne Market", "Market", "South Melbourne"),
            Place("Prahran Market", "Market", "Prahran"),
            Place("South Melbourne Market", "Market", "South Melbourne"),
            Place("Camberwell Sunday Market", "Market", "Camberwell"),

            // =========================
            // Shopping
            // =========================

            Place("Melbourne Central", "Shopping Centre", "Melbourne"),
            Place("Emporium Melbourne", "Shopping Centre", "Melbourne"),
            Place("Bourke Street Mall", "Shopping Area", "Melbourne"),
            Place("Chadstone Shopping Centre", "Shopping Centre", "Chadstone"),
            Place("Queen Victoria Village", "Shopping Centre", "Melbourne"),

            // =========================
            // Transport / Tourist Hubs
            // =========================

            Place("Flinders Street Station", "Transport", "Melbourne"),
            Place("Southern Cross Station", "Transport", "Melbourne"),
            Place("Melbourne Central Station", "Transport", "Melbourne"),
            Place("Docklands", "Tourist Area", "Docklands"),
            Place("Southbank Promenade", "Tourist Area", "Southbank"),
            Place("St Kilda Pier", "Tourist Area", "St Kilda"),

            // =========================
            // Culture / Entertainment
            // =========================

            Place("Arts Centre Melbourne", "Entertainment", "Southbank"),
            Place("Princess Theatre", "Theatre", "Melbourne"),
            Place("Regent Theatre", "Theatre", "Melbourne"),
            Place("Forum Melbourne", "Entertainment", "Melbourne"),
            Place("Crown Melbourne", "Entertainment", "Southbank"),
            Place("Melbourne Convention and Exhibition Centre", "Convention Centre", "South Wharf")
        )

        val query = "Melbour123"

        val result = searchPlaces(
            query,
            places
        )

        println("Search query: $query")

        result.forEach { result ->

            println(
                "${result.place.name} " +
                        "| ${result.place.category} " +
                        "| ${result.place.suburb} " +
                        "| score=${result.score}"
            )
        }
    }
}