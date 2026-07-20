package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// --- ROOM ENTITIES ---

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false,
    val deadlineTime: Long = 0L, // 0L means no deadline
    val durationText: String = "" // E.g., "1 hour", "5 mins"
) {
    val isExpired: Boolean
        get() = deadlineTime > 0L && System.currentTimeMillis() > deadlineTime && !isCompleted
}

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val tier: String = "Free", // Free vs Premium
    val persona: String = "Mentor", // Spartan, Mentor, Partner
    val xpPoints: Int = 150, // Points earned by completing tasks
    val avatarIndex: Int = 0, // Profile avatar selection (0 to 29)
    val isBiometricEnabled: Boolean = false, // Biometric authentication activation status
    val isVerified: Boolean = false, // Account verification status
    val planType: String = "Monthly" // Monthly vs Yearly
)

@Entity(tableName = "coaching_logs")
data class CoachingLogEntity(
    @PrimaryKey val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val persona: String,
    val userMessage: String,
    val aiResponse: String
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: String = "motivation", // motivation, reminder, blame, guidance
    val imageUrl: String? = null,
    val videoUrl: String? = null
)


// --- DAOS ---

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY updatedAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("SELECT * FROM tasks WHERE synced = 0")
    suspend fun getUnsyncedTasks(): List<TaskEntity>
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles LIMIT 1")
    fun getProfile(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles LIMIT 1")
    suspend fun getProfileOnce(): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    fun getProfileById(id: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileByIdOnce(id: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE email = :email LIMIT 1")
    fun getProfileByEmail(email: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE email = :email LIMIT 1")
    suspend fun getProfileByEmailOnce(email: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: ProfileEntity)
}

@Dao
interface CoachingLogDao {
    @Query("SELECT * FROM coaching_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<CoachingLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CoachingLogEntity)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: String)

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications()
}

// --- DATABASE ---

@Database(
    entities = [TaskEntity::class, ProfileEntity::class, CoachingLogEntity::class, NotificationEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun profileDao(): ProfileDao
    abstract fun coachingLogDao(): CoachingLogDao
    abstract fun notificationDao(): NotificationDao
}
