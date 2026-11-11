package com.nexus.gamespace

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nexus.gamespace.ui.navigation.NexusApp
import com.nexus.gamespace.ui.theme.NexusTheme

class MainActivity : ComponentActivity() {

    /** Ruta pedida desde fuera (por ejemplo, la biblioteca desde el panel flotante). */
    private var requestedRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestedRoute = intent?.getStringExtra(EXTRA_ROUTE)
        setContent {
            NexusTheme {
                NexusApp(requestedRoute = requestedRoute, onRouteHandled = { requestedRoute = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedRoute = intent.getStringExtra(EXTRA_ROUTE)
    }

    companion object {
        const val EXTRA_ROUTE = "nexus_route"
    }
}
