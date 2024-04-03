@file:OptIn(ExperimentalMaterial3Api::class)

package dev.shrk.timerapp

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import dev.shrk.timerapp.service.ScreenStateListenerService
import dev.shrk.timerapp.ui.theme.TimerAppTheme
import java.time.Instant

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TimerAppTheme {
                TimerUI(this)
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
fun TimerUI(context: Context) {
    var userInputText by remember { mutableStateOf("") }
    var displayText by remember { mutableStateOf("") }
    var secondsTotal by remember { mutableIntStateOf(0) }
    var remainingTime by remember { mutableLongStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val ringtone = remember {
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        RingtoneManager.getRingtone(context, ringtoneUri)
    }

    var isTimerGoing by remember { mutableStateOf(false) }
    var timer: CountDownTimer? by remember { mutableStateOf(null) }
    var startTime by remember { mutableStateOf<Instant?>(null) }
    var pauseTime by remember { mutableStateOf<Instant?>(null) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    fun cancelTimer() {
        timer?.cancel() // Cancel the current timer if it's running
        timer = null // Set the timer to null after cancelling
    }

    fun startOrResumeTimer() {
        cancelTimer() // Ensure any running timer is cancelled before starting a new one
        startTime = Instant.now() // Record the start time
        timer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val leftSeconds = (millisUntilFinished / 1000).toInt()
                displayText = secondsToTimerText(leftSeconds)
            }

            override fun onFinish() {
                displayText = "Time's up!"
                ringtone.play()
                isTimerGoing = false
            }
        }.start()
        isTimerGoing = true
    }

    fun pauseTimer() {
        cancelTimer() // Cancel the timer
        pauseTime = Instant.now() // Record the pause time
        if (startTime != null) {
            val elapsed = pauseTime!!.toEpochMilli() - startTime!!.toEpochMilli()
            remainingTime -= elapsed // Subtract the elapsed time from the remaining time
        }
        isTimerGoing = false
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (isTimerGoing) {
                    pauseTimer()
                } else {
                    startOrResumeTimer()
                }
            }) {
                if (isTimerGoing) Icon(
                    painter = painterResource(id = R.drawable.baseline_pause_24),
                    contentDescription = "Pause the timer"
                )
                else Icon(Icons.Filled.PlayArrow, contentDescription = "Resume the timer")
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
                    value = userInputText,
                    onValueChange = {
                        userInputText = it.filter { char -> char.isDigit() }.take(4).trimStart('0')
                        secondsTotal = parseTime(userInputText)
                        displayText = when {
                            secondsTotal > 0 -> secondsToTimerText(secondsTotal)
                            else -> ""
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            isTimerGoing = true
                            focusRequester.freeFocus()
                        if (secondsTotal > 0) {
                            remainingTime = secondsTotal * 1000L
                            startOrResumeTimer()
                        }
                        }
                    ),
                    enabled = !isTimerGoing,
                    readOnly = isTimerGoing,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .fillMaxSize()
                        .alpha(0.0F)
                        .onFocusChanged {
                            if (it.isFocused) {
                                ringtone.stop()
                                displayText = secondsToTimerText(secondsTotal)
                            }
                        }
                )
                Text(displayText)
                Text(
                    when (displayText) {
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