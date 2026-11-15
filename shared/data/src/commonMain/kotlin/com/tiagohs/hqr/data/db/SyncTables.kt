package com.tiagohs.hqr.data.db

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Upsert

/** Changes waiting to go to the Worker (ADR-029); a newer change of the same item replaces the older one. */
@Entity(tableName = "sync_outbox", primaryKeys = ["kind", "key"])
data class SyncOutboxEntity(
    val kind: String,
    val key: String,
    val payload: String,
    val changedAt: Long,
    val deleted: Boolean,
)

/** What the account had of each item after the last cycle: the base that tells what changed on this device. */
@Entity(tableName = "sync_state", primaryKeys = ["kind", "key"])
data class SyncStateEntity(
    val kind: String,
    val key: String,
    val payload: String,
)

/**
 * Sync values that belong to this database, like the cursor: kept here and not in the settings, which the browser's
 * tabs share while each tab may open a database of its own.
 */
@Entity(tableName = "sync_meta")
data class SyncMetaEntity(
    @PrimaryKey val name: String,
    val number: Long,
)

@Dao
interface SyncDao {
    @Query("SELECT * FROM sync_outbox ORDER BY changedAt")
    suspend fun outbox(): List<SyncOutboxEntity>

    @Upsert
    suspend fun enqueue(changes: List<SyncOutboxEntity>)

    @Query("DELETE FROM sync_outbox WHERE kind = :kind AND `key` = :key AND changedAt <= :changedAt")
    suspend fun sent(
        kind: String,
        key: String,
        changedAt: Long,
    )

    @Query("SELECT * FROM sync_state")
    suspend fun state(): List<SyncStateEntity>

    @Upsert
    suspend fun remember(items: List<SyncStateEntity>)

    @Query("DELETE FROM sync_state WHERE kind = :kind AND `key` = :key")
    suspend fun forget(
        kind: String,
        key: String,
    )

    @Query("DELETE FROM sync_state")
    suspend fun clearState()

    @Query("DELETE FROM sync_outbox")
    suspend fun clearOutbox()

    @Query("SELECT number FROM sync_meta WHERE name = :name")
    suspend fun meta(name: String): Long?

    @Upsert
    suspend fun putMeta(meta: SyncMetaEntity)

    @Query("DELETE FROM sync_meta")
    suspend fun clearMeta()
}
