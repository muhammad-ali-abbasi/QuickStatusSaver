package com.example.quickstatussaver

import BannerAd
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.quickstatussaver.ui.theme.QuickStatusSaverTheme
import com.example.quickstatussaver.ui.components.DrawerContent
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.quickstatussaver.ui.components.FullScreenComponent
import com.example.quickstatussaver.ui.screens.BusinessStatusScreen
import com.example.quickstatussaver.ui.screens.SavedStatusScreen
import com.example.quickstatussaver.ui.screens.WhatsAppStatusScreen
import com.example.quickstatussaver.utils.PreferencesUtils
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {
    private var interstitialAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferencesUtils.init(this)
        // Initialize Mobile Ads
//        val backgroundScope = CoroutineScope(Dispatchers.IO)
//        backgroundScope.launch {
//            MobileAds.initialize(this@MainActivity) {}
//
//            runOnUiThread {
//                loadInterstitialAd()
//            }
//        }

        setContent {
            var isDarkTheme by rememberSaveable { mutableStateOf(false) }

            QuickStatusSaverTheme(isDarkTheme = isDarkTheme) {
                AppContent(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }

//    private fun loadInterstitialAd() {
//        val adRequest = AdRequest.Builder().build()
//
//        InterstitialAd.load(
//            this,
//            "ca-app-pub-3131360788277380/5447251840", // <-- Test Interstitial Ad Unit ID
//            adRequest,
//            object : InterstitialAdLoadCallback() {
//                override fun onAdLoaded(ad: InterstitialAd) {
//                    Log.d("MainActivity", "Interstitial Ad Loaded")
//                    interstitialAd = ad
//                    showInterstitialAd()
//                }
//
//                override fun onAdFailedToLoad(adError: LoadAdError) {
//                    Log.d("MainActivity", "Failed to load interstitial ad: ${adError.message}")
//                    interstitialAd = null
//                }
//            }
//        )
//    }
//
//    private fun showInterstitialAd() {
//        interstitialAd?.show(this)
//    }
//}


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AppContent(
        isDarkTheme: Boolean,
        onToggleTheme: () -> Unit
    ) {
        val navController = rememberNavController()
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        // Remember drawer content to prevent recreation
        val drawerContent = remember(isDarkTheme) {
            @Composable {
                DrawerContent(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    onItemSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }

        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.fillMaxSize()
        ) {
            // No Drawer / TopBar
            composable("splash") {
                SplashScreen(navController)
            }

            // No Drawer / TopBar
            composable(
                route = "fullScreen/{mediaUri}/{isVideo}/{displayName}/{lastModified}/{fromSavedStatus}",
                arguments = listOf(
                    navArgument("mediaUri") { type = NavType.StringType },
                    navArgument("isVideo") { type = NavType.BoolType },
                    navArgument("displayName") { type = NavType.StringType },
                    navArgument("lastModified") { type = NavType.LongType },
                    navArgument("fromSavedStatus") { type = NavType.BoolType }


                )
            ) { backStackEntry ->
                val mediaUri = backStackEntry.arguments?.getString("mediaUri")
                val isVideo = backStackEntry.arguments?.getBoolean("isVideo")
                val name = backStackEntry.arguments?.getString("displayName")
                val modified = backStackEntry.arguments?.getLong("isVlastModifiedideo")
                val fromSaved = backStackEntry.arguments?.getBoolean("fromSavedStatus")



                mediaUri?.let { uri ->
                    isVideo?.let { video ->
                        if (name != null) {
                            if (modified != null) {
                                if (fromSaved != null) {
                                    FullScreenComponent(
                                        mediaUri = Uri.parse(uri),
                                        isVideo = video,
                                        displayName = name,
                                        lastModified = modified,
                                        fromSavedStatus = fromSaved,
                                        navController = navController
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Drawer + TopBar screens
            composable("whatsapp") {
                MainScaffold(drawerContent, drawerState, scope) {
                    WhatsAppStatusScreen(navController)
                }
            }

            composable("business") {
                MainScaffold(drawerContent, drawerState, scope) {
                    BusinessStatusScreen(navController)
                }
            }

            composable("savedStatus") {
                MainScaffold(drawerContent, drawerState, scope) {
                    SavedStatusScreen(navController)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScaffold(
        drawerContent: @Composable () -> Unit,
        drawerState: DrawerState,
        scope: CoroutineScope,
        content: @Composable (PaddingValues) -> Unit
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = drawerContent
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Quick Status Saver") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    )
                },
                content = { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        content(innerPadding)
                        Spacer(modifier = Modifier.height(8.dp))
                        BannerAd(adUnitId = "ca-app-pub-3131360788277380/5382854428")
                    }
                }
            )
        }
    }
}

