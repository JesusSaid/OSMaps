package com.example.osmap

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.File
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private var isMapDownloaded = false

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

        // Verificar la conectividad a Internet
        if (isInternetAvailable()) {
            // Descargar y mostrar el mapa
            downloadAndShowOSMData()
        } else {
            // Cargar el mapa descargado si no hay Internet
            loadAndShowMap()
        }
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
                    val xmlData = response.body()

                    // Guardar los datos en el almacenamiento interno
                    applicationContext.openFileOutput("map_data.xml", Context.MODE_PRIVATE).use {
                        it.write(xmlData?.toByteArray())
                    }

                    // Mostrar el mapa descargado
                    loadAndShowMap()

                    // Indicar que el mapa se ha descargado completamente
                    isMapDownloaded = true

                    // Mostrar un mensaje indicando que el mapa está listo para ser usado offline
                    Toast.makeText(this@MainActivity, "Mapa descargado y listo para uso offline", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Error al descargar datos de OSM", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error al descargar datos de OSM", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadAndShowMap() {
        val mapView = findViewById<MapView>(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        mapView.setMultiTouchControls(true)
        mapView.minZoomLevel = 2.0
        mapView.maxZoomLevel = 19.0
        mapView.controller.setZoom(12.0)
        mapView.controller.setCenter(GeoPoint(17.8111683, -97.7810027))

        val mapDataFile = File(filesDir, "map_data.xml")
        if (mapDataFile.exists()) {
            // El archivo del mapa descargado está disponible
            val mapTileProvider = MapTileProviderBasic(applicationContext)
            val tilesOverlay = TilesOverlay(mapTileProvider, applicationContext)
            mapView.overlays.add(tilesOverlay)
            mapView.invalidate()
        } else {
            // El archivo del mapa descargado no está disponible
            Toast.makeText(this, "No se puede acceder al mapa sin conexión", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            return networkInfo.isConnected
        }
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
