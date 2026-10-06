package com.iocl.knowledgehub.data.local.dao

import androidx.room.*
import com.iocl.knowledgehub.data.local.entity.MagazineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MagazineDao {
    @Query("SELECT * FROM magazines ORDER BY year DESC, month DESC")
    fun getAll(): Flow<List<MagazineEntity>>

    @Query("SELECT * FROM magazines WHERE year = :year ORDER BY month DESC")
    fun getByYear(year: Int): Flow<List<MagazineEntity>>

    @Query("SELECT * FROM magazines WHERE month = :month AND year = :year")
    fun getByMonthAndYear(month: Int, year: Int): Flow<List<MagazineEntity>>

    @Query("SELECT * FROM magazines WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<MagazineEntity>>

    @Query("SELECT * FROM magazines WHERE id = :id")
    suspend fun getById(id: String): MagazineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(magazines: List<MagazineEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(magazine: MagazineEntity)

    @Delete
    suspend fun delete(magazine: MagazineEntity)

    @Query("DELETE FROM magazines WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM magazines")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM magazines")
    suspend fun getCount(): Int
}
