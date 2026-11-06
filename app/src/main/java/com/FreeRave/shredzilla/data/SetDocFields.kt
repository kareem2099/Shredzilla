package com.FreeRave.shredzilla.data

import com.FreeRave.shredzilla.models.SetEntry
import com.FreeRave.shredzilla.models.SetType
import com.FreeRave.shredzilla.models.Tempo
import com.google.firebase.firestore.DocumentSnapshot

/** Optional set fields; absent on sets logged before they existed, which read as the defaults. */
internal object SetDocFields {
    const val TYPE = "type"
    const val ECCENTRIC = "eccentric"
    const val PAUSE = "pause"
    const val PER_SIDE = "perSide"

    fun setTypeOf(doc: DocumentSnapshot): SetType = SetType.fromStored(doc.getString(TYPE))

    fun tempoOf(doc: DocumentSnapshot): Tempo =
        Tempo(doc.getLong(ECCENTRIC)?.toInt(), doc.getLong(PAUSE)?.toInt())

    fun perSideOf(doc: DocumentSnapshot): Boolean = doc.getBoolean(PER_SIDE) ?: false

    /** Only what differs from the defaults is written. */
    fun extrasOf(entry: SetEntry): Map<String, Any> = buildMap {
        if (entry.setType != SetType.NORMAL) put(TYPE, entry.setType.storedValue)
        entry.tempo.eccentricSeconds?.let { put(ECCENTRIC, it.toLong()) }
        entry.tempo.pauseSeconds?.let { put(PAUSE, it.toLong()) }
        if (entry.perSide) put(PER_SIDE, true)
    }

    /** Fields an edit may have turned off, so they're removed when absent. */
    val CLEARABLE = listOf(TYPE, ECCENTRIC, PAUSE, PER_SIDE)
}
