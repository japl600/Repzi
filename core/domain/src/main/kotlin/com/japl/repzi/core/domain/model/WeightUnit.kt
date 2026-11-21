package com.japl.repzi.core.domain.model

/**
 * The unit the user works in. Weights are always stored in kilograms; a unit only decides how a weight is typed and
 * shown, so switching it never changes what was logged.
 */
enum class WeightUnit(private val kilogramsPerUnit: Double) {
    KG(1.0),
    LB(0.45359237),
    ;

    fun fromKg(kg: Double): Double = kg / kilogramsPerUnit

    fun toKg(value: Double): Double = value * kilogramsPerUnit
}
