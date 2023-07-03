package com.artify.api.entity

import com.artify.api.entity.SnowflakeGenerator.Companion.TIMESTAMP_SHIFT
import kotlinx.datetime.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.sign

val defaultSnowflakeGenerator = SnowflakeGenerator(0)

class SnowflakeGenerator(
    private val machineId: Int
) {
    private val atomicInteger = AtomicInteger(0)

    companion object {
        val EPOCH = Instant.parse("2023-01-01T00:00:00Z")
        const val MAX_MACHINE_ID = 64
        const val TIMESTAMP_SHIFT = 22
        const val MACHINE_ID_SHIFT = 16
    }

    init {
        require(machineId < MAX_MACHINE_ID || machineId >= 0) {
            "Machine Number must between 0 - ${MAX_MACHINE_ID - 1}"
        }
    }

    fun nextId(): Snowflake {
        synchronized(this) {
            val currentTs = System.currentTimeMillis()
            val ts = currentTs - EPOCH.toEpochMilliseconds()
            val maxIncrement = 16384
            val max = maxIncrement - 2

            if (atomicInteger.get() >= max) {
                atomicInteger.set(0)
            }
            val i = atomicInteger.incrementAndGet()
            return Snowflake((ts shl TIMESTAMP_SHIFT) or (machineId shl MACHINE_ID_SHIFT).toLong() or i.toLong())
        }
    }
}

data class Snowflake(
    val id: Long
) : Comparable<Snowflake> {
    val timestamp
        get() = Instant.fromEpochMilliseconds(SnowflakeGenerator.EPOCH.toEpochMilliseconds() + (id shr TIMESTAMP_SHIFT))

    override fun compareTo(other: Snowflake): Int {
        return ((id shr TIMESTAMP_SHIFT) - (other.id shr TIMESTAMP_SHIFT)).sign
    }

    override fun equals(other: Any?): Boolean {
        return (other is Snowflake) && (other.id == id)
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Snowflake{$id}"
    }
}
