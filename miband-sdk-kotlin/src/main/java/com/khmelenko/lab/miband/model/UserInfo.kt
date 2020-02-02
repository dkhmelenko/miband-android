package com.khmelenko.lab.miband.model

import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * User information

 * @author Dmytro Khmelenko
 */
class UserInfo constructor(private val uid: Int,
                           private val gender: Byte,
                           private val age: Byte,
                           private val height: Byte,
                           private val weight: Byte,
                           private val alias: String,
                           private val type: Byte) {

    fun getBytes(mBTAddress: String): ByteArray {
        val aliasBytes = try {
            this.alias.toByteArray(charset("UTF-8"))
        } catch (e: UnsupportedEncodingException) {
            ByteArray(0)
        }

        val bf = ByteBuffer.allocate(20)
        bf.put((uid and 0xff).toByte())
        bf.put((uid shr 8 and 0xff).toByte())
        bf.put((uid shr 16 and 0xff).toByte())
        bf.put((uid shr 24 and 0xff).toByte())
        bf.put(this.gender)
        bf.put(this.age)
        bf.put(this.height)
        bf.put(this.weight)
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

    companion object {

        /**
         * Creates an instance of user info from byte data

         * @param data Byte data
         * *
         * @return User info object or null, if data are invalid
         */
        fun fromByteData(data: ByteArray): UserInfo {
            val uid = data[3].toInt() shl 24 or (data[2].toInt() and 0xFF shl 16) or (data[1].toInt() and 0xFF shl 8) or (data[0].toInt() and 0xFF)
            val gender = data[4]
            val age = data[5]
            val height = data[6]
            val weight = data[7]
            val type = data[8]
            var alias = ""
            try {
                alias = String(data, 9, 8, Charset.forName("UTF-8"))
            } catch (e: UnsupportedEncodingException) {
            }

            return UserInfo(uid, gender, age, height, weight, alias, type)
        }
    }
}
