package com.iocl.knowledgehub.data.local.dao

import androidx.room.*
import com.iocl.knowledgehub.data.local.entity.NewspaperEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NewspaperDao {
    @Query("SELECT * FROM newspapers ORDER BY publishDate DESC")
    fun getAll(): Flow<List<NewspaperEntity>>

    @Query("SELECT * FROM newspapers WHERE publishDate BETWEEN :startDate AND :endDate ORDER BY publishDate DESC")
    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<NewspaperEntity>>

    @Query("SELECT * FROM newspapers WHERE title LIKE '%' || :query || '%' OR headline LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<NewspaperEntity>>

    @Query("SELECT * FROM newspapers WHERE id = :id")
    suspend fun getById(id: String): NewspaperEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(newspapers: List<NewspaperEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(newspaper: NewspaperEntity)

    @Delete
    suspend fun delete(newspaper: NewspaperEntity)

    @Query("DELETE FROM newspapers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM newspapers")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM newspapers")
    suspend fun getCount(): Int
}
