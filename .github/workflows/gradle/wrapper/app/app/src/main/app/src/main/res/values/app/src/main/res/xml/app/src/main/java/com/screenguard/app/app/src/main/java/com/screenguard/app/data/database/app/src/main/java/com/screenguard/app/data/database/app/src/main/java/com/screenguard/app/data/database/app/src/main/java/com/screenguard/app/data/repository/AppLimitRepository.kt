package com.screenguard.data.repository

import com.screenguard.data.database.AppLimitDao
import com.screenguard.data.database.AppLimitEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class AppLimitRepository(private val dao: AppLimitDao) {

    fun getAllEnabled(): Flow<List<AppLimitEntity>> = dao.getAllEnabled()
    fun getAll(): Flow<List<AppLimitEntity>> = dao.getAll()

    suspend fun getAllEnabledList(): List<AppLimitEntity> {
        return dao.getAllEnabled().firstOrNull() ?: emptyList()
    }

    suspend fun getByPackage(pkg: String): AppLimitEntity? = dao.getByPackage(pkg)

    suspend fun saveLimit(entity: AppLimitEntity) = dao.insertOrUpdate(entity)

    suspend fun deleteLimit(pkg: String) = dao.deleteByPackage(pkg)

    suspend fun toggleEnabled(pkg: String, enabled: Boolean) {
        val entity = dao.getByPackage(pkg) ?: return
        dao.insertOrUpdate(entity.copy(isEnabled = enabled))
    }
}
