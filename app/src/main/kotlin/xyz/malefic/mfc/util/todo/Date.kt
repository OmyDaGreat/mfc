package xyz.malefic.mfc.util.todo

import java.time.LocalDate

/**
 * Extension function to parse a `String` into a `LocalDate`.
 *
 * This function attempts to parse the input string into a `LocalDate` object.
 * - If the string matches the format `MM-dd` (e.g., "06-28"), it assumes the current year
 *   and constructs a full date in the format `yyyy-MM-dd`.
 * - If the string does not match the `MM-dd` format, it tries to parse the string directly
 *   as a full date in ISO-8601 format (e.g., "2025-06-28").
 *
 * @receiver The input string to be parsed. Can be `null`.
 * @return A `LocalDate` object if parsing is successful, or `null` if the input is `null` or invalid.
 */
fun String.getParsedDate(): LocalDate = LocalDate.parse(if (this matches Regex("\\d{2}-\\d{2}")) "${LocalDate.now().year}-$this" else this)
