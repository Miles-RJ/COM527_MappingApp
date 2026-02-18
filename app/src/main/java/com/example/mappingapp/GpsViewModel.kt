package com.example.mappingapp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.maplibre.android.geometry.LatLng

class GpsViewModel : ViewModel() {

    var latLonLiveData = MutableLiveData<LatLng>(LatLng(0.0, 0.0))

    var zoom = 1.0
        set(newZoom) {
            field = newZoom
            zoomLiveData.value = newZoom
        }
    var zoomLiveData = MutableLiveData<Double>()

    fun setLat(newLat: Double) {
          latLon = LatLng(newLat, latLon.longitude)
    }

    fun setLon(newLon: Double) {
        latLon = LatLng(latLon.longitude, newLon)
    }

    var latLon = LatLng(0.0, 0.0)
        set(newLatLon) {
            field = newLatLon
            latLonLiveData.value = newLatLon
        }

}