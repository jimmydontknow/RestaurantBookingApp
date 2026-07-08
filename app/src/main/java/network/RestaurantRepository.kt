package com.example.restaurantbookingapp.network

import org.json.JSONArray
import org.json.JSONObject

class RestaurantRepository(
    private val apiClient: ApiClient = ApiClient
) {
    fun checkServer(): ApiResult<String> {
        return when (val result = apiClient.get("/api/tables")) {
            is ApiResult.Success -> {
                val json = JSONObject(result.data)
                val count = json.optJSONArray("data")?.length() ?: 0
                ApiResult.Success("Server hoạt động, đã nhận $count bàn")
            }
            is ApiResult.Error -> result
        }
    }

    fun loadRestaurantSnapshot(): ApiResult<RestaurantSnapshot> {
        val tables = apiClient.get("/api/tables")
        if (tables is ApiResult.Error) return tables

        val menu = apiClient.get("/api/menu")
        if (menu is ApiResult.Error) return menu

        return try {
            val tableArray = JSONObject((tables as ApiResult.Success).data).optJSONArray("data") ?: JSONArray()
            val menuArray = JSONObject((menu as ApiResult.Success).data).optJSONArray("data") ?: JSONArray()
            ApiResult.Success(
                RestaurantSnapshot(
                    tableCount = tableArray.length(),
                    menuCount = menuArray.length(),
                    availableTables = countStatus(tableArray, "available"),
                    bookedTables = countStatus(tableArray, "booked"),
                    occupiedTables = countStatus(tableArray, "occupied")
                )
            )
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Không đọc được dữ liệu từ server")
        }
    }

    private fun countStatus(array: JSONArray, status: String): Int {
        var count = 0
        for (index in 0 until array.length()) {
            if (array.optJSONObject(index)?.optString("status") == status) count++
        }
        return count
    }
}

data class RestaurantSnapshot(
    val tableCount: Int,
    val menuCount: Int,
    val availableTables: Int,
    val bookedTables: Int,
    val occupiedTables: Int
)
