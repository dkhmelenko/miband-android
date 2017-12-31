package com.khmelenko.lab.miband.model

import java.text.SimpleDateFormat
import java.util.*

/**
 * Battery info

 * @author Dmytro Khmelenko
 */
class BatteryInfo private constructor(private val level: Int,
                                      private val cycles: Int,
                                      private val status: Status?,
                                      private val lastChargedDate: Calendar = Calendar.getInstance()) {

    internal enum class Status {
        UNKNOWN, LOW, FULL, CHARGING, NOT_CHARGING;

        companion object {

            fun fromByte(b: Byte): Status {
                return when (b.toInt()) {
                    1 -> LOW
                    2 -> CHARGING
                    3 -> FULL
                    4 -> NOT_CHARGING
                    else -> UNKNOWN
                }
            }
        }
    }

    override fun toString(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:SS", Locale.getDefault())
        val formattedDate = formatter.format(lastChargedDate.time)
        return "cycles:" + cycles +
                ",level:" + level +
                ",status:" + status +
                ",last:" + formattedDate
    }

    companion object {

        /**
         * Creates an instance of the battery info from byte data

         * @param data Byte data
         * *
         * @return Battery info
         */
        fun fromByteData(data: ByteArray): BatteryInfo {
            val level = data[0].toInt()
            val status = Status.fromByte(data[9])
            val cycles = 0xffff and (0xff and data[7].toInt() or (0xff and data[8].toInt() shl 8))

            val lastChargeDay = Calendar.getInstance()
            lastChargeDay.set(Calendar.YEAR, data[1] + 2000)
            lastChargeDay.set(Calendar.MONTH, data[2].toInt())
            lastChargeDay.set(Calendar.DATE, data[3].toInt())

            lastChargeDay.set(Calendar.HOUR_OF_DAY, data[4].toInt())
            lastChargeDay.set(Calendar.MINUTE, data[5].toInt())
            lastChargeDay.set(Calendar.SECOND, data[6].toInt())

            return BatteryInfo(level, cycles, status, lastChargeDay)
        }
    }

}
