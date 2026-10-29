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
internal class AudioTransferMetrics : EventListener() {
    var requests = 0
    var wireBytes = 0L
    var readNanos = 0L
    var writeNanos = 0L
    var syncNanos = 0L
    var resumedBytes = 0L
    var mode = "Checking resume support"
    private var dnsStart = 0L
    private var connectStart = 0L
    private var tlsStart = 0L
    private var headerStart = 0L
    private var dnsNanos = 0L
    private var connectNanos = 0L
    private var tlsNanos = 0L
    private var headerNanos = 0L
    private var status = 0
    private var protocol = "pending"
    override fun callStart(call: Call) { requests++ }
    override fun dnsStart(call: Call, domainName: String) { dnsStart = System.nanoTime() }
    override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) {
        dnsNanos += System.nanoTime() - dnsStart
    }
    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        connectStart = System.nanoTime()
    }
    override fun connectEnd(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy,
                            protocol: Protocol?) {
        connectNanos += System.nanoTime() - connectStart
    }
    override fun secureConnectStart(call: Call) { tlsStart = System.nanoTime() }
    override fun secureConnectEnd(call: Call, handshake: okhttp3.Handshake?) {
        tlsNanos += System.nanoTime() - tlsStart
    }
    override fun requestHeadersEnd(call: Call, request: okhttp3.Request) {
        headerStart = System.nanoTime()
    }
    override fun responseHeadersStart(call: Call) {
        if (headerStart > 0) headerNanos += System.nanoTime() - headerStart
    }
    override fun responseHeadersEnd(call: Call, response: Response) {
        status = response.code
        protocol = when (response.protocol) {
            Protocol.HTTP_2 -> "HTTP/2"
            Protocol.HTTP_1_1 -> "HTTP/1.1"
            else -> "HTTP"
        }
    }
    fun summary(): String {
        fun ms(n: Long) = TimeUnit.NANOSECONDS.toMillis(n.coerceAtLeast(0))
        return "$mode · $protocol · HTTP $status · $requests requests\n" +
            "Received $wireBytes bytes · Retained start $resumedBytes bytes\n" +
            "DNS ${ms(dnsNanos)} ms · Connect ${ms(connectNanos)} ms · TLS ${ms(tlsNanos)} ms\n" +
            "Headers ${ms(headerNanos)} ms · Receive ${ms(readNanos)} ms · " +
            "Write ${ms(writeNanos)} ms · Sync ${ms(syncNanos)} ms\n" +
            "Cumulative phases; connect includes TLS. Reused connections can show 0 ms. " +
            "File verification/import is separate. 1 Mbps = 125,000 bytes/s."
    }
}
