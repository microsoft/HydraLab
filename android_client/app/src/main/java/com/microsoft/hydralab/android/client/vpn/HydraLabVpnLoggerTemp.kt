package com.microsoft.hydralab.android.client.vpn

import android.os.Environment
import com.microsoft.hydralab.android.client.vpn.protocol.Packet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.IOException
import kotlin.io.path.Path

class HydraLabVpnLoggerTemp(private var filePath: String?) {
    private var lines = mutableListOf<String>()
    private var linesStaging = listOf<String>()


    fun stringify(packet: Packet): String {
        return packet.toString()
    }

    fun log(packet: Packet) {
        log(stringify(packet))
    }

    private fun log(line: String) {

    }

    fun flush() {

    }
}
