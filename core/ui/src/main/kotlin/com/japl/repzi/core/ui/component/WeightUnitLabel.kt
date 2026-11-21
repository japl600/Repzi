package com.japl.repzi.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.japl.repzi.core.domain.model.WeightUnit
import com.japl.repzi.core.ui.R

/** The short name of the unit as it is written next to a number: kg or lb. */
@Composable
fun WeightUnit.label(): String = stringResource(
    when (this) {
        WeightUnit.KG -> R.string.unit_kg
        WeightUnit.LB -> R.string.unit_lb
    },
)
