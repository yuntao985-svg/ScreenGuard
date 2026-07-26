package com.screenguard.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLimitDao {

    @Query("SELECT * FROM app_limits WHERE isEnabled = 1")
    fun getAllEnabled(): Flow<List<AppLimitEntity>>

    @Query("SELECT * FROM app_limits")
    fun getAll(): Flow<List<AppLimitEntity>>

    @Query("SELECT * FROM app_limits WHERE packageName = :pkg")
    suspend fun getByPackage(pkg: String): AppLimitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: AppLimitEntity)

    @Delete
    suspend fun delete(entity: AppLimitEntity)

    @Query("DELETE FROM app_limits WHERE packageName = :pkg")
    suspend fun deleteByPackage(pkg: String)
}
