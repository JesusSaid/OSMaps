package com.example.osmap

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.osmap.network.MapService
import com.example.osmap.network.RouteRequest
import com.example.osmap.network.RouteResponse
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuración de osmdroid
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", 0))
        setContentView(R.layout.activity_main)

        // Inicializar el MapView
        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setBuiltInZoomControls(true)
        mapView.setMultiTouchControls(true)
        mapView.minZoomLevel = 2.0
        mapView.maxZoomLevel = 19.0
        mapView.controller.setCenter(GeoPoint(17.8111683, -97.7810027))
        mapView.controller.setZoom(12.0)

        // Llamar a la API para obtener la ruta
        getRouteFromAPI()
    }

    private fun getRouteFromAPI() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000")  // Use 10.0.2.2 for the emulator
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(MapService::class.java)
        val request = RouteRequest(
            start = listOf(17.7491299, -97.76906),
            end = listOf(17.8771799, -97.7329293),
            algorithm = "busqueda_bidireccional"
        )

        service.getRoute(request).enqueue(object : Callback<RouteResponse> {
            override fun onResponse(call: Call<RouteResponse>, response: Response<RouteResponse>) {
                if (response.isSuccessful) {
                    val route = response.body()?.ruta
                    if (route != null) {
                        displayRouteOnMap(route)
                        Toast.makeText(this@MainActivity, "Ruta cargada exitosamente", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "No se encontró la ruta", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Error al obtener la ruta", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RouteResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error al conectarse con la API: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayRouteOnMap(route: List<List<Double>>) {
        val geoPoints = route.map { GeoPoint(it[1], it[0]) }
        val polyline = Polyline().apply {
            setPoints(geoPoints)
            outlinePaint.color = Color.RED
        }
        mapView.overlayManager.add(polyline)
        mapView.invalidate()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDetach()
    }
}
