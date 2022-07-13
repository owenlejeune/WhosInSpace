package com.owenlejeune.whosinspace

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.owenlejeune.whosinspace.extensions.WindowSizeClass
import com.owenlejeune.whosinspace.extensions.getOrientation
import com.owenlejeune.whosinspace.extensions.rememberWindowSizeClass
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
                    val windowSizeClass = rememberWindowSizeClass()

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color = MaterialTheme.colorScheme.background)
                    ) {
                        val bottomPadding = if (windowSizeClass == WindowSizeClass.Expanded) 0.dp else 12.dp
                        Image(
                            painter = painterResource(id = R.drawable.space_background_2),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = bottomPadding)
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

                        if (windowSizeClass == WindowSizeClass.Expanded) {
                            DualColumnView(
                                astronautData = astronautData.value
                            )
                        } else {
                            SingleColumnView(
                                astronautData = astronautData.value
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SingleColumnView(
        astronautData: List<Astronaut>?,
        useLargeCards: Boolean = false
    ) {
        val scrollableModifier = if (astronautData == null) {
            Modifier
        } else {
            Modifier.verticalScroll(rememberScrollState())
        }

        Column(
            modifier = scrollableModifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MainNumberCard(
                number = astronautData?.size,
                modifier = Modifier.padding(top = 50.dp, bottom = 50.dp)
            )

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .background(color = MaterialTheme.colorScheme.background)
                    .fillMaxSize()
                    .padding(all = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                if (astronautData == null) {
                    Spacer(modifier = Modifier.weight(1f))
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(80.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    astronautData.forEach { astronaut ->
                        if (useLargeCards) {
                            LargeAstronautCard(astronaut = astronaut)
                        } else {
                            AstronautCard(astronaut = astronaut)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun DualColumnView(
        astronautData: List<Astronaut>?
    ) {
        if (getOrientation() == Configuration.ORIENTATION_PORTRAIT) {
            SingleColumnView(
                astronautData = astronautData,
                useLargeCards = true
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                DualPaneNumberCard(
                    modifier = Modifier
                        .weight(.6f)
                        .fillMaxHeight(),
                    number = astronautData?.size
                )

                DualPaneAstronautColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    astronautData = astronautData
                )
            }
        }
    }

    @Composable
    private fun DualPaneNumberCard(
        modifier: Modifier,
        number: Int?
    ) {
        Box(
            modifier = modifier
        ) {
            MainNumberCard(
                modifier = Modifier.align(Alignment.Center),
                number = number
            )
        }
    }

    @Composable
    private fun DualPaneAstronautColumn(
        modifier: Modifier,
        astronautData: List<Astronaut>?
    ) {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .background(color = MaterialTheme.colorScheme.background)
                .padding(all = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (astronautData == null) {
                Spacer(modifier = Modifier.weight(1f))
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.weight(1f))
            } else {
                astronautData.forEach { astronaut ->
                    LargeAstronautCard(
                        astronaut = astronaut
                    )
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
                .toggleable(
                    value = expanded,
                    onValueChange = {
                        expanded = it
                    }
                )
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            backgroundColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                ) {
                    val profileImageSize = AstronautProfileImage(astronaut = astronaut, isExpanded = expanded)

                    Box(
                        modifier = Modifier
                            .height(profileImageSize.height)
                            .fillMaxWidth()
                    ) {
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
                                        text = stringResource(
                                            id = R.string.number_of_missions,
                                            astronaut.numberOfMissions
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp
                                    )
                                }

                                Text(
                                    text = stringResource(
                                        id = R.string.days_in_space,
                                        astronaut.dayInSpace
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        
                        AsyncImage(
                            model = astronaut.flagImageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(size = DpSize(width = 40.dp, height = 27.dp))
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
                    
                    ReadMoreView(
                        astronaut = astronaut,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun LargeAstronautCard(
        astronaut: Astronaut,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            backgroundColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                ) {
                    val profileImageSize = AstronautProfileImage(astronaut = astronaut, isExpanded = true)

                    Column(
                        modifier = Modifier
                            .height(height = profileImageSize.height)
                            .padding(all = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Column {
                                Text(
                                    text = astronaut.name,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontSize = 20.sp
                                )
                                Text(
                                    text = astronaut.craft,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 14.sp
                                )
                            }

                            Column(
                                modifier = Modifier.padding(start = 12.dp)
                            ) {
                                Text(
                                    text = stringResource(
                                        id = R.string.number_of_missions,
                                        astronaut.numberOfMissions
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = stringResource(
                                        id = R.string.days_in_space,
                                        astronaut.dayInSpace
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Text(
                            text = astronaut.occupationOrRank?.title ?: "",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Column (
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(top = 12.dp)
                        ) {
                            Text(
                                text = astronaut.profileExcerpt,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            ReadMoreView(
                                astronaut = astronaut,
                                modifier = Modifier
                                    .padding(end = 12.dp, bottom = 12.dp)
                            )
                        }
                    }
                }

                AsyncImage(
                    model = astronaut.flagImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(size = DpSize(width = 120.dp, height = 81.dp))
                        .align(Alignment.TopEnd),
                    contentScale = ContentScale.FillBounds
                )
            }
        }
    }
    
    @Composable
    private fun AstronautProfileImage(
        astronaut: Astronaut,
        isExpanded: Boolean
    ): DpSize {
        val profileImageSize = if (!isExpanded) {
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
        
        return profileImageSize
    }
    
    @Composable
    private fun ReadMoreView(
        astronaut: Astronaut,
        modifier: Modifier = Modifier
    ) {
        Row(
            modifier = modifier
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
                text = stringResource(R.string.read_more_text),
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