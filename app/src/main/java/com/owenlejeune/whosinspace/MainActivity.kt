package com.owenlejeune.whosinspace

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kieronquinn.monetcompat.app.MonetCompatActivity
import com.owenlejeune.whosinspace.model.Astronaut
import com.owenlejeune.whosinspace.preferences.AppPreferences
import com.owenlejeune.whosinspace.ui.theme.WhosInSpaceTheme
import com.owenlejeune.whosinspace.webcrawler.AstroCrawler
import org.koin.android.ext.android.inject

class MainActivity : MonetCompatActivity() {

    private val preferences: AppPreferences by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launchWhenCreated {
            monet.awaitMonetReady()
            setContent {
                WhosInSpaceTheme(monetCompat = monet) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color = MaterialTheme.colorScheme.background)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.space_background_2),
                            contentDescription = null,
                            contentScale = ContentScale.FillHeight,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 12.dp)
                        )

                        val astronautData = remember { mutableStateOf<List<Astronaut>?>(null) }

                        val crawlForAstronauts = {
                            if (astronautData.value == null) {
                                AstroCrawler().scrapeAstroData { astronauts ->
                                    astronautData.value = astronauts
                                    preferences.testJson = Gson().toJson(astronautData.value)
                                }
                            }
                        }

                        if (astronautData.value == null) {
                            if (preferences.useTestJson) {
                                val testJson = preferences.testJson
                                if (testJson.isEmpty()) {
                                    crawlForAstronauts()
                                } else {
                                    val listOfAstronauts = object : TypeToken<ArrayList<Astronaut>>() {}.type
                                    astronautData.value = Gson().fromJson(testJson, listOfAstronauts)
                                }
                            } else {
                                crawlForAstronauts()
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            MainNumberCard(
                                number = astronautData.value?.size,
                                modifier = Modifier.padding(top = 50.dp, bottom = 50.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                                    .background(color = MaterialTheme.colorScheme.background)
                                    .fillMaxSize()
                            ) {
                                Column(modifier = Modifier
                                    .background(color = MaterialTheme.colorScheme.background)
                                    .padding(all = 24.dp),
                                    verticalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    astronautData.value?.forEach { astronaut ->
                                        AstronautCard(
                                            astronaut = astronaut
                                        )
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
    private fun MainNumberCard(
        number: Int?,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier
                .width(width = 200.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            elevation = 20.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.background(color = MaterialTheme.colorScheme.background)
            ) {
                Text(
                    text = "${number ?: "?"}",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 52.sp,
                    modifier = Modifier.padding(top = 24.dp)
                )
                Text(
                    text = stringResource(id = R.string.people_in_space_label),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 36.dp)
                )
            }
        }
    }

    @Composable
    private fun AstronautCard(
        astronaut: Astronaut,
        modifier: Modifier = Modifier
    ) {
        var expanded by remember { mutableStateOf(false) }

        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .toggleable(
                    value = expanded,
                    onValueChange = {
                        expanded = it
                    }
                ),
            backgroundColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                ) {
                    val profileImageSize = if (!expanded) {
                        DpSize(width = 80.dp, height = 100.dp)
                    } else {
                        DpSize(width = 160.dp, height = 200.dp)
                    }

                    AsyncImage(
                        model = astronaut.profileImageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(profileImageSize),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .height(profileImageSize.height)
                            .fillMaxWidth()
                            .padding(all = 12.dp)
                    ) {
                        Column {
                            Text(
                                text = astronaut.name,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = astronaut.craft,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 14.sp
                            )
                            if (expanded) {
                                Text(
                                    text = astronaut.occupationOrRank?.title ?: "",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    modifier = Modifier.widthIn(max = 100.dp)
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))


                            if (expanded) {
                                Text(
                                    text = stringResource(id = R.string.number_of_missions, astronaut.numberOfMissions),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                            }

                            Text(
                                text = stringResource(id = R.string.days_in_space, astronaut.dayInSpace),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                        }

                        AsyncImage(
                            model = astronaut.flagImageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(width = 40.dp, height = 27.dp)
                                .align(Alignment.TopEnd),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }

                if (expanded) {
                    Text(
                        text = astronaut.profileExcerpt,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(all = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                            .semantics(mergeDescendants = true) {}
                            .clickable {
                                val urlName = astronaut.name
                                    .replace(" ", "-")
                                    .lowercase()
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data =
                                    Uri.parse("https://www.supercluster.com/astronauts/$urlName")
                                startActivity(intent)
                            }
                    ) {
                        Text(
                            text = "Read more",
                            color = MaterialTheme.colorScheme.tertiary,
                            style = TextStyle(textDecoration = TextDecoration.Underline)
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_outward),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}