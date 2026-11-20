package com.japl.repzi.core.domain

/** Keeps the exercise catalog up to date in the background. Features never see how it is scheduled. */
interface CatalogSyncScheduler {
    /** Safe to call on every app start: it never creates a second schedule. */
    fun schedule()
}
