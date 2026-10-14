package com.m4isper.repository

import androidx.room.withTransaction
import com.m4isper.domain.model.Pagination
import com.m4isper.domain.model.Planet
import com.m4isper.domain.repository.PlanetRepository
import com.m4isper.local.PlanetsDatabase
import com.m4isper.local.entity.ApodEntity
import com.m4isper.remote.api.ApodPage
import com.m4isper.remote.api.NasaApiService
import com.m4isper.remote.api.getApodPage
import com.m4isper.remote.api.getApodRecord
import com.m4isper.repository.mappers.toDomain
import com.m4isper.repository.mappers.toEntity
import com.m4isper.repository.mappers.toKeys
import com.m4isper.repository.mappers.toPagination
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlanetRepositoryImpl @Inject constructor(
    private val database: PlanetsDatabase,
    private val api: NasaApiService,
) : PlanetRepository {

    override fun observePlanets(): Flow<List<Planet>> =
        database.apodDao().observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observePagination(): Flow<Pagination?> =
        database.remoteKeysDao().observeKeys().map { keys -> keys?.toPagination() }

    override suspend fun refresh() {
        val page = api.getApodPage(page = Pagination.INITIAL_PAGE)
        val pagination = Pagination.first(page.totalPages)

        // Swap the cache only after a successful response —
        // a failed request never wipes the offline cache.
        database.withTransaction {
            database.remoteKeysDao().clear()
            database.apodDao().clearAll()
            database.apodDao().insertAll(page.toEntities(pageNumber = pagination.currentPage))
            database.remoteKeysDao().insert(pagination.toKeys())
        }
    }

    override suspend fun loadMore(after: Pagination): Pagination? {
        if (!after.hasMore) return null

        val page = api.getApodPage(page = after.nextPage)
        val entities = page.toEntities(pageNumber = after.nextPage)
        // Nothing to add ends the archive, whatever the page header claimed. Advancing
        // anyway would keep the feed asking for pages that hold nothing.
        val advanced = after.advancedBy(loadedCount = entities.size) ?: return null

        database.withTransaction {
            database.apodDao().insertAll(entities)
            database.remoteKeysDao().insert(advanced.toKeys())
        }
        return advanced
    }

    override fun getPlanet(date: String): Flow<Planet?> =
        database.apodDao().observeByDate(date).map { it?.toDomain() }

    override suspend fun fetchPlanet(date: String): Boolean {
        // A 404 is the feed's way of saying it has no such date; it must stay
        // distinguishable from a dropped connection, which a retry could get past.
        val dto = api.getApodRecord(date = date) ?: return false
        // page 0 puts a record fetched on its own at the top of the feed: the user
        // just asked for it, and the normal paginated path will overwrite the
        // position once that page is loaded.
        val entity = dto.toEntity(page = 0, position = 0) ?: return false
        database.apodDao().upsert(entity)
        return true
    }

    private fun ApodPage.toEntities(pageNumber: Int): List<ApodEntity> =
        items.mapIndexedNotNull { index, dto -> dto.toEntity(page = pageNumber, position = index) }
}