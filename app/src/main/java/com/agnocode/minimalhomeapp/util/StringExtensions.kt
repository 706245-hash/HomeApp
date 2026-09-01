package com.agnocode.minimalhomeapp.util

/**
 * Checks if the string matches the query using a simple fuzzy search algorithm.
 * It checks if the characters in the query appear in the same order in the string.
 */
fun String.isFuzzyMatch(query: String): Boolean {
    if (query.isEmpty()) return true
    val cleanQuery = query.lowercase().replace(" ", "")
    val cleanText = this.lowercase().replace(" ", "")
    
    var queryIndex = 0
    for (char in cleanText) {
        if (queryIndex < cleanQuery.length && char == cleanQuery[queryIndex]) {
            queryIndex++
        }
    }
    return queryIndex == cleanQuery.length
}
