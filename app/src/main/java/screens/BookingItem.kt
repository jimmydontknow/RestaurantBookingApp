package com.example.restaurantbookingapp.screens

data class BookingItem(
    val id: String,
    val bookingCode: String,
    val guestName: String,
    val guestPhone: String,
    val tableSummary: String,
    val totalAmount: Double,
    val status: String,

    // THÊM NGÀY + GIỜ ĐẶT
    val bookingDate: String = "",
    val bookingTime: String = "",
    val hasInvoice: Boolean = false
)
