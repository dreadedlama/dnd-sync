package `in`.dreadedlama.dndsync

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import `in`.dreadedlama.dndsync.MainScreen

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            val wearColorPalette = Colors(
                primary = colorResource(R.color.primaryColor),
                primaryVariant = colorResource(R.color.primaryLightColor),
                secondary = colorResource(R.color.secondaryColor),
                secondaryVariant = colorResource(R.color.secondaryLightColor),
                background = Color.Black,
            )

            MaterialTheme(
                colors = wearColorPalette,
            ) {
                MainScreen(this@MainActivity)
            }
        }
    }
}