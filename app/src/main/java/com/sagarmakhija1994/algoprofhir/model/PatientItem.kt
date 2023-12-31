package com.sagarmakhija1994.algoprofhir.model

import java.time.LocalDate

data class PatientItem(
    val id: String,
    val resourceId: String,
    val name: String,
    val gender: String,
    val dob: LocalDate? = null,
    val phone: String,
    val city: String,
    val country: String,
    val isActive: Boolean,
    val html: String,
    var risk: String? = "",
    var riskItem: RiskAssessmentItem? = null
) {
    override fun toString(): String = name
}