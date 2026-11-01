package com.japl.repzi.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.japl.repzi.core.database.model.LoggedSetEntity
import com.japl.repzi.core.database.model.WorkoutSessionEntity
import com.japl.repzi.core.database.model.WorkoutSessionWithSets

// Abstract class instead of interface: @Transaction needs a method with a body.
@Dao
abstract class WorkoutSessionDao {

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    abstract suspend fun getById(id: Long): WorkoutSessionWithSets?

    /**
     * Inserts the session and all its sets in one transaction, so a failure never leaves a half-saved workout.
     * The session id is only known after the insert, so the `sessionId` of [sets] is overwritten here.
     */
    @Transaction
    open suspend fun save(session: WorkoutSessionEntity, sets: List<LoggedSetEntity>): Long {
        val id = insert(session)
        insertSets(sets.map { it.copy(sessionId = id) })
        return id
    }

    @Insert
    protected abstract suspend fun insert(session: WorkoutSessionEntity): Long

    @Insert
    protected abstract suspend fun insertSets(sets: List<LoggedSetEntity>)
}
