package com.vboard.aac.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vboard.aac.data.local.db.entity.VocabCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabCardDao {

    @Query("SELECT * FROM vocab_cards ORDER BY display_order ASC")
    fun getAllCards(): Flow<List<VocabCardEntity>>

    @Query("SELECT * FROM vocab_cards WHERE category_id = :categoryId ORDER BY display_order ASC")
    fun getCardsByCategory(categoryId: String): Flow<List<VocabCardEntity>>

    @Query("SELECT * FROM vocab_cards WHERE id = :id")
    suspend fun getCardById(id: String): VocabCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: VocabCardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<VocabCardEntity>)

    @Update
    suspend fun updateCard(card: VocabCardEntity)

    @Delete
    suspend fun deleteCard(card: VocabCardEntity)

    @Query("DELETE FROM vocab_cards WHERE id = :id")
    suspend fun deleteCardById(id: String)

    @Query("SELECT COUNT(*) FROM vocab_cards")
    suspend fun getCardCount(): Int

    @Query("DELETE FROM vocab_cards WHERE is_custom = 0")
    suspend fun deleteDefaultCards()

    @Query("SELECT * FROM vocab_cards WHERE is_custom = 1 ORDER BY display_order ASC")
    fun getCustomCards(): Flow<List<VocabCardEntity>>
}
