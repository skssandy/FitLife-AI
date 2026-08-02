package com.fitlife.ai.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class BloodMarkerDto(
    val name: String = "",
    val value: Double? = null,
    val unit: String = "",
    val refLow: Double? = null,
    val refHigh: Double? = null,
    val category: String? = null
)
