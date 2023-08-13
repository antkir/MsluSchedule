package by.ntnk.msluschedule.network.dns

import okio.Buffer
import org.threeten.bp.Instant
import timber.log.Timber
import java.io.Serializable
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.charset.Charset
import kotlin.random.Random

data class Record(val inetAddress: InetAddress, val epochSecond: Long, val ttl: Int) : Serializable

/**
 * Trivial DNS Encoder/Decoder.
 */
object DnsRecordCodec {

    enum class Type(val type: Int) {
        A(0x0001),
        AAAA(0x001C)
    }

    @Suppress("UNUSED")
    private enum class ResponseCode(val code: Int) {
        NOERROR(0x0),
        SERVFAIL(0x2),
        NXDOMAIN(0x3),
        REFUSED(0x5)
    }

    private const val CLASS_IN = 0x0001

    @Throws(Exception::class)
    fun encodeDnsQuery(host: String, type: Type): ByteArray {
        Timber.v("Encoding DNS request.")

        val buffer = Buffer()

        // Header
        val queryId = Random.nextBits(16).toShort()
        Timber.v("Header: Query id: $queryId")
        buffer.writeShort(queryId.toInt()) // Id
        buffer.writeShort(0b00000001_00000000) // Flags with Recursion Desired bit set
        buffer.writeShort(1) // Question count
        buffer.writeShort(0) // Answer count
        buffer.writeShort(0) // Authority record count
        buffer.writeShort(0) // Additional record count

        // Questions
        val labels = host.split('.')
            .dropLastWhile { label -> label.isEmpty() }
            .toTypedArray()
        for (label in labels) {
            val labelByteArray = label.toByteArray()
            buffer.writeByte(labelByteArray.size)
            buffer.write(labelByteArray)
        }
        buffer.writeByte(0) // Domain name termination
        buffer.writeShort(type.type) // Type
        buffer.writeShort(CLASS_IN) // Class

        return buffer.readByteArray()
    }

    @Throws(Exception::class)
    fun decodeDnsAnswers(byteArray: ByteArray): List<Record> {
        val records = mutableListOf<Record>()

        val buffer = Buffer()
        buffer.write(byteArray)

        // Header
        val queryId = buffer.readShort()
        Timber.v("Header: Query id: $queryId")
        val flags = buffer.readShort()
        if (flags.toUShort().toInt() ushr 15 != 1) {
            throw UnknownHostException("Header: QR bit must be 1!")
        }
        Timber.v("Header: Flags: ${flags.toUShort().toString(2)}")
        val responseCode = flags.toUShort().toInt() and 0xF
        if (responseCode != ResponseCode.NOERROR.code) {
            throw UnknownHostException("Header: Incorrect response code!")
        }
        val questionCount = buffer.readShort()
        Timber.v("Header: Question count: $questionCount")
        val answerCount = buffer.readShort()
        Timber.v("Header: Answer count: $answerCount")
        val authorityRecordCount = buffer.readShort()
        Timber.v("Header: Authority record count: $authorityRecordCount")
        val additionalRecordCount = buffer.readShort()
        Timber.v("Header: Additional record count: $additionalRecordCount")

        // Questions
        for (i in 0 until questionCount) {
            val host = parseHost(buffer, byteArray)
            Timber.v("Question: Name: $host")
            val type = buffer.readShort()
            Timber.v("Question: Type: $type")
            val dataClass = buffer.readShort()
            Timber.v("Question: Class: $dataClass")
        }

        // Answers
        for (i in 0 until answerCount) {
            val host = parseHost(buffer, byteArray)
            Timber.v("Answer: Name: $host")
            val type = buffer.readShort()
            Timber.v("Answer: Type: $type")
            val dataClass = buffer.readShort()
            Timber.v("Answer: Class: $dataClass")
            val ttl = buffer.readInt()
            Timber.v("Answer: TTL: $ttl")
            val dataLength = buffer.readShort()
            Timber.v("Answer: RD length: $dataLength")

            when (type.toInt()) {
                Type.A.type -> {
                    val data = mutableListOf<Int>()
                    for (j in 0 until dataLength) {
                        data.add(buffer.readByte().toUByte().toInt())
                    }
                    val ipBuilder = StringBuilder()
                    for (octet in data) {
                        ipBuilder.append(octet).append('.')
                    }
                    if (ipBuilder.isNotEmpty()) ipBuilder.deleteCharAt(ipBuilder.lastIndex)
                    Timber.v("$host : $ipBuilder")

                    val record = Record(InetAddress.getByName(ipBuilder.toString()), Instant.now().epochSecond, ttl / 2)
                    records.add(record)
                }

                Type.AAAA.type -> {
                    val data = mutableListOf<Int>()
                    for (j in 0 until dataLength) {
                        data.add(buffer.readByte().toUByte().toInt())
                    }

                    val ipBuilder = StringBuilder()
                    if (!isEmbeddedIPv4inIPv6Address(data)) {
                        var index = 0
                        while (index < data.size) {
                            val hextet = (data[index] shl 8) or data[index + 1]
                            ipBuilder.append(hextet).append(':')
                            index += 2
                        }
                    } else {
                        for (index in data.size - 4..data.size) {
                            ipBuilder.append(data[index]).append('.')
                        }
                    }
                    if (ipBuilder.isNotEmpty()) ipBuilder.deleteCharAt(ipBuilder.lastIndex)
                    Timber.v("$host : $ipBuilder")

                    val record = Record(InetAddress.getByName(ipBuilder.toString()), Instant.now().epochSecond, ttl / 2)
                    records.add(record)
                }

                else -> {}
            }
        }

        return records
    }

    private fun parseHost(buffer: Buffer, data: ByteArray): String {
        val nameBuilder = StringBuilder()
        var firstLabelByte: Byte
        while (buffer.readByte().also { firstLabelByte = it }.toInt() != 0) {
            val isPointer = ((firstLabelByte.toInt() ushr 6) and 0b11) == 0b11
            if (isPointer) {
                val secondLabelByte = buffer.readByte()
                var offset = ((firstLabelByte.toInt() and 0b00111111) shl 8) or secondLabelByte.toInt()

                while (data[offset].toInt() != 0) {
                    val labelLength = data[offset].toInt()
                    val labelByteArray = ByteArray(labelLength)
                        .apply { forEachIndexed { index, _ -> set(index, data[++offset]) } }
                    nameBuilder.append(labelByteArray.toString(Charset.defaultCharset())).append('.')
                    ++offset
                }
                break
            } else {
                val labelLength = firstLabelByte.toInt()
                val labelByteArray = ByteArray(labelLength)
                    .apply { forEachIndexed { index, _ -> set(index, buffer.readByte()) } }
                nameBuilder.append(labelByteArray.toString(Charset.defaultCharset())).append('.')
            }
        }
        if (nameBuilder.isNotEmpty()) nameBuilder.deleteCharAt(nameBuilder.lastIndex)
        return nameBuilder.toString()
    }

    private fun isEmbeddedIPv4inIPv6Address(data: List<Int>): Boolean {
        var index = 0
        while (index + 6 < data.size) {
            if (data[index].toUByte().toInt() != 0) {
                return false
            }
            index++
        }
        return true
    }
}
