package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Update
    suspend fun updatePost(post: PostEntity)

    @Query("DELETE FROM posts")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<PostEntity>)
}

@Dao
interface PeerDao {
    @Query("SELECT * FROM peers ORDER BY isOnline DESC, lastSeen DESC")
    fun getAllPeers(): Flow<List<PeerEntity>>

    @Query("SELECT * FROM peers WHERE username = :username LIMIT 1")
    suspend fun getPeerByUsername(username: String): PeerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeer(peer: PeerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(peers: List<PeerEntity>)

    @Update
    suspend fun updatePeer(peer: PeerEntity)

    @Query("DELETE FROM peers")
    suspend fun deleteAll()
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE (sender = :user1 AND receiver = :user2) OR (sender = :user2 AND receiver = :user1) ORDER BY timestamp ASC")
    fun getMessagesBetween(user1: String, user2: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE sender = :user OR receiver = :user ORDER BY timestamp DESC")
    fun getAllUserMessages(user: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)
}

@Database(entities = [PostEntity::class, PeerEntity::class, MessageEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun peerDao(): PeerDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "offgrid_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
