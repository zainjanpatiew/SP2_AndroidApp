package com.example.ermes.utils

import android.text.TextUtils
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.Arrays

/**
 * Description:
 * Created by bruce on 2019/4/1.
 */
object ByteUtils {
    /**
     * 合并字节数组
     *
     * @param values
     * @return
     */
    fun byteMergerAll(vararg values: ByteArray): ByteArray {
        var length_byte = 0
        for (i in values.indices) {
            length_byte += values[i].size
        }
        val all_byte = ByteArray(length_byte)
        var countLength = 0
        for (i in values.indices) {
            val b = values[i]
            System.arraycopy(b, 0, all_byte, countLength, b.size)
            countLength += b.size
        }
        return all_byte
    }

    fun subBytes(src: ByteArray?, begin: Int, count: Int): ByteArray {
        val bs = ByteArray(count)
        System.arraycopy(src, begin, bs, 0, count)
        return bs
    }

    //byte[]字节 转化string 的16进制
    fun bytesToHex(bytes: ByteArray): String {
        val buf = StringBuilder(bytes.size * 2)
        for (b in bytes) { // 使用String的format方法进行转换
            buf.append(String.format("%02x", b.toInt() and 0xff))
        }
        return buf.toString()
    }

    /**
     * 将16进制字符串转换为byte[]
     *
     * @param str
     * @return
     */
    fun hexToBytes(str: String?): ByteArray {
        if (str == null || str.trim { it <= ' ' } == "") {
            return ByteArray(0)
        }
        val bytes = ByteArray(str.length / 2)
        for (i in 0 until str.length / 2) {
            val subStr = str.substring(i * 2, i * 2 + 2)
            bytes[i] = subStr.toInt(16).toByte()
        }
        return bytes
    }

    /**
     * 整数转换为2字节的byte数组，低位到高位
     */
    fun intToByte2L(i: Int): ByteArray {
        val targets = ByteArray(2)
        targets[0] = (i shr 8 and 0xFF).toByte()
        targets[1] = (i and 0xFF).toByte()
        return targets
    }

    fun byte2String(bytes: ByteArray): String {
        val cs = Charset.forName("UTF-8")
        val bb = ByteBuffer.allocate(bytes.size)
        bb.put(bytes)
        bb.flip()
        val cb = cs.decode(bb)
        return cb.toString()
    }

    fun String2Byte64(message: String): ByteArray {
        val byte64 = ByteArray(64)
        Arrays.fill(byte64, 0.toByte())
        if (TextUtils.isEmpty(message)) {
            return byte64
        }
        val len = message.length
        //        byte[] bytes = new byte[len];
        val chars = message.toCharArray()
        for (i in 0 until len) {
            byte64[i] = chars[i].code.toByte()
        }
        return byte64
    }

    val noneByte64: ByteArray
        get() {
            val byte64 = ByteArray(64)
            Arrays.fill(byte64, 0.toByte())
            return byte64
        }

    /**
     * int整数转换为4字节的byte数组
     *
     * @param i
     * @return byte
     */
    fun intToByte2(i: Int): ByteArray {
        val targets = ByteArray(2)
        targets[1] = (i and 0xFF).toByte()
        targets[0] = (i shr 8 and 0xFF).toByte()
        return targets
    }

    fun byte2ToInt(bytes: ByteArray): Int {
        val b0 = bytes[0].toInt() and 0xFF
        val b1 = bytes[1].toInt() and 0xFF
        return b1 shl 8 or b0
    }

    /**
     * int整数转换为4字节的byte数组
     *
     * @param i
     * @return byte
     */
    fun intToByte4(i: Int): ByteArray {
        val targets = ByteArray(4)
        targets[3] = (i and 0xFF).toByte()
        targets[2] = (i shr 8 and 0xFF).toByte()
        targets[1] = (i shr 16 and 0xFF).toByte()
        targets[0] = (i shr 24 and 0xFF).toByte()
        return targets
    }

    fun byte4ToInt(bytes: ByteArray): Int {
        val b0 = bytes[0].toInt() and 0xFF
        val b1 = bytes[1].toInt() and 0xFF
        val b2 = bytes[2].toInt() and 0xFF
        val b3 = bytes[3].toInt() and 0xFF
        return b0 shl 24 or (b1 shl 16) or (b2 shl 8) or b3
    }

    fun byte4ToIntL(bytes: ByteArray): Int {
        val b0 = bytes[3].toInt() and 0xFF
        val b1 = bytes[2].toInt() and 0xFF
        val b2 = bytes[1].toInt() and 0xFF
        val b3 = bytes[0].toInt() and 0xFF
        return b0 shl 24 or (b1 shl 16) or (b2 shl 8) or b3
    }

    fun FloatArrayToByteArray(data: FloatArray): ByteArray {
        var Resutl = byteArrayOf()
        for (i in data.indices) {
            val intToBytes4 = intToByte4(
                java.lang.Float.floatToIntBits(
                    data[i]
                )
            )
            Resutl = concat(Resutl, intToBytes4)
        }
        return Resutl
    }

    fun concat(a: ByteArray, b: ByteArray): ByteArray {
        val c = ByteArray(a.size + b.size)
        System.arraycopy(a, 0, c, 0, a.size)
        System.arraycopy(b, 0, c, a.size, b.size)
        return c
    }

    fun ByteArrayToFloatArray(data: ByteArray): FloatArray {
        val result = FloatArray(data.size / 4)
        var temp = 0
        var i = 0
        while (i < data.size) {
            temp = temp or (data[i].toInt() and 0xff shl 0)
            temp = temp or (data[i + 1].toInt() and 0xff shl 8)
            temp = temp or (data[i + 2].toInt() and 0xff shl 16)
            temp = temp or (data[i + 3].toInt() and 0xff shl 24)
            result[i / 4] = java.lang.Float.intBitsToFloat(temp)
            i += 4
        }
        return result
    }
}
