package com.example.ble_example

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val REQUEST_BLUETOOTH_PERMISSIONS = 123
    private val REQUEST_ENABLE_BLUETOOTH = 456

    private val bluetoothManager: BluetoothManager by lazy {
        getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    MainScreen()
                }
            }
        }

        // Request Bluetooth permissions when the activity is created
        requestBluetoothPermissions()
    }

    private fun requestBluetoothPermissions() {
        val bluetoothPermission = Manifest.permission.BLUETOOTH
        val bluetoothAdminPermission = Manifest.permission.BLUETOOTH_ADMIN

        if (ContextCompat.checkSelfPermission(
                this,
                bluetoothPermission
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                bluetoothAdminPermission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(bluetoothPermission, bluetoothAdminPermission),
                REQUEST_BLUETOOTH_PERMISSIONS
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Bluetooth permissions granted, proceed with Bluetooth operations
                Log.d("Bluetooth", "Bluetooth permissions granted")
            } else {
                // Permission denied, handle accordingly (e.g., show a message to the user)
                Log.d("Bluetooth", "Bluetooth permissions denied")
            }
        }
    }

    @Composable
    fun MainScreen() {
        // State to track whether the button is clicked
        var isButtonClicked by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(shape = CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Button to request permissions and show the list
            Button(onClick = {
                isButtonClicked = true
                requestBluetoothPermissions()
            }) {
                Text("Request Permissions and Show List")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // List of bonded Bluetooth devices
            if (isButtonClicked && checkBluetoothPermissions()) {
                LazyColumn {
                    val devicesList = enableBluetoothAndGetBondedDevices()

                    items(devicesList) { device ->
                        BluetoothDeviceListItem(context = this@MainActivity, device = device)
                    }
                }
            } else {
                // Display a message or take appropriate action when Bluetooth permissions are not granted
                Text("Bluetooth permissions not granted")
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun BluetoothDeviceListItem(context: Context, device: BluetoothDevice) {
        ListItem(
            modifier = Modifier.clickable {},
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colors.primary
                )
            },
            text = {
                if (checkBluetoothPermissions()) {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return@ListItem
                    }
                    Text(
                        text = device.name ?: "Unknown Device",
                        style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        )
    }

    // Check Bluetooth permissions
    private fun checkBluetoothPermissions(): Boolean {
        val bluetoothPermission = Manifest.permission.BLUETOOTH
        val bluetoothAdminPermission = Manifest.permission.BLUETOOTH_ADMIN

        val bluetoothPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            bluetoothPermission
        ) == PackageManager.PERMISSION_GRANTED

        val bluetoothAdminPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            bluetoothAdminPermission
        ) == PackageManager.PERMISSION_GRANTED

        return bluetoothPermissionGranted && bluetoothAdminPermissionGranted
    }

    private fun enableBluetoothAndGetBondedDevices(): List<BluetoothDevice> {
        if (checkBluetoothPermissions()) {
            // Check if Bluetooth is enabled, if not, request user to enable it
            if (!bluetoothAdapter?.isEnabled!!) {
                requestBluetoothEnable()
                // Return an empty list for now, the actual list will be populated after enabling Bluetooth
                return emptyList()
            }

            // Bluetooth is already enabled, return the list of bonded devices
            return getBondedDevices()
        } else {
            // Bluetooth permissions not granted, request permissions
            requestBluetoothPermissions()
        }

        // Return an empty list if permissions are not granted yet
        return emptyList()
    }

    private fun getBondedDevices(): List<BluetoothDevice> {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request Bluetooth permissions if not granted
            requestBluetoothPermissions()
            // Return an empty list for now, as permissions need to be granted
            return emptyList()
        }

        // Permissions are granted, return the list of bonded devices
        return BluetoothAdapter.getDefaultAdapter()?.bondedDevices?.toList() ?: emptyList()
    }

    private fun requestBluetoothEnable() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request Bluetooth permissions if not granted
            requestBluetoothPermissions()
            // Return without starting the enable Bluetooth intent, as permissions need to be granted
            return
        }

        // Permissions are granted, proceed with enabling Bluetooth
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH)
    }
}



@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Composable
fun MainUserScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Centered Image at the top of the screen
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(shape = CircleShape),
            contentScale = ContentScale.Crop
        )

        // Spacer with 16dp height for separation
        Spacer(modifier = Modifier.height(16.dp))

        // List below the image
        LazyColumn {
            items((1..10).toList()) { index ->
                ListUserItem(index)
            }
        }
    }
}

@Composable
fun ListUserItem(index: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { /* Handle item click */ }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Icon
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "list icon",
            modifier = Modifier.size(40.dp),
            tint = Color.Gray
        )

        // Text
        Text(
            text = "Device $index Pair?",
            style = MaterialTheme
                .typography
                .subtitle1
                .copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MainUserScreen()
}