package com.recovery.companion.blocking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.recovery.companion.MainActivity
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import kotlin.concurrent.thread

/**
 * Local DNS-only VPN filter (plan §4).
 *
 * How it works: the VPN interface's DNS server is our own virtual address
 * (10.0.0.2). Android sends DNS queries through the tunnel; we parse each
 * query, check the domain against the on-device blocklist, and either
 * answer NXDOMAIN (blocked) or forward the query to Cloudflare Family
 * (1.1.1.3) and relay the answer. Only DNS traffic enters the tunnel —
 * all other traffic bypasses it, and the upstream socket is protected
 * with [VpnService.protect] so it never loops back into the tunnel.
 *
 * Honest limits (shown in the app UI, not hidden):
 * - Only one VPN at a time; any other VPN replaces this one.
 * - DNS sees domain names only. It cannot see inside HTTPS, apps, or feeds.
 * - DoH/DoT in a browser, IP-direct access, and brand-new domains bypass it.
 * - The user can disconnect it in seconds. That is why the 24h delay +
 *   trusted-person approval (§ProtectionManager) is a *social* lock, and
 *   why tamper detection only *reports* that protection went off.
 * - IPv6 DNS is not intercepted in this scaffold; the interface has no
 *   IPv6 address. (Phase 1: add an IPv6 DNS server + AAAA handling.)
 */
class RecoveryVpnService : VpnService() {

    private var tun: ParcelFileDescriptor? = null
    private var worker: Thread? = null
    @Volatile private var running = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (running) return START_STICKY
        Blocklist.load(this)
        val builder = Builder()
            .addAddress("10.0.0.2", 32)
            .addDnsServer("10.0.0.2")
            .addRoute("10.0.0.2", 32)
            .setSession("Recovery Companion — DNS filter")
        tun = builder.establish() ?: return START_NOT_STICKY
        startForeground(NOTIFICATION_ID, buildNotification())
        running = true
        isRunning = true
        worker = thread(name = "dns-filter", isDaemon = true) { runLoop() }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        isRunning = false
        worker?.interrupt()
        runCatching { tun?.close() }
        super.onDestroy()
    }

    override fun onRevoke() {
        // The system (or the user, via Settings) revoked the VPN.
        // Record it locally so the app can show "protection was turned off".
        getSharedPreferences("protection", Context.MODE_PRIVATE)
            .edit().putLong("revoked_at", System.currentTimeMillis()).apply()
        stopSelf()
        super.onRevoke()
    }

    private fun runLoop() {
        val fd = tun ?: return
        val input = FileInputStream(fd.fileDescriptor)
        val output = FileOutputStream(fd.fileDescriptor)
        val buffer = ByteArray(32767)
        var lastHeartbeat = 0L
        while (running) {
            try {
                val length = input.read(buffer)
                if (length <= 0) continue
                val now = System.currentTimeMillis()
                if (now - lastHeartbeat > 30_000) {
                    lastHeartbeat = now
                    getSharedPreferences("protection", Context.MODE_PRIVATE)
                        .edit().putLong("last_heartbeat", now).apply()
                }
                val response = handlePacket(buffer.copyOf(length)) ?: continue
                output.write(response)
            } catch (e: Exception) {
                if (!running) break
                try { Thread.sleep(100) } catch (_: InterruptedException) { break }
            }
        }
    }

    /** Returns a full IPv4/UDP packet to write back, or null to drop. */
    private fun handlePacket(packet: ByteArray): ByteArray? {
        if (packet.size < 28) return null
        if ((packet[0].toInt() shr 4) != 4) return null // IPv4 only in this scaffold
        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (packet[9].toInt() and 0xFF != 17) return null // UDP only
        val udpStart = ihl
        val srcPort = u16(packet, udpStart)
        val dstPort = u16(packet, udpStart + 2)
        if (dstPort != 53) return null
        val dnsStart = udpStart + 8
        if (packet.size < dnsStart + 12) return null
        val domain = parseQname(packet, dnsStart) ?: return null
        val dnsQuery = packet.copyOfRange(dnsStart, packet.size)
        val srcIp = packet.copyOfRange(12, 16)

        val dnsResponse: ByteArray = if (Blocklist.isBlocked(domain)) {
            buildNxdomain(dnsQuery)
        } else {
            forwardToUpstream(dnsQuery) ?: return null
        }
        return buildIpUdpPacket(
            srcIpBytes = byteArrayOf(10, 0, 0, 2), srcPort = 53,
            dstIpBytes = srcIp, dstPort = srcPort,
            payload = dnsResponse
        )
    }

    private fun parseQname(packet: ByteArray, dnsStart: Int): String? {
        val labels = StringBuilder()
        var pos = dnsStart + 12
        while (pos < packet.size) {
            val len = packet[pos].toInt() and 0xFF
            if (len == 0) break
            if (len >= 64) return null // compression pointer — not in a query
            if (pos + 1 + len > packet.size) return null
            if (labels.isNotEmpty()) labels.append('.')
            labels.append(packet.copyOfRange(pos + 1, pos + 1 + len).decodeToString())
            pos += 1 + len
        }
        return labels.toString().ifEmpty { null }
    }

    private fun buildNxdomain(query: ByteArray): ByteArray {
        // Copy the question, set flags = response + NXDOMAIN (RCODE 3).
        val out = ByteArray(12 + (query.size - 12))
        query.copyInto(out)
        out[2] = 0x81.toByte()
        out[3] = 0x83.toByte()
        out[4] = 0; out[5] = 1   // QDCOUNT = 1
        out[6] = 0; out[7] = 0   // ANCOUNT = 0
        out[8] = 0; out[9] = 0
        out[10] = 0; out[11] = 0
        return out
    }

    private fun forwardToUpstream(query: ByteArray): ByteArray? {
        return try {
            val socket = DatagramSocket()
            protect(socket)
            socket.soTimeout = 3000
            val upstream = InetAddress.getByName("1.1.1.3") // Cloudflare Family
            socket.send(DatagramPacket(query, query.size, upstream, 53))
            val buf = ByteArray(4096)
            val resp = DatagramPacket(buf, buf.size)
            socket.receive(resp)
            socket.close()
            resp.data.copyOf(resp.length)
        } catch (_: Exception) {
            null
        }
    }

    private fun buildIpUdpPacket(
        srcIpBytes: ByteArray, srcPort: Int,
        dstIpBytes: ByteArray, dstPort: Int,
        payload: ByteArray
    ): ByteArray {
        val totalLen = 20 + 8 + payload.size
        val buf = ByteBuffer.allocate(totalLen)
        buf.put(0x45.toByte()) // version 4, IHL 5
        buf.put(0)             // TOS
        buf.putShort(totalLen.toShort())
        buf.putShort(0)        // ID
        buf.putShort(0)        // flags/fragment
        buf.put(64.toByte())   // TTL
        buf.put(17.toByte())   // UDP
        buf.putShort(0)        // checksum (filled below)
        buf.put(srcIpBytes)
        buf.put(dstIpBytes)
        buf.putShort(srcPort.toShort())
        buf.putShort(dstPort.toShort())
        buf.putShort((8 + payload.size).toShort())
        buf.putShort(0)        // UDP checksum: 0 = optional for IPv4
        buf.put(payload)
        val arr = buf.array()
        val checksum = ipChecksum(arr, 0, 20)
        arr[10] = (checksum shr 8).toByte()
        arr[11] = (checksum and 0xFF).toByte()
        return arr
    }

    private fun ipChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        while (i < offset + length - 1) {
            sum += ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (i < offset + length) sum += (data[i].toInt() and 0xFF) shl 8
        while (sum ushr 16 != 0) sum = (sum and 0xFFFF) + (sum ushr 16)
        return sum.inv() and 0xFFFF
    }

    private fun u16(b: ByteArray, off: Int): Int =
        ((b[off].toInt() and 0xFF) shl 8) or (b[off + 1].toInt() and 0xFF)

    private fun buildNotification(): Notification {
        val channelId = "protection"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(channelId, "الحماية", NotificationManager.IMPORTANCE_LOW)
        )
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, channelId)
            .setContentTitle("رفيق التعافي — الحماية شغالة")
            .setContentText("فلتر DNS شغال على جهازك. إيقافه بيتسجل محليًا.")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1
        @Volatile var isRunning: Boolean = false
            private set
    }
}
