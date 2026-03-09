package net.battaglini.fantaf1appbackend.repository

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.withContext
import net.battaglini.fantaf1appbackend.model.DriverCost
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper

@Repository
class DriverCostRepository(
    private val firestore: Firestore,
    private val objectMapper: ObjectMapper
) {
    suspend fun getDriversCosts(): Flow<DriverCost> {
        val snapshot = withContext(Dispatchers.IO) {
            firestore.collection(COLLECTION_NAME).get().get()
        }
        return snapshot.map { driverCost -> objectMapper.convertValue(driverCost.data, DriverCost::class.java) }
            .asFlow()
    }

    suspend fun createOrUpdateDriversCosts(costs: List<DriverCost>) {
        withContext(Dispatchers.IO) {
            firestore.runTransaction { transaction ->
                costs.forEach { cost ->
                    val reference = firestore.collection(COLLECTION_NAME).document(cost.driverId)
                    transaction.set(reference, objectMapper.convertValue(cost, Map::class.java))
                }
            }.get()
        }
    }

    companion object {
        private const val COLLECTION_NAME = "driver_costs"
    }
}