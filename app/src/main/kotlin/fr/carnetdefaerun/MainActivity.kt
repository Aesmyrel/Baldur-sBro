package fr.carnetdefaerun

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import fr.carnetdefaerun.navigation.AppNavigator
import fr.carnetdefaerun.navigation.DeepLinks

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val appState: AppStateViewModel by viewModels()
    private val navigator: AppNavigator by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // L'écran de démarrage reste affiché le temps de relire les préférences et la pile (quelques ms).
        splash.setKeepOnScreenCondition { appState.preferences.value == null || navigator.state.value == null }
        if (savedInstanceState == null) handle(intent)
        setContent { CarnetApp(navigator, appState) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        DeepLinks.parse(intent?.data)?.let(navigator::openDeepLink)
    }
}
