package net.battaglini.fantaf1appbackend.service

import net.battaglini.fantaf1appbackend.model.request.CreateLineupRequest
import net.battaglini.fantaf1appbackend.model.response.CreateLineupResponse

interface LineupService {
    suspend fun createLineup(request: CreateLineupRequest): CreateLineupResponse
}
