package com.example.mappingapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.text.isDigitsOnly
import com.example.mappingapp.ui.theme.MappingAppTheme
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import org.ramani.compose.CameraPosition
import org.ramani.compose.Circle
import org.ramani.compose.MapLibre
import org.ramani.compose.Polygon
import org.ramani.compose.Polyline


class MainActivity : ComponentActivity(), LocationListener {
    val viewModel : GpsViewModel by viewModels()
    val liveLocation = false
    val styleBuilder = Style.Builder().fromUri("https://tiles.openfreemap.org/styles/bright")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startGPS()
        viewModel.let {
            it.latLon = LatLng(50.9079, -1.4015)
        }
        setContent {
            MappingAppTheme {

                val enterLatLonHeight = 40.dp


                Column(modifier=Modifier.fillMaxSize()) {

                    DisplayLatLon()

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

                        DisplayMap(
                            Modifier.align(Alignment.TopCenter)
                                .height(this.maxHeight - enterLatLonHeight)
                        )

                        EnterLatLon(
                            Modifier.height(enterLatLonHeight).align(Alignment.BottomCenter)
                        )
                    }
                }

            }
        }
    }

    @Composable
    fun DisplayMap(modifier: Modifier = Modifier) {

        // Get Lat and on values from view model
        val latLon = remember { mutableStateOf(LatLng(0.0, 0.0)) }
        viewModel.latLonLiveData.observe(this){
            latLon.value = it
        }

        MapLibre(
            modifier = modifier
                .fillMaxSize()
                .border(BorderStroke(2.dp, Color.Red)),
            styleBuilder = styleBuilder,
            cameraPosition = CameraPosition(
                target = latLon.value,
                zoom = 12.0
            )
        ){
            ShowCheckpointsOnMap()
        }
    }

    @Composable
    fun EnterLatLon(modifier: Modifier = Modifier) {

        val latState = remember { mutableStateOf("") }
        val lonState = remember { mutableStateOf("") }

        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        )  {
            // Latitude Text Field
            TextField(
                modifier = Modifier.padding(2.dp).weight(1f),
                singleLine = true,
                value = latState.value,
                onValueChange = {latState.value = it}
            )
            // Longitude Text Field
            TextField(
                modifier = Modifier.padding(2.dp).weight(1f),
                singleLine = true,
                value = lonState.value,
                onValueChange = {lonState.value = it}
            )
            // Go Button
            Button(
                modifier = Modifier.padding(2.dp).weight(1f),
                onClick = {
                    try {
                        val lat = latState.value.toDouble()
                        val lon = lonState.value.toDouble()
                        viewModel.let { it.latLon = LatLng(lat, lon) }
                    }
                    catch (e: Exception) {
                        showToast(e.message.toString())
                    }
                }
            ) {
                Text("GO")
            }
        }

    }

    @Composable
    fun ShowCheckpointsOnMap() {

        // Add circle over university
        val uniLatLon = LatLng(50.9079, -1.4077)
        Circle(
            center = uniLatLon,
            radius = 100f,
            opacity = 0f,
            borderColor = "Red",
            borderWidth = 3f
        )

        // Add polygon over St Mary's
        val stMarysCoordinates = listOf(
            LatLng(50.9063, -1.3914), LatLng(50.9063, -1.3905), LatLng(50.9053, -1.3905), LatLng(50.9053, -1.3914)
        )
        Polygon(
            vertices = stMarysCoordinates,
            opacity = 0f,
            borderColor = "Green",
            borderWidth = 3f
        )

        // Route to the railway station
        val routeToRailwayCoordinates = listOf(
            LatLng(50.9079, -1.4015), LatLng(50.9071, -1.4015), LatLng(50.9069, -1.4047), LatLng(50.9073, -1.4077), LatLng(50.9081, -1.4134)
        )
        Polyline(
            points = routeToRailwayCoordinates,
            lineWidth = 3f,
            color = "Blue"
        )

    }

    @Composable
    fun DisplayLatLon(modifier: Modifier = Modifier) {

        // Get Lat and on values from view model
        val lat = remember { mutableStateOf(0.0) }
        val lon = remember { mutableStateOf(0.0) }
        viewModel.latLonLiveData.observe(this){
            lat.value = it.latitude
            lon.value = it.longitude
        }

        Column(
            modifier = modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.Start
            ) {

                val latLonStr = "Latitude: ${lat.value} \t Longitude: ${lon.value}"
                Text(latLonStr)
            }
        }
    }

    /**
     * Checks the app has access to fine location
     */
    private fun checkLocationPermissions(successCallback: () -> Unit) {
        val requiredPermission = Manifest.permission.ACCESS_FINE_LOCATION

        if (checkSelfPermission(requiredPermission) == PackageManager.PERMISSION_GRANTED) {
            successCallback()
        }
        else {
            val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    successCallback()
                }
                else {
                    Toast.makeText(this, "GPS permission not granted", Toast.LENGTH_LONG).show()
                }
            }
            permissionLauncher.launch(requiredPermission)
        }

    }

    /**
     * starts the GPS if permissions are enabled
     */
    @SuppressLint("MissingPermission")
    fun startGPS() {
        checkLocationPermissions {
            Toast.makeText(this, "GPS permission granted", Toast.LENGTH_LONG).show()
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
        }
    }

    /**
     * updates state variables every time location changes
     */
    override fun onLocationChanged(location: Location) {
        if (liveLocation) {
            viewModel.let {
                it.latLon = LatLng(location.latitude, location.longitude)
            }
        }
    }

    fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    @Deprecated("don't use this")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

    }

}
