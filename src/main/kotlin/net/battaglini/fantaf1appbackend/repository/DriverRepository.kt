package net.battaglini.fantaf1appbackend.repository

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.withContext
import net.battaglini.fantaf1appbackend.model.Driver
import org.springframework.stereotype.Repository

/**
 * Repository for accessing Driver data from Firestore.
 *
 * This repository provides methods to retrieve all drivers, find a driver by ID,
 * and find a driver by their acronym.
 *
 * @property defaultFirestoreInstance The Firestore instance used for database operations.
 */
@Repository
class DriverRepository(
    private val defaultFirestoreInstance: Firestore
) {
    private val collectionName = "drivers"

    /**
     * Retrieves all drivers from the "drivers" collection.
     *
     * @return A [Flow] emitting [Driver] objects.
     */
    suspend fun getDrivers(): Flow<Driver> {
        val snapshot = withContext(Dispatchers.IO) {
            defaultFirestoreInstance.collection(collectionName).get().get()
        }
        return snapshot.map { driver -> driver.toObject(Driver::class.java) }.asFlow()
    }

    /**
     * Finds a driver by their unique ID.
     *
     * @param id The unique identifier of the driver.
     * @return The [Driver] object if found, or null if no driver exists with the given ID.
     */
    suspend fun findDriverById(id: String): Driver? {
        val snapshot = withContext(Dispatchers.IO) {
            defaultFirestoreInstance.collection(collectionName).document(id).get().get()
        }
        if (!snapshot.exists()) {
            return null
        }
        return snapshot.toObject(Driver::class.java)
    }

    /**
     * Finds a driver by their acronym.
     *
     * @param acronym The acronym of the driver (e.g., "HAM", "VER").
     * @return The [Driver] object if found, or null if no driver exists with the given acronym.
     */
    suspend fun findDriverByAcronym(acronym: String): Driver? {
        val snapshot = withContext(Dispatchers.IO) {
            defaultFirestoreInstance.collection(collectionName)
                .whereEqualTo("acronym", acronym).get().get()
        }
        if (snapshot.isEmpty) {
            return null
        }
        return snapshot.map { driver -> driver.toObject(Driver::class.java) }.first()
    }
}