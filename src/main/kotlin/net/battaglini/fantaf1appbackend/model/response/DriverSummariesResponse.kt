package net.battaglini.fantaf1appbackend.model.response

import net.battaglini.fantaf1appbackend.model.DriverSummary

data class DriverSummariesResponse(
    val summaries: List<DriverSummary>
)
