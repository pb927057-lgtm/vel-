package com.vel.secure.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "authorized_devices")
data class AuthorizedDeviceEntity(
    @PrimaryKey val deviceId: String,
    val deviceName: String,
    val role: String,
    val authorizationStatus: String, // AUTHORIZED, REVOKED
    val pairedAtTimestamp: Long,
    val lastSeenTimestamp: Long,
    val authTokenHash: String,
    val publicKeyHex: String
)

@Entity(tableName = "security_audit_log")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val requestId: String,
    val deviceId: String,
    val operationType: String,
    val status: String,
    val timestamp: Long,
    val isConfirmed: Boolean
)

@Dao
interface DeviceDao {
    @Query("SELECT * FROM authorized_devices")
    fun getAllDevices(): Flow<List<AuthorizedDeviceEntity>>

    @Query("SELECT * FROM authorized_devices WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getDeviceById(deviceId: String): AuthorizedDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(device: AuthorizedDeviceEntity)

    @Query("UPDATE authorized_devices SET authorizationStatus = 'REVOKED' WHERE deviceId = :deviceId")
    suspend fun markAsRevoked(deviceId: String)

    @Query("DELETE FROM authorized_devices WHERE deviceId = :deviceId")
    suspend fun deleteDevice(deviceId: String)

    @Query("SELECT * FROM security_audit_log ORDER BY timestamp DESC LIMIT 50")
    fun getRecentAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert
    suspend fun logOperation(entry: AuditLogEntity)
}

@Database(entities = [AuthorizedDeviceEntity::class, AuditLogEntity::class], version = 1, exportSchema = false)
abstract class VelDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
}
