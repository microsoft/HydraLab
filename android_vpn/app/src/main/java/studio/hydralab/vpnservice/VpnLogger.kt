package studio.hydralab.vpnservice

import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import studio.hydralab.vpnservice.protocol.Packet
import java.io.File
import java.io.FileWriter
import java.io.IOException

class VpnLogger(private var filePath: String?) {
    private var lines = mutableListOf<String>()
    private var linesStaging = listOf<String>()

    init {
        if (filePath != null) {
            filePath = Environment.getExternalStorageDirectory().toString() + filePath
            val file = File(filePath)
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
