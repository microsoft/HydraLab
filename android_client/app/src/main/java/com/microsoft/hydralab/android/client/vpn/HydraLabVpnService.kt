package com.microsoft.hydralab.android.client.vpn

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.microsoft.hydralab.android.client.MainActivity

data class VpnUserConfig(
    val apps: List<String>,
    val dumpPath: String,
    val dnsServer: String
)

var isVpnServiceRunning  = false

class HydraLabVpnService : VpnService() {

    private val mConfigureIntent: PendingIntent by lazy {
        var activityFlag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activityFlag += PendingIntent.FLAG_MUTABLE
        }
        PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), activityFlag)
    }

    private lateinit var vpnInterface: ParcelFileDescriptor

    private lateinit var vpnLogger: HydraLabVpnLogger

    override fun onCreate() {
        UdpSendWorker.start(this)
        UdpReceiveWorker.start(this)
        UdpSocketCleanWorker.start()
        TcpWorker.start(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return if (intent?.action != "com.microsoft.hydralab.android.client.vpn.START") {
            disconnect()
            START_NOT_STICKY
        } else {
            val config = VpnUserConfig(
                intent?.extras?.getString("apps")?.split("-") ?: emptyList(),
                intent?.extras?.getString("output")?:"/Documents/dump.log",
                intent?.extras?.getString("dns")?:"114.114.114.114"
            )
            connect(config)
            START_STICKY
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
        UdpSendWorker.stop()
        UdpReceiveWorker.stop()
        UdpSocketCleanWorker.stop()
        TcpWorker.stop()
    }

    private fun connect(config: VpnUserConfig) {
        vpnInterface = createVpnInterface(config)
        vpnLogger = HydraLabVpnLogger(config.dumpPath)
        val fileDescriptor = vpnInterface.fileDescriptor
        ToNetworkQueueWorker.start(fileDescriptor, vpnLogger)
        ToDeviceQueueWorker.start(fileDescriptor, vpnLogger)
        isVpnServiceRunning = true
    }

    private fun disconnect() {
        ToNetworkQueueWorker.stop()
        ToDeviceQueueWorker.stop()
        vpnInterface.close()
        isVpnServiceRunning = false
        stopForeground(true)
        System.gc()
    }

    private fun createVpnInterface(config: VpnUserConfig): ParcelFileDescriptor {
        val builder = Builder()
        config.apps.forEach {
            builder.addAllowedApplication(it)
        }

        return builder
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer(config.dnsServer)
            .setSession("VPN-Demo")
            .setBlocking(true)
            .setConfigureIntent(mConfigureIntent)
            .also {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    it.setMetered(false)
                }
            }
            .establish() ?: throw IllegalStateException("Init vpnInterface failed")
    }
}