package by.ntnk.msluschedule.network.dns

import okhttp3.Dns
import okhttp3.internal.publicsuffix.PublicSuffixDatabase
import org.threeten.bp.Instant
import timber.log.Timber
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet6Address
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * [DNS over UDP implementation].
 */
class MsluDns(private val url: String, private val cacheDir: File, private val includeIPv6: Boolean) : Dns {

    @Throws(UnknownHostException::class)
    override fun lookup(hostname: String): List<InetAddress> {
        val inetAddresses = mutableListOf<InetAddress>()

        // System
        inetAddresses.addAll(getInetAddressesSystem(hostname))
        if (inetAddresses.isNotEmpty()) {
            return inetAddresses
        }

        // MSLU
        try {
            val dnsAddress = InetAddress.getByName(url)
            inetAddresses.addAll(getInetAddressesMslu(hostname, dnsAddress))
        } catch (e: Exception) {
            if (e is UnknownHostException) {
                throw e
            } else {
                throw UnknownHostException(hostname).apply { initCause(e) }
            }
        }

        return inetAddresses
    }

    private fun getInetAddressesSystem(hostname: String): List<InetAddress> {
        val inetAddresses = mutableListOf<InetAddress>()
        val unfilteredInetAddresses = mutableListOf<InetAddress>()

        try {
            unfilteredInetAddresses.addAll(InetAddress.getAllByName(hostname))
        } catch (e: Exception) {
            Timber.w(e, "System DNS failed to resolve a host!")
        }

        for (inetAddress in unfilteredInetAddresses) {
            Timber.v("System DNS result: $inetAddress)")

            if (inetAddress is Inet6Address && !includeIPv6) {
                continue
            }

            if (!isPrivateHost(hostname) && !inetAddress.isSiteLocalAddress) {
                inetAddresses.add(inetAddress)
            }
        }

        return inetAddresses
    }

    @Throws(Exception::class)
    private fun getInetAddressesMslu(hostname: String, dnsAddress: InetAddress): List<InetAddress> {
        val records = mutableListOf<Record>()
        val unfilteredRecords = mutableListOf<Record>()

        records.addAll(readCache())
        if (records.isNotEmpty()) {
            return records.map { record -> record.inetAddress }
        }

        val socket = DatagramSocket().apply {
            soTimeout = DNS_SOCKET_TIMEOUT
        }

        var query = 0
        while (query < DNS_QUERY_LIMIT) {
            try {
                if (includeIPv6) {
                    val recordsIPv6 = resolveHost(socket, hostname, DnsRecordCodec.Type.AAAA, dnsAddress)
                    unfilteredRecords.addAll(recordsIPv6)
                }

                val recordsIPv4 = resolveHost(socket, hostname, DnsRecordCodec.Type.A, dnsAddress)
                unfilteredRecords.addAll(recordsIPv4)
            } catch (e: Exception) {
                if (e is SocketTimeoutException) {
                    Timber.v(e)
                } else {
                    throw e
                }
            }

            if (unfilteredRecords.isNotEmpty()) {
                break
            }

            socket.soTimeout = (socket.soTimeout * 1.5f).toInt()
            ++query
        }

        socket.close()

        for (record in unfilteredRecords) {
            Timber.v("MSLU DNS result: ${record.inetAddress})")

            if (!isPrivateHost(hostname) && !record.inetAddress.isSiteLocalAddress) {
                records.add(record)
            }
        }

        writeCache(records)

        return records.map { record -> record.inetAddress }
    }

    private fun readCache(): List<Record> {
        val records = mutableListOf<Record>()
        val cacheFile = File(cacheDir, DNS_CACHE_FILE)
        if (cacheFile.length() != 0L) {
            var objectInputStream: ObjectInputStream? = null
            try {
                objectInputStream = ObjectInputStream(cacheFile.inputStream())

                @Suppress("UNCHECKED_CAST")
                val cachedRecords = (objectInputStream.readObject() as ArrayList<Record>)
                    .filter { cachedRecord ->
                        cachedRecord.epochSecond < Instant.now().epochSecond &&
                            cachedRecord.epochSecond + cachedRecord.ttl > Instant.now().epochSecond
                    }
                Timber.v("Read DNS cache: $cachedRecords")
                records.addAll(cachedRecords)
            } catch (e: Exception) {
                Timber.w(e)
            } finally {
                objectInputStream?.close()
            }
        }
        return records
    }

    private fun writeCache(records: List<Record>) {
        val cacheFile = File(cacheDir, DNS_CACHE_FILE)
        var objectOutputStream: ObjectOutputStream? = null
        try {
            objectOutputStream = ObjectOutputStream(cacheFile.outputStream())
            Timber.v("Write DNS cache: $records")
            objectOutputStream.writeObject(records)
            objectOutputStream.flush()
        } catch (e: Exception) {
            Timber.w(e)
        } finally {
            objectOutputStream?.close()
        }
    }

    @Throws(Exception::class)
    private fun resolveHost(
        socket: DatagramSocket,
        hostname: String,
        type: DnsRecordCodec.Type,
        dnsAddress: InetAddress
    ): List<Record> {
        val records = mutableListOf<Record>()

        val query = DnsRecordCodec.encodeDnsQuery(hostname, type)
        sendDnsRequest(socket, query, dnsAddress)

        val byteArray = receiveDnsResponse(socket)
        records.addAll(DnsRecordCodec.decodeDnsAnswers(byteArray))

        return records
    }

    @Throws(Exception::class)
    private fun sendDnsRequest(socket: DatagramSocket, frame: ByteArray, dnsAddress: InetAddress) {
        val dnsPacket = DatagramPacket(frame, frame.size, dnsAddress, DNS_PORT)
        Timber.v("Sending DNS request.")
        socket.send(dnsPacket)
    }

    @Throws(Exception::class)
    private fun receiveDnsResponse(socket: DatagramSocket): ByteArray {
        val response = ByteArray(1232)
        val packet = DatagramPacket(response, response.size)
        socket.receive(packet)
        Timber.v("Received DNS response.")
        return packet.data
    }

    class Builder {
        private var url: String? = null
        private var cacheDir: File? = null
        private var includeIPv6: Boolean = true

        fun build(): MsluDns {
            return MsluDns(
                checkNotNull(url) { "Url is not set!" },
                checkNotNull(cacheDir) { "Cache dir is not set!" },
                includeIPv6
            )
        }

        fun url(url: String) = apply {
            this.url = url
        }

        fun cacheDir(cacheDir: File) = apply {
            this.cacheDir = cacheDir
        }

        fun includeIPv6(includeIPv6: Boolean) = apply {
            this.includeIPv6 = includeIPv6
        }
    }

    private companion object {
        private const val DNS_PORT = 53
        private const val DNS_QUERY_LIMIT = 5
        private const val DNS_SOCKET_TIMEOUT = 1000 // 1 second
        private const val DNS_CACHE_FILE = "dns"

        private fun isPrivateHost(host: String): Boolean {
            return PublicSuffixDatabase.get().getEffectiveTldPlusOne(host) == null
        }
    }
}
