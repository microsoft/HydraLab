package studio.hydralab.vpnservice

import android.content.Context
import android.content.Intent
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.*
import studio.hydralab.vpnservice.protocol.Packet
import studio.hydralab.vpnservice.ui.theme.AndroidVpnServiceDemoTheme
import kotlin.coroutines.CoroutineContext


class MainActivity : ComponentActivity(), CoroutineScope {
    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private val job = Job()
    override val coroutineContext: CoroutineContext = Dispatchers.Main + job + CoroutineName("MainActivity")

    private val vpnContent = registerForActivityResult(VpnContent()) {
        if (it) {
            startVpn()
        }
    }

    private var currentHandleAckId by mutableStateOf(0L)
    private var totalInputCount by mutableStateOf(0L)
    private var totalOutputCount by mutableStateOf(0L)

    private val dataUpdater = launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
        while (isActive) {
            if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                currentHandleAckId = Packet.globalPackId.get()
                totalInputCount = ToNetworkQueueWorker.totalInputCount
                totalOutputCount = ToDeviceQueueWorker.totalOutputCount
            }
            delay(16L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidVpnServiceDemoTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Greeting(name = "Vpn Service")
                        Text(text = "AckId:$currentHandleAckId")
                        Text(text = "Send bytes:${ToNetworkQueueWorker.totalInputCount}")
                        Text(text = "Receive bytes:${ToDeviceQueueWorker.totalOutputCount}")
                        Button(onClick = {
                            if (isMyVpnServiceRunning) {
                                stopVpn()
                                dataUpdater.cancel()
                            } else {
                                dataUpdater.start()
                                prepareVpn()
                            }
                        }) {
                            val text = if (isMyVpnServiceRunning) {
                                "Stop"
                            } else {
                                "Start"
                            }
                            Text(text = text)
                        }
                    }
                }
            }
        }
        verifyStoragePermissions(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            if (intent.action == "studio.hydralab.vpnservice.START") {
                val output = intent.extras?.getString("output")
                val appsStr = intent.extras?.getString("apps")
                dataUpdater.start()
                prepareVpn(output, appsStr)
            }
            else if (intent.action == "studio.hydralab.vpnservice.STOP") {
                stopVpn()
                dataUpdater.cancel()
            }
        }
    }

    private fun verifyStoragePermissions(activity: Activity) {
        val permission = ActivityCompat.checkSelfPermission(
            activity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
            )
        }
    }

    private fun prepareVpn(output: String? = null, appsStr: String? = null) {
        VpnService.prepare(this@MainActivity)?.let {
            vpnContent.launch(it)
        } ?: kotlin.run {
            startVpn(output, appsStr)
        }
    }

    private fun startVpn(output: String? = null, appsStr: String? = null) {
        val intent = Intent(this@MainActivity, MyVpnService::class.java)
        intent.putExtra("output", output)
        intent.putExtra("apps", appsStr)
        startService(intent)
    }

    private fun stopVpn() {
        startService(Intent(this@MainActivity, MyVpnService::class.java).also { it.action = MyVpnService.ACTION_DISCONNECT })
    }

    class VpnContent : ActivityResultContract<Intent, Boolean>() {
        override fun createIntent(context: Context, input: Intent): Intent {
            return input
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
            return resultCode == RESULT_OK
        }
    }

    companion object {
        const val TAG = "MainActivity"
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = name)
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AndroidVpnServiceDemoTheme {
        Greeting("Android")
    }
}

