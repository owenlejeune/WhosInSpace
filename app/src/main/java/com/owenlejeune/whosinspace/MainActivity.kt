package com.owenlejeune.whosinspace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.monetcompat.app.MonetCompatActivity
import com.owenlejeune.whosinspace.model.Astronaut
import com.owenlejeune.whosinspace.ui.theme.WhosInSpaceTheme
import com.owenlejeune.whosinspace.webcrawler.AstroCrawler

class MainActivity : MonetCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launchWhenCreated {
            monet.awaitMonetReady()
            setContent {
                WhosInSpaceTheme(monetCompat = monet) {
                    Column {
                        val astronautData = remember { mutableStateOf<List<Astronaut>?>(null) }
                        if (astronautData.value == null) {
                            AstroCrawler().scrapeAstroData { astronauts ->
                                astronautData.value = astronauts
                            }
                        }
                        astronautData.value?.let { astronauts ->
                            // todo - show full cards
                            Text(text = "${astronauts.size}")
                            Spacer(modifier = Modifier.height(20.dp))
                            astronauts.forEach { astronaut ->
                                val text = astronaut.toString()
                                Text(text = text)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}