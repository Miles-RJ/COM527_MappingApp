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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.mappingapp.ui.theme.MappingAppTheme
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import org.ramani.compose.CameraPosition
import org.ramani.compose.Circle
import org.ramani.compose.MapLibre
import org.ramani.compose.Polygon
import org.ramani.compose.Polyline
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity(), LocationListener {
    val viewModel : GpsViewModel by viewModels()
    val liveLocation = false
    var showCheckpoints = mutableStateOf(false)
    val styleBuilder = Style.Builder().fromUri("https://tiles.openfreemap.org/styles/bright")

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startGPS()
        viewModel.let {
            it.latLon = LatLng(50.9079, -1.4015)
        }
        setContent {
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val coroutineScope = rememberCoroutineScope()
            val navController = rememberNavController()
            MappingAppTheme {


                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Mapping App") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.primary
                            ),
                            actions = {
                                IconButton( // hamburger menu button
                                    onClick = {
                                        if (drawerState.isOpen) {
                                            coroutineScope.launch(){
                                            drawerState.close()
                                                }
                                        }
                                        else {
                                            coroutineScope.launch() {
                                                drawerState.open()
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.Menu, "Menu")
                                }
                            }
                        )
                    },

                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(  // Map icon
                                label = { Text("Map") },
                                icon = { Icon(
                                    painter = painterResource(R.drawable.map_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
                                    "Map") },
                                onClick = { navController.navigate("mapScreen") },
                                selected = false
                            )

                            NavigationBarItem(  // Settings icon
                                label = { Text("Settings") },
                                icon = { Icon(Icons.Filled.Settings, "Settings") },
                                onClick = { navController.navigate("settingsScreen") { popUpTo("settingsScreen") } },
                                selected = false
                            )
                        }
                    },

                    floatingActionButton = {
                        AddClearButton("Show Checkpoints") {
                            showCheckpoints.value = !showCheckpoints.value
                        }
                    }
                ) { innerPadding ->

                    ModalNavigationDrawer(
                        modifier = Modifier.padding(innerPadding),
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet {
                                Column {
                                    NavigationDrawerItem( // Map menu item
                                        selected = false,
                                        label = { Text("Map") },
                                        onClick = {
                                            coroutineScope.launch {
                                                drawerState.close()
                                            }
                                            navController.navigate("mapScreen")
                                        }
                                    )
                                    NavigationDrawerItem( // Settings menu item
                                        selected = false,
                                        label = { Text("Settings") },
                                        onClick = {
                                            coroutineScope.launch {
                                                drawerState.close()
                                            }
                                            navController.navigate("settingsScreen") { popUpTo("settingsScreen") }
                                        }
                                    )
                                }
                            }
                        }
                    ) {
                        Column() {
                            NavHost(navController, startDestination = "settingsScreen") {
                                composable("settingsScreen") {
                                    SettingsScreen(viewModel) {
                                        navController.navigate("mapScreen")
                                    }
                                }
                                composable("mapScreen") {
                                    MapScreen() {
                                        navController.navigate("settingsScreen") { popUpTo("settingsScreen") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    @Composable
    fun AddClearButton(contentDescription: String, onClick: () -> Unit) {
        var iconIndex by remember { mutableStateOf(0) }
        val icons = arrayOf(
            Icons.Filled.Add,
            Icons.Filled.Clear
        )

        FloatingActionButton(
            onClick = {
                onClick()
                iconIndex = (iconIndex+1) % icons.size
            },
            content = {
                Icon(imageVector = icons[iconIndex], contentDescription = contentDescription)
            }
        )

    }

    @Composable
    fun MapScreen(onOpenSettings: () -> Unit) {

        Column(modifier=Modifier.fillMaxSize()) {

            Button(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, "Settings Icon", modifier = Modifier.padding(4.dp))
                Text("Open Settings")
            }

            DisplayLatLon()

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

                DisplayMap(
                    Modifier
                        .align(Alignment.TopCenter)
                        .height(this.maxHeight)
                )
            }
        }
    }

    @Composable
    fun DisplayMap(modifier: Modifier = Modifier) {

        // Get Lat and on values from view model
        var latLon by remember { mutableStateOf(LatLng(0.0, 0.0)) }
        var zoom by remember { mutableStateOf(1.0) }
        viewModel.latLonLiveData.observe(this) {
            latLon = it
        }
        viewModel.zoomLiveData.observe(this) {
            zoom = it
        }

        MapLibre(
            modifier = modifier
                .fillMaxSize()
                .border(BorderStroke(2.dp, Color.Red)),
            styleBuilder = styleBuilder,
            cameraPosition = CameraPosition(
                target = latLon,
                zoom = zoom
            )
        ){
            if (showCheckpoints.value) {
                ShowCheckpointsOnMap()
            }
        }
    }

    @Composable
    fun EnterLatLon(modifier: Modifier = Modifier) {

        var latState by remember { mutableStateOf("") }
        var lonState by remember { mutableStateOf("") }

        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        )  {
            // Latitude Text Field
            OutlinedTextField(
                modifier = Modifier
                    .padding(2.dp)
                    .weight(1f),
                singleLine = true,
                value = latState,
                onValueChange = {latState = it}
            )
            // Longitude Text Field
            OutlinedTextField(
                modifier = Modifier
                    .padding(2.dp)
                    .weight(1f),
                singleLine = true,
                value = lonState,
                onValueChange = {lonState = it}
            )
            // Go Button
            Button(
                modifier = Modifier
                    .padding(2.dp)
                    .weight(1f),
                onClick = {
                    try {
                        val lat = latState.toDouble()
                        val lon = lonState.toDouble()
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
            borderWidth = 1.5f
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
        var lat by remember { mutableStateOf(0.0) }
        var lon by remember { mutableStateOf(0.0) }
        viewModel.latLonLiveData.observe(this){
            lat = it.latitude
            lon = it.longitude
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

                val latLonStr = "Latitude: ${lat} \t Longitude: ${lon}"
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

@Composable
fun SettingsScreen(gpsViewModel: GpsViewModel, onOpenMap: () -> Unit) {

    var latState by remember { mutableStateOf("") }
    var lonState by remember { mutableStateOf("") }
    var zoomState by remember { mutableStateOf("") }

    Column (modifier = Modifier.fillMaxSize().padding(10.dp)){
        Row (modifier = Modifier.fillMaxWidth()) {
            Button(onClick = {
                onOpenMap()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back Arrow", modifier = Modifier.padding(4.dp))
                Text("Open Map")
            }
        }
        Row (modifier = Modifier.fillMaxWidth()) {
            Text("Settings Screen")
        }

        val settingsLabelModifier = Modifier.padding(4.dp).weight(1f)
        val settingsEntryModifier = Modifier.padding(2.dp).weight(2f)

        // Latitude
        Row (modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Latitude: ", modifier=settingsLabelModifier)

            OutlinedTextField(
                modifier = settingsEntryModifier,
                singleLine = true,
                value = latState,
                onValueChange = {
                    latState = it
                    try {
                        val lat = latState.toDouble()
                        gpsViewModel.setLat(lat)
                    }
                    catch (_: Exception) {

                    }
                }
            )
        }

        // Longitude
        Row (modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Longitude: ", modifier=settingsLabelModifier)

            OutlinedTextField(
                modifier = settingsEntryModifier,
                singleLine = true,
                value = lonState,
                onValueChange = {
                    lonState = it
                    try {
                        val lon = lonState.toDouble()
                        gpsViewModel.setLon(lon)
                    } catch (_: Exception) {

                    }
                }
            )
        }

        // Zoom
        Row (modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Zoom: ", modifier=settingsLabelModifier)

            OutlinedTextField(
                modifier = settingsEntryModifier,
                singleLine = true,
                value = zoomState,
                onValueChange = {
                    zoomState = it
                    try {
                        val zoom = zoomState.toDouble()
                        gpsViewModel.zoom = zoom
                    }
                    catch (_: Exception) {

                    }
                }
            )
        }
    }
}
