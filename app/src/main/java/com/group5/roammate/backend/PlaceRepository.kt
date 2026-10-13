package com.group5.roammate.backend

interface PlaceRepository {
    fun getPlaces(): List<Place>
}