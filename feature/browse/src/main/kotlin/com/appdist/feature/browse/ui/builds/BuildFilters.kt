package com.appdist.feature.browse.ui.builds

data class BuildFilters(
    val flavor: String? = null,
    val stand: String? = null,
    val searchQuery: String = ""
) {
    val isEmpty get() = flavor == null && stand == null && searchQuery.isBlank()
}
