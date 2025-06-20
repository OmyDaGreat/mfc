package xyz.malefic.mfc.util.todo

import java.time.LocalDate

fun String?.getParsedDate(): LocalDate? =
    this?.let {
        if (it.matches(Regex("\\d{2}-\\d{2}"))) {
            val currentYear = LocalDate.now().year
            LocalDate.parse("$currentYear-$it")
        } else {
            LocalDate.parse(it)
        }
    }
