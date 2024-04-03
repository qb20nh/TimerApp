@file:OptIn(ExperimentalMaterial3Api::class)

package dev.shrk.timerapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import dev.shrk.timerapp.service.ScreenStateListenerService
import dev.shrk.timerapp.ui.theme.TimerAppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TimerAppTheme {
                TimerUI()
            }
        }
        if (isFirstLaunch()) {
            startActivity(Intent(this, FirstTimeActivity::class.java))
        }
        startMyForegroundService()
    }


    private fun isFirstLaunch(): Boolean {
        val sharedPreferences = getSharedPreferences("device_local", MODE_PRIVATE)
        return sharedPreferences.getBoolean("isFirstLaunch", true)
    }

    override fun onResume() {
        super.onResume()
        Log.d("RESUME", "RESUME")
    }

    private fun startMyForegroundService() {
        val serviceIntent = Intent(this, ScreenStateListenerService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }
}


@Composable
fun TimerUI() {
    var timeText by remember { mutableStateOf("") }
    var parsedTime by remember { mutableStateOf("") }
    var secondsTotal by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }

    var disabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Handle FAB click here */ }) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        }
    ) {
            innerPadding -> Box(modifier = Modifier.padding(innerPadding)) {
        CompositionLocalProvider(LocalTextStyle provides TextStyle(fontSize = 32.sp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,

                ) {
                TextField(
                    value = timeText,
                    onValueChange = {
                        timeText = it.filter { char -> char.isDigit() }.take(4).trimStart('0')
                        secondsTotal = parseTime(timeText)
                        parsedTime = when {
                            secondsTotal > 0 -> secondsToTimerText(secondsTotal)
                            else -> ""
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            disabled = true
                            performAction(secondsTotal)
                        }
                    ),
                    enabled = !disabled,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .fillMaxSize()
                        .alpha(0.0F)
                )
                Text(parsedTime)
                Text(
                    when (parsedTime) {
                        "" -> "Enter time"
                        else -> ""
                    },
                    Modifier.alpha(0.6F)
                )
                // Timer display and logic...
            }
        }
        }
    }

}

fun performAction(secondsTotal: Int) {
    // start the timer
}

fun parseTime(input: String): Int {
    val seconds = input.takeLast(2).toIntOrNull() ?: 0
    val minutes = input.dropLast(2).takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
    return minutes * 60 + seconds
}

fun secondsToTimerText(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60

    return when {
        minutes > 0 -> "${minutes}m ${"$remainingSeconds".padStart(2, '0')}s"
        else -> "${remainingSeconds}s"
    }
}