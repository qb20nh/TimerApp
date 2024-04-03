package dev.shrk.timerapp

import android.app.Activity
import android.content.ContextWrapper
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.shrk.timerapp.ui.theme.TimerAppTheme
import dev.shrk.timerapp.view.composable.ConfirmationDialog
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

class FirstTimeActivity : ComponentActivity() {

    private var permissionRequestResult = CompletableDeferred<Boolean>()

    private val pushNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionRequestResult.complete(granted)
    }

    private suspend fun checkNotificationPermission(): Boolean {
        permissionRequestResult = CompletableDeferred() // Reset for new permission request
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pushNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            return true
        }
        return permissionRequestResult.await()
    }

    private val pages = listOf(
        OnboardingPage("AppIntro", R.drawable.ic_launcher_foreground, "This is TimerApp"),
        OnboardingPage("Notification", R.drawable.ic_launcher_foreground, "Allow notifications permissions please.", "Grant",
            suspend {
                checkNotificationPermission()
            }),
        OnboardingPage("Done", R.drawable.ic_launcher_foreground, "You're ready to go!")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TimerAppTheme {
                // A surface container using the 'background' color from the theme
                OnboardingSlideshow(pages)
            }
        }
    }

    public fun markFirstLaunch() {
        val sharedPreferences = getSharedPreferences("device_local", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("isFirstLaunch", false)
        editor.apply()
    }
}


data class OnboardingPage(
    val title: String,
    @DrawableRes val image: Int,
    val description: String,
    val buttonText: String? = null,
    val buttonAction: (suspend () -> Boolean) = {true},
    val nextAction: ((Boolean) -> Boolean) = {true},
)


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingSlideshow(pages: List<OnboardingPage>, initialPage: Int = 0) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        initialPageOffsetFraction = 0f
    ) {
        pages.size
    }
    var currentPageActionResult by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }
    var triggerPageChange by remember { mutableStateOf(false) }

    HorizontalPager(
        state = pagerState,
        userScrollEnabled = false,
    ) {
        page ->
        val currentPage = pages[page]
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                Image(
                    painter = painterResource(id = currentPage.image),
                    contentDescription = currentPage.title,
                )
            }
            DotIndicator(
                totalDots = pages.size,
                currentDot = pagerState.currentPage
            )
            Column {
                Text(currentPage.description)
                Row {
                    if (currentPage.buttonText != null) {
                        currentPageActionResult = false
                        OnboardingButton(onClick = suspend { currentPage.buttonAction() }, buttonText = currentPage.buttonText)
                    }
                    Button(onClick = {
                        currentPage.nextAction.let {
                            if (currentPageActionResult) {
                                triggerPageChange = true
                            } else {
                                showDialog = true
                            }
                        }
                    }) {
                        Text(when {
                            page == pages.size - 1 -> "Done"
                            else -> "Next"
                        })
                    }
                }
            }
        }

    }

    val activity = getCurrentActivity()

    LaunchedEffect(triggerPageChange) {
        if (triggerPageChange) {
            if (pagerState.currentPage == pages.size - 1) {
                activity?.let {
                    if (it is FirstTimeActivity) {
                        it.markFirstLaunch()
                    }
                    it.finish()
                }
            } else {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
            currentPageActionResult = true
            triggerPageChange = false
        }
    }

    if (showDialog) {
        ConfirmationDialog(
            title = "Proceed?",
            text = "You are skipping important setup for this app",
            onConfirm = {
                showDialog = false
                triggerPageChange = true
            },
            onDismiss = {
                showDialog = false
            })
    }
}

@Composable
fun OnboardingButton(onClick: suspend () -> Boolean, buttonText: String) {
    val scope = rememberCoroutineScope()
    Button(onClick = {
        scope.launch {
            var currentPageActionResult = onClick()
        }
    }) {
        Text(buttonText)
    }
}

@Composable
fun getCurrentActivity(): Activity? {
    var context = LocalContext.current
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}


@Composable
fun DotIndicator(totalDots: Int, currentDot: Int) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(16.dp)
    ) {
        for (i in 0 until totalDots) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(10.dp)
                    .background(if (i == currentDot) Color.Blue else Color.Gray)
            )
        }
    }
}
