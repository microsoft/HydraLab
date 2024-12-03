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

class HydraLabVpnLogger(private var filePath: String?) {
    private var lines = mutableListOf<String>()
    private var linesStaging = listOf<String>()

    init {
        if (filePath != null) {
            filePath = Environment.getExternalStorageDirectory().toString() + filePath

            val publicFolder = Path(Environment.getExternalStorageDirectory().toString()).toString()
            val fileUrl = Path(filePath.toString()).toString()
            if (!fileUrl.startsWith(publicFolder + File.separator)) {
                throw IllegalArgumentException("Invalid file path")
            }

            val file = File(filePath ?: "")
            if (file.exists()) {
                file.writeText("")
            } else {
                file.createNewFile()
            }
        }
    }

    fun stringify(packet: Packet): String {
        return packet.toString()
    }

    fun log(packet: Packet) {
        log(stringify(packet))
    }

    private fun log(line: String) {
        lines.add(line)
        if (lines.size >= 100) {
            flush()
        }
    }

    fun flush() {
        linesStaging = lines.toList()
        lines.clear()
        if (filePath == null) {
            return
        }
        GlobalScope.launch(Dispatchers.IO) {
            try {
                FileWriter(filePath, true).use { writer ->
                    for (line in linesStaging) {
                        writer.write(line)
                        writer.write("\n")
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
