package com.example.localdatebase.planner

interface PlannerResultCallback<T> {
    fun onSuccess(value: T)
    fun onError(error: Exception)
}
