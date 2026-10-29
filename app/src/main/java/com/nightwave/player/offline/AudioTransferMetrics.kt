package com.nightwave.player.offline

import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Protocol
import okhttp3.Response
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

/** Numbers only. Never retains a request, URL, IP, validator or account. */
internal class AudioTransferMetrics {
    @Volatile var requests = 0
    @Volatile var wireBytes = 0L
    @Volatile var readNanos = 0L
    @Volatile var writeNanos = 0L
    @Volatile var syncNanos = 0L
    @Volatile var resumedBytes = 0L
    @Volatile var mode = "Checking resume support"
    var finished: (Call) -> Unit = {}
    @Synchronized fun addReceived(n: Long) { wireBytes += n }
    @Synchronized fun addRead(n: Long) { readNanos += n }
    @Synchronized fun addWrite(n: Long) { writeNanos += n }
    @Synchronized fun addSync(n: Long) { syncNanos += n }
    /** Per-call clocks avoid interleaving DNS/TLS/header phases across sockets. */
    fun listener(): EventListener = object : EventListener() {
        private var dnsAt = 0L
        private var connectAt = 0L
        private var tlsAt = 0L
        private var headersAt = 0L
        override fun callStart(call: Call) { synchronized(this@AudioTransferMetrics) { requests++ } }
        override fun dnsStart(call: Call, domainName: String) { dnsAt = System.nanoTime() }
        override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) {
            synchronized(this@AudioTransferMetrics) { dnsNanos += System.nanoTime() - dnsAt }
        }
        override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) { connectAt = System.nanoTime() }
        override fun connectEnd(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy, protocol: Protocol?) {
            synchronized(this@AudioTransferMetrics) { connectNanos += System.nanoTime() - connectAt }
        }
        override fun secureConnectStart(call: Call) { tlsAt = System.nanoTime() }
        override fun secureConnectEnd(call: Call, handshake: okhttp3.Handshake?) {
            synchronized(this@AudioTransferMetrics) { tlsNanos += System.nanoTime() - tlsAt }
        }
        override fun requestHeadersEnd(call: Call, request: okhttp3.Request) { headersAt = System.nanoTime() }
        override fun responseHeadersStart(call: Call) {
            if (headersAt > 0) synchronized(this@AudioTransferMetrics) { headerNanos += System.nanoTime() - headersAt }
        }
        override fun responseHeadersEnd(call: Call, response: Response) {
            synchronized(this@AudioTransferMetrics) {
                status = response.code
                protocol = when (response.protocol) {
                    Protocol.HTTP_2 -> "HTTP/2"
                    Protocol.HTTP_1_1 -> "HTTP/1.1"
                    else -> "HTTP"
                }
            }
        }
        override fun callEnd(call: Call) { finished(call) }
        override fun callFailed(call: Call, ioe: java.io.IOException) { finished(call) }
    }
    private var dnsNanos = 0L
    private var connectNanos = 0L
    private var tlsNanos = 0L
    private var headerNanos = 0L
    private var status = 0
    private var protocol = "pending"
    private val startedAt = System.nanoTime()
    @Synchronized fun summary(): String {
        fun ms(n: Long) = TimeUnit.NANOSECONDS.toMillis(n.coerceAtLeast(0))
        return "$mode · $protocol · HTTP $status · $requests requests\n" +
            "Received $wireBytes bytes · Retained start $resumedBytes bytes\n" +
            "Elapsed ${ms(System.nanoTime() - startedAt)} ms · Receive is summed across concurrent calls\n" +
            "DNS ${ms(dnsNanos)} ms · Connect ${ms(connectNanos)} ms · TLS ${ms(tlsNanos)} ms\n" +
            "Headers ${ms(headerNanos)} ms · Receive ${ms(readNanos)} ms · " +
            "Write ${ms(writeNanos)} ms · Sync ${ms(syncNanos)} ms\n" +
            "Cumulative phases; connect includes TLS. Reused connections can show 0 ms. " +
            "File verification/import is separate. 1 Mbps = 125,000 bytes/s."
    }
}
