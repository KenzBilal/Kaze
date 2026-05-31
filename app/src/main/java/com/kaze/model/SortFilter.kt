package com.kaze.model

enum class SortOption(val label: String) {
    DATE_ADDED_DESC("Date Added (Newest)"),
    DATE_ADDED_ASC("Date Added (Oldest)"),
    TITLE_ASC("Title (A–Z)"),
    TITLE_DESC("Title (Z–A)"),
    YEAR_DESC("Year (Newest)"),
    YEAR_ASC("Year (Oldest)"),
    RATING_DESC("Rating (High–Low)"),
    PROGRESS("Progress (Series)")
}

enum class FilterOption(val label: String) {
    ALL("All"),
    MOVIES("Movies"),
    SERIES("Series"),
    IN_PROGRESS("In Progress")
}

data class SortFilterState(
    val sort: SortOption = SortOption.DATE_ADDED_DESC,
    val filter: FilterOption = FilterOption.ALL
)
