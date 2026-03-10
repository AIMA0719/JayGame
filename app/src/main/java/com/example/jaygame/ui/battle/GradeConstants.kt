package com.example.jaygame.ui.battle

import androidx.compose.ui.graphics.Color
import com.example.jaygame.data.UnitFamily
import com.example.jaygame.data.UnitGrade

/** Grade colors indexed by grade ordinal (0=Common … 6=Immortal). */
val GradeColorsByIndex: Array<Color> = UnitGrade.entries.map { it.color }.toTypedArray()

/** Grade display names indexed by grade ordinal. */
val GradeNamesByIndex: Array<String> = UnitGrade.entries.map { it.label }.toTypedArray()

/** Family tint colors indexed by family ordinal (0=Fire … 4=Support). */
val FamilyColorsByIndex: Array<Color> = UnitFamily.entries.map { it.color }.toTypedArray()
