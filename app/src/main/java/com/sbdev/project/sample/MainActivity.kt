package com.sbdev.project.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sbdev.project.richlinkpreview.RichLinkPreview
import com.sbdev.project.richlinkpreview.RichLinkStyle
import com.sbdev.project.sample.ui.theme.RichLinkPreviewTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RichLinkPreviewTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(text = "Default Style", modifier = Modifier.padding(bottom = 8.dp))
                        RichLinkPreview(url = "https://www.google.com")

                        Text(text = "Skype Style", modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                        RichLinkPreview(url = "https://www.github.com", style = RichLinkStyle.SKYPE)

                        Text(text = "Twitter Style", modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                        RichLinkPreview(url = "https://www.github.com", style = RichLinkStyle.TWITTER)

                        Text(text = "Telegram Style", modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                        RichLinkPreview(url = "https://www.telegram.org", style = RichLinkStyle.TELEGRAM)
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RichLinkPreviewTheme {
        Greeting("Android")
    }
}