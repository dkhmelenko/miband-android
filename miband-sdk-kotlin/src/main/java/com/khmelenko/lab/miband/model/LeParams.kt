package com.khmelenko.lab.miband.model

/**
 * LE parameters
 *
 * @author Dmytro Khmelenko (d.khmelenko@gmail.com)
 */
class LeParams {

    var mConnIntMin: Int = 0
    var mConnIntMax: Int = 0
    var mConnInt: Int = 0

    var mLatency: Int = 0
    var mTimeout: Int = 0
    var mAdvInt: Int = 0

    companion object {
        fun fromByte(data: ByteArray): LeParams {
            var params = LeParams()
            params.mConnIntMax = 0xffff and (0xff and data[0].toInt() or (0xff and data[1].toInt() shl 8))
            params.mConnIntMax = 0xffff and (0xff and data[2].toInt() or (0xff and data[3].toInt() shl 8))
            params.mLatency = 0xffff and (0xff and data[4].toInt() or (0xff and data[5].toInt() shl 8))
            params.mTimeout = 0xffff and (0xff and data[6].toInt() or (0xff and data[7].toInt() shl 8))
            params.mConnInt = 0xffff and (0xff and data[8].toInt() or (0xff and data[9].toInt() shl 8))
            params.mAdvInt = 0xffff and (0xff and data[10].toInt() or (0xff and data[11].toInt() shl 8))

            params.mConnIntMin = (params.mConnIntMin * 1.25).toInt()
            params.mConnIntMax = (params.mConnIntMax * 1.25).toInt()
            params.mAdvInt = (params.mAdvInt * 0.625).toInt()
            params.mTimeout *= 10

            return params;
        }
    }

}