package com.example.osmap

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.DefaultOverlayManager
import org.osmdroid.views.overlay.TilesOverlay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuración de osmdroid
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", 0))
        setContentView(R.layout.activity_main)

        // Inicializar el MapView
        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        mapView.setBuiltInZoomControls(true)
        mapView.setMultiTouchControls(true)
        mapView.minZoomLevel = 2.0
        mapView.maxZoomLevel = 19.0
        mapView.controller.setCenter(GeoPoint(17.8111683, -97.7810027))
        mapView.controller.setZoom(12.0)

        // Descargar y mostrar el mapa
        downloadAndShowOSMData()
    }

    private fun downloadAndShowOSMData() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://overpass-api.de")
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .build()

        val service = retrofit.create(OverpassService::class.java)

        val query = """
            [out:xml][timeout:25];
            (
              way(around:3000000, 17.8111683,-97.7810027)[highway];
              way(around:3000000, 17.8111683,-97.7810027)[building];
            );
            out body;
            >;
            out skel qt;
        """.trimIndent()

        service.getMapData(query).enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    // Mostrar el mapa
                    mapView.invalidate()
                    Toast.makeText(this@MainActivity, "Mapa cargado exitosamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Error al descargar datos de OSM", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error al descargar datos de OSM", Toast.LENGTH_SHORT).show()
            }
        })
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

    // Interfaz para la API de Overpass
    interface OverpassService {
        @GET("/api/interpreter")
        fun getMapData(@Query("data") query: String): Call<String>
    }
}