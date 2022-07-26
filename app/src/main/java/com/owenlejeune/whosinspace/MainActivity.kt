package com.owenlejeune.whosinspace

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.android.gms.ads.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kieronquinn.monetcompat.app.MonetCompatActivity
import com.owenlejeune.whosinspace.api.model.NasaService
import com.owenlejeune.whosinspace.api.model.PhotoOfTheDayResponse
import com.owenlejeune.whosinspace.extensions.WindowSizeClass
import com.owenlejeune.whosinspace.extensions.getOrientation
import com.owenlejeune.whosinspace.extensions.isConnected
import com.owenlejeune.whosinspace.extensions.rememberWindowSizeClass
import com.owenlejeune.whosinspace.model.Astronaut
import com.owenlejeune.whosinspace.preferences.AppPreferences
import com.owenlejeune.whosinspace.ui.theme.WhosInSpaceTheme
import com.owenlejeune.whosinspace.webcrawler.AstroCrawler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : MonetCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
    }

    private val preferences: AppPreferences by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (preferences.showAds) {
            MobileAds.initialize(this) {}
        }

        lifecycleScope.launchWhenCreated {
            monet.awaitMonetReady()

            val hasInternet = isConnected()

            val backdropImageModel = mutableStateOf<Any?>(null)

            getPhotoOfTheDay { response ->
                backdropImageModel.value = if (response == PhotoOfTheDayResponse.NonResponse || response.mediaType != PhotoOfTheDayResponse.PhotoMediaType.IMAGE) {
                    R.drawable.space_background
                } else {
                    response.hdUrl
                }
            }

            var errorOccurred = false
            val astronautList = mutableStateOf<List<Astronaut>?>(null)
            if (hasInternet && shouldCrawlForData()) {
                crawlForData(astronautList) {
                    errorOccurred = true
                }
            } else if (BuildConfig.DEBUG) {
                val testJson = preferences.testJson
                if (testJson.isNotEmpty()) {
                    val listOfAstronauts = object : TypeToken<ArrayList<Astronaut>>() {}.type
                    astronautList.value = Gson().fromJson(testJson, listOfAstronauts)
                }
            }

            installSplashScreen().apply {
                setKeepOnScreenCondition {
                    (
                        isConnected() &&
                            (astronautList.value == null || backdropImageModel.value == null)
                    ) || errorOccurred
                }
            }

            actionBar?.hide()

            setContent {
                WhosInSpaceTheme(monetCompat = monet) {
                    val context = LocalContext.current

                    var rememberErrorOccurred = remember { errorOccurred }
                    if (rememberErrorOccurred) {
                        rememberErrorOccurred = false
                        Toast.makeText(context, stringResource(id = R.string.loading_error_message), Toast.LENGTH_SHORT).show()
                    }
                    val windowSizeClass = rememberWindowSizeClass()

                    val isRefreshing = rememberSwipeRefreshState(isRefreshing = false)

                    SwipeRefresh(
                        state = isRefreshing,
                        onRefresh = {
                            isRefreshing.isRefreshing = true
                            crawlForData(
                                astronautList = astronautList,
                                onError = {
                                    rememberErrorOccurred = true
                                    isRefreshing.isRefreshing = false
                                },
                                onSuccess = {
                                    isRefreshing.isRefreshing = false
                                }
                            )
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            val model by remember { backdropImageModel }
                            AsyncImage(
                                model = model,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                            )

                            val astronautData = remember { astronautList }
                            val isAdLoaded = remember { mutableStateOf(false) }

                            if (windowSizeClass == WindowSizeClass.Expanded) {
                                DualColumnView(
                                    astronautData = astronautData,
                                    isAdLoaded = isAdLoaded
                                )
                            } else {
                                SingleColumnView(
                                    astronautData = astronautData,
                                    isAdLoaded = isAdLoaded
                                )
                            }

                            AdvertView(
                                modifier = Modifier.align(Alignment.BottomCenter),
                                isAdLoaded = isAdLoaded
                            )
                        }
                    }
                }
            }
        }
    }

    private fun shouldCrawlForData(): Boolean {
        return !preferences.useTestJson || preferences.testJson.isEmpty()
    }

    private fun crawlForData(
        astronautList: MutableState<List<Astronaut>?>,
        onSuccess: () -> Unit = {},
        onError: () -> Unit = {}
    ) {
        try {
            AstroCrawler().scrapeAstroData { astronauts ->
                astronautList.value = astronauts
                preferences.testJson = Gson().toJson(astronautList.value)
                onSuccess()
            }
        } catch (e: Exception) {
            onError()
        }
    }

    private fun getPhotoOfTheDay(callback: (PhotoOfTheDayResponse) -> Unit) {
        if (isConnected()) {
            CoroutineScope(Dispatchers.Main).launch {
                val photoService = NasaService().createService()
                val photoResponse = photoService.getAstronomyPhotoOfTheDay()
                if (photoResponse.isSuccessful) {
                    photoResponse.body()?.let {
                        callback(it)
                        return@launch
                    }
                }
                callback(PhotoOfTheDayResponse.NonResponse)
            }
        } else {
            callback(PhotoOfTheDayResponse.NonResponse)
        }
    }

    @Composable
    private fun SingleColumnView(
        astronautData: MutableState<List<Astronaut>?>,
        useLargeCards: Boolean = false,
        isAdLoaded: MutableState<Boolean>
    ) {
        val scrollableModifier = if (astronautData.value == null) {
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
                number = astronautData.value?.size,
                modifier = Modifier.padding(top = 50.dp, bottom = 50.dp)
            )

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .background(color = MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
                    .fillMaxSize()
                    .padding(all = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (astronautData.value == null && isConnected()) {
                    Spacer(modifier = Modifier.weight(1f))
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(80.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    if (!isConnected()) {
                        OfflineRow(astronautData = astronautData)
                    }
                    astronautData.value?.forEach { astronaut ->
                        if (useLargeCards) {
                            LargeAstronautCard(astronaut = astronaut)
                        } else {
                            AstronautCard(astronaut = astronaut)
                        }
                    }
                }
                PrivacyPolicyView(isAdLoaded = isAdLoaded)
            }
        }
    }

    @Composable
    private fun DualColumnView(
        astronautData: MutableState<List<Astronaut>?>,
        isAdLoaded: MutableState<Boolean>
    ) {
        if (getOrientation() == Configuration.ORIENTATION_PORTRAIT) {
            SingleColumnView(
                astronautData = astronautData,
                useLargeCards = true,
                isAdLoaded = isAdLoaded
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
                    number = astronautData.value?.size
                )

                DualPaneAstronautColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    astronautData = astronautData,
                    isAdLoaded = isAdLoaded
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
        astronautData: MutableState<List<Astronaut>?>,
        isAdLoaded: MutableState<Boolean>
    ) {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .background(color = MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
                .padding(all = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (astronautData.value == null && isConnected()) {
                Spacer(modifier = Modifier.weight(1f))
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.weight(1f))
            } else {
                if (!isConnected()) {
                    OfflineRow(astronautData = astronautData)
                }
                astronautData.value?.forEach { astronaut ->
                    LargeAstronautCard(
                        astronaut = astronaut
                    )
                }
                PrivacyPolicyView(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    isAdLoaded = isAdLoaded
                )
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
            backgroundColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
            elevation = 20.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
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
            Column(
                modifier = Modifier.clip(RoundedCornerShape(20.dp))
            ) {
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
    
    @Composable
    private fun OfflineRow(
        astronautData: MutableState<List<Astronaut>?>
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .semantics(mergeDescendants = true) {}
                    .align(Alignment.TopCenter)
            ) {
                Text(
                    text = stringResource(R.string.offline_label),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_cloud_off),
                    tint = MaterialTheme.colorScheme.onBackground,
                    contentDescription = null
                )
            }

            val context = LocalContext.current
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clickable(
                        onClick = {
                            if (isConnected()) {
                                astronautData.value = emptyList()
                                astronautData.value = null
                                crawlForData(astronautData) {
                                    Toast
                                        .makeText(
                                            context,
                                            context.getString(R.string.loading_error_message),
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                }
                            } else {
                                Toast
                                    .makeText(context, R.string.offline_label, Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    ),
                tint = MaterialTheme.colorScheme.onBackground
            )
        } 
    }

    @Composable
    private fun AdvertView(
        modifier: Modifier = Modifier,
        isAdLoaded: MutableState<Boolean>
    ) {
        if (preferences.showAds) {
            if (LocalInspectionMode.current) {
                Text(
                    modifier = modifier
                        .fillMaxWidth()
                        .background(Color.Red)
                        .padding(horizontal = 2.dp, vertical = 6.dp),
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    text = "Advert Here",
                )
            } else {
                AndroidView(
                    modifier = modifier.fillMaxWidth(),
                    factory = { context ->
                        AdView(context).apply {
                            adUnitId = BuildConfig.BannerAdUnitId
                            setAdSize(AdSize.BANNER)
                            loadBanner(this)
                            adListener = object : AdListener() {
                                override fun onAdFailedToLoad(p0: LoadAdError) {
                                    super.onAdFailedToLoad(p0)
                                    isAdLoaded.value = false
                                }

                                override fun onAdLoaded() {
                                    super.onAdLoaded()
                                    isAdLoaded.value = true
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    @Composable
    private fun PrivacyPolicyView(
        modifier: Modifier = Modifier,
        isAdLoaded: MutableState<Boolean>
    ) {
        Column(
            modifier = modifier
        ) {
            Text(
                text = stringResource(id = R.string.privacy_policy_label),
                style = TextStyle(textDecoration = TextDecoration.Underline),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = modifier.clickable(
                    onClick = {
                        openPrivacyPolicyLink()
                    }
                )
            )
            if (isAdLoaded.value) {
                Spacer(modifier = Modifier.height(AdSize.BANNER.height.dp))
            }
        }
    }

    private fun loadBanner(adView: AdView) {
        val testDevices = listOf(AdRequest.DEVICE_ID_EMULATOR)

        val requestConfiguration = RequestConfiguration.Builder()
            .setTestDeviceIds(testDevices)
            .build()

        MobileAds.setRequestConfiguration(requestConfiguration)

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    private fun openPrivacyPolicyLink() {
        val link = getString(R.string.privacy_policy_link)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        // If the user has a default browser set, open the intent there
        // Prevents the system from asking the user to open the link in the ArriveCAN app
        try {
            getDefaultBrowserPackage(this)?.let { browserPackage ->
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.setPackage(browserPackage)
            }
            startActivity(intent)
        } catch(exception: ActivityNotFoundException) {
            Log.e(TAG, "Exception: browser possibly not found")
        }
    }

    fun getDefaultBrowserPackage(context: Context): String? {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://"))
        val resolveInfo = context.packageManager.resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }
}