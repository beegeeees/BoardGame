package com.example.adchaosdemo

data class DemoRoom(
    val id: String,
    val roomCode: String,
    val currentCount: Int,
    val maxCount: Int,
    val hostNickname: String
)
