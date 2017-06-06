package com.khmelenko.lab.miband.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Battery info

 * @author Dmytro Khmelenko
 */
class BatteryInfo private constructor() {

    internal enum class Status {
        UNKNOWN, LOW, FULL, CHARGING, NOT_CHARGING;


        companion object {

            fun fromByte(b: Byte): Status {
                when (b) {
                    1.toByte() -> return LOW
                    2.toByte() -> return CHARGING
                    3.toByte() -> return FULL
                    4.toByte() -> return NOT_CHARGING

                    else -> return UNKNOWN
                }
            }
        }
    }

    /**
     * Gets battery level

     * @return Battery level
     */
    var level: Int = 0
        private set
    /**
     * Gets cycles

     * @return Cycles
     */
    var cycles: Int = 0
        private set
    /**
     * Gets battery status

     * @return Battery status
     */
    internal var status: Status? = null
        private set
    /**
     * Gets last charging date

     * @return Last charging date
     */
    var lastChargedDate: Calendar? = null
        private set

    override fun toString(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:SS", Locale.getDefault())
        val formattedDate = formatter.format(lastChargedDate?.time)
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
         * @return Battery info or null, if data are invalid
         */
        fun fromByteData(data: ByteArray): BatteryInfo? {
            if (data.size < 10) {
                return null
            }
            val info = BatteryInfo()

            info.level = data[0].toInt()
            info.status = Status.fromByte(data[9])
            info.cycles = 0xffff and (0xff and data[7].toInt() or (0xff and data[8].toInt() shl 8))
            info.lastChargedDate = Calendar.getInstance()

            info.lastChargedDate!!.set(Calendar.YEAR, data[1] + 2000)
            info.lastChargedDate!!.set(Calendar.MONTH, data[2].toInt())
            info.lastChargedDate!!.set(Calendar.DATE, data[3].toInt())

            info.lastChargedDate!!.set(Calendar.HOUR_OF_DAY, data[4].toInt())
            info.lastChargedDate!!.set(Calendar.MINUTE, data[5].toInt())
            info.lastChargedDate!!.set(Calendar.SECOND, data[6].toInt())

            return info
        }
    }

}
