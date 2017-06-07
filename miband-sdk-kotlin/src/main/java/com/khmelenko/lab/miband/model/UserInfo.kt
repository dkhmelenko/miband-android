package com.khmelenko.lab.miband.model

import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * User information

 * @author Dmytro Khmelenko
 */
class UserInfo {


    var uid: Int = 0
        private set

    var gender: Byte = 0
        private set

    var age: Byte = 0
        private set

    private var mHeight: Byte = 0
    private var mWeight: Byte = 0

    var alias = ""
        private set

    var type: Byte = 0
        private set

    private constructor() {

    }

    constructor(uid: Int, gender: Int, age: Int, height: Int, weight: Int, alias: String, type: Int) {
        this.uid = uid
        this.gender = gender.toByte()
        this.age = age.toByte()
        mHeight = (height and 0xFF).toByte()
        mWeight = weight.toByte()
        this.alias = alias
        this.type = type.toByte()
    }

    fun getBytes(mBTAddress: String): ByteArray {
        var aliasBytes: ByteArray
        try {
            aliasBytes = this.alias.toByteArray(charset("UTF-8"))
        } catch (e: UnsupportedEncodingException) {
            aliasBytes = ByteArray(0)
        }

        val bf = ByteBuffer.allocate(20)
        bf.put((uid and 0xff).toByte())
        bf.put((uid shr 8 and 0xff).toByte())
        bf.put((uid shr 16 and 0xff).toByte())
        bf.put((uid shr 24 and 0xff).toByte())
        bf.put(this.gender)
        bf.put(this.age)
        bf.put(this.mHeight)
        bf.put(this.mWeight)
        bf.put(this.type)
        bf.put(4.toByte())
        bf.put(0.toByte())

        if (aliasBytes.size <= 8) {
            bf.put(aliasBytes)
            bf.put(ByteArray(8 - aliasBytes.size))
        } else {
            bf.put(aliasBytes, 0, 8)
        }

        val crcSequence = ByteArray(19)
        for (u in crcSequence.indices)
            crcSequence[u] = bf.array()[u]

        val crcb = (getCRC8(crcSequence) xor Integer.parseInt(mBTAddress.substring(mBTAddress.length - 2), 16) and 0xff).toByte()
        bf.put(crcb)
        return bf.array()
    }

    private fun getCRC8(seq: ByteArray): Int {
        var len = seq.size
        var i = 0
        var crc: Byte = 0x00

        while (len-- > 0) {
            var extract = seq[i++]
            for (tempI in 8 downTo 1) {
                var sum = (crc.toInt() and 0xff xor (extract.toInt() and 0xff)).toByte()
                sum = (sum.toInt() and 0xff and 0x01).toByte()
                crc = (crc.toInt() and 0xff).ushr(1).toByte()
                if (sum.toInt() != 0) {
                    crc = (crc.toInt() and 0xff xor 0x8c).toByte()
                }
                extract = (extract.toInt() and 0xff).ushr(1).toByte()
            }
        }
        return (crc.toInt() and 0xff)
    }

    override fun toString(): String {
        return "uid:" + uid +
                ",gender:" + gender +
                ",age:" + age +
                ",height:" + height +
                ",weight:" + weight +
                ",alias:" + alias +
                ",type:" + type
    }

    /**
     * Gets height in cm

     * @return Height in cm
     */
    val height: Int
        get() = mHeight.toInt() and 0xFF

    /**
     * Gets weight in kg

     * @return Weight in kg
     */
    val weight: Int
        get() = mWeight.toInt() and 0xFF

    companion object {

        /**
         * Creates an instance of user info from byte data

         * @param data Byte data
         * *
         * @return User info object or null, if data are invalid
         */
        fun fromByteData(data: ByteArray): UserInfo? {
            if (data.size < 20) {
                return null
            }
            val info = UserInfo()

            info.uid = data[3].toInt() shl 24 or (data[2].toInt() and 0xFF shl 16) or (data[1].toInt() and 0xFF shl 8) or (data[0].toInt() and 0xFF)
            info.gender = data[4]
            info.age = data[5]
            info.mHeight = data[6]
            info.mWeight = data[7]
            info.type = data[8]
            try {
                info.alias = String(data, 9, 8, Charset.forName("UTF-8"))
            } catch (e: UnsupportedEncodingException) {
                info.alias = ""
            }

            return info
        }
    }
}
