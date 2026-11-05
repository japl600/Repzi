package com.japl.repzi.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.japl.repzi.core.database.model.DraftSetEntity
import com.japl.repzi.core.database.model.WorkoutDraftEntity
import com.japl.repzi.core.database.model.WorkoutDraftWithSets

// Abstract class instead of interface: @Transaction needs a method with a body.
@Dao
abstract class WorkoutDraftDao {

    @Transaction
    @Query("SELECT * FROM workout_drafts WHERE routine_id = :routineId")
    abstract suspend fun getByRoutine(routineId: Long): WorkoutDraftWithSets?

    /**
     * Replaces the draft of the routine in one transaction: the old rows go first (their sets with them, through
     * ON DELETE CASCADE) so no set of a previous version can survive.
     */
    @Transaction
    open suspend fun save(draft: WorkoutDraftEntity, sets: List<DraftSetEntity>) {
        delete(draft.routineId)
        insert(draft)
        insertSets(sets)
    }

    @Query("DELETE FROM workout_drafts WHERE routine_id = :routineId")
    abstract suspend fun delete(routineId: Long)

    @Insert
    protected abstract suspend fun insert(draft: WorkoutDraftEntity)

    @Insert
    protected abstract suspend fun insertSets(sets: List<DraftSetEntity>)
}
