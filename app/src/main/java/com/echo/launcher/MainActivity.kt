package com.echo.launcher

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.echo.launcher.data.AppDatabase
import com.echo.launcher.data.CommandEntry
import com.echo.launcher.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private var speechRecognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EchoLauncherTheme {
                Surface(color = Bg, modifier = Modifier.fillMaxSize()) {
                    LauncherScreen(
                        hasRecognizer = SpeechRecognizer.isRecognitionAvailable(this),
                        createRecognizer = {
                            speechRecognizer?.destroy()
                            SpeechRecognizer.createSpeechRecognizer(this).also { speechRecognizer = it }
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        super.onDestroy()
    }

    // Standard launcher behaviour: Home button / back should not exit the launcher.
    override fun onBackPressed() {
        // no-op — intentionally swallow back presses like a real launcher
    }
}

@Composable
fun LauncherScreen(
    hasRecognizer: Boolean,
    createRecognizer: () -> SpeechRecognizer
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.get(context) }

    val apps = remember { AppRepository.getLaunchableApps(context) }
    val commandLog by db.commandDao().recent().collectAsStateWithLifecycle(initialValue = emptyList())

    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var listening by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Tap the orb to speak") }
    var typedText by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        micGranted = granted
        status = if (granted) "Microphone enabled — tap the orb" else "Microphone permission denied"
    }

    fun logAndRun(text: String) {
        if (text.isBlank()) return
        scope.launch { db.commandDao().insert(CommandEntry(text = text, timestamp = System.currentTimeMillis())) }
        when (val result = VoiceCommandHandler.parse(text, apps)) {
            is CommandResult.LaunchApp -> {
                status = "Opening ${result.app.label}…"
                AppRepository.launch(context, result.app.packageName)
            }
            is CommandResult.WebSearch -> {
                status = "Searching \"${result.query}\"…"
                AppRepository.openWebSearch(context, result.query)
            }
            is CommandResult.NotUnderstood -> {
                status = "Didn't catch that — try again"
            }
        }
    }

    fun startListening() {
        if (!hasRecognizer) {
            status = "Voice recognition isn't available on this device"
            return
        }
        if (!micGranted) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        val recognizer = createRecognizer()
        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                listening = true
                status = "Listening…"
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { listening = false }
            override fun onError(error: Int) {
                listening = false
                status = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that — try again"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied"
                    else -> "Mic error ($error) — try again"
                }
            }
            override fun onResults(results: Bundle?) {
                listening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val best = matches?.firstOrNull()
                if (best != null) {
                    status = "\"$best\""
                    logAndRun(best)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { status = it }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer.startListening(intent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 32.dp, bottom = 16.dp)
    ) {
        Text(
            "ECHO",
            color = InkDim,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp
        )
        Text(
            "Hey, I'm listening.",
            color = Ink,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        if (!micGranted) {
            PermissionBanner(onEnable = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) })
            Spacer(Modifier.height(16.dp))
        }

        VoiceOrb(listening = listening, onTap = { startListening() })

        Text(
            status,
            color = if (listening) Cyan else InkDim,
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 12.dp, bottom = 16.dp)
        )

        OutlinedTextField(
            value = typedText,
            onValueChange = { typedText = it },
            placeholder = { Text("or type a command…", color = InkDim) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Surface,
                unfocusedContainerColor = Surface,
                focusedTextColor = Ink,
                unfocusedTextColor = Ink,
                focusedBorderColor = Violet,
                unfocusedBorderColor = Line
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = {
                logAndRun(typedText); typedText = ""
            })
        )

        Spacer(Modifier.height(24.dp))
        Text("Apps (${apps.size})", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Spacer(Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(apps) { app ->
                AppTile(app = app, onClick = { AppRepository.launch(context, app.packageName) })
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Recent commands", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        CommandLogList(entries = commandLog)
    }
}

@Composable
fun PermissionBanner(onEnable: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Violet, Cyan))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Mic, contentDescription = null, tint = Bg)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Microphone access needed", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Echo needs your mic to hear voice commands.",
                color = InkDim,
                fontSize = 11.5.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onEnable,
            colors = ButtonDefaults.buttonColors(containerColor = Violet)
        ) {
            Text("Enable", fontSize = 12.sp)
        }
    }
}

@Composable
fun VoiceOrb(listening: Boolean, onTap: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "orb")
    val ringScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringScale"
    )
    val ringAlpha by infinite.animateFloat(
        initialValue = 0.7f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (listening) {
            Box(
                modifier = Modifier
                    .size(126.dp * ringScale)
                    .clip(CircleShape)
                    .background(Cyan.copy(alpha = ringAlpha * 0.15f))
            )
        }
        Box(
            modifier = Modifier
                .size(126.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFFA78BFA), Violet, Color(0xFF6D28D9))))
                .clickable { onTap() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Mic,
                contentDescription = "Voice command",
                tint = Bg,
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

@Composable
fun AppTile(app: InstalledApp, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .clickable { onClick() }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.foundation.Image(
            bitmap = app.icon.toBitmap(width = 96, height = 96).asImageBitmap(),
            contentDescription = app.label,
            modifier = Modifier.size(36.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            app.label,
            color = InkDim,
            fontSize = 10.sp,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun CommandLogList(entries: List<CommandEntry>) {
    val fmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    if (entries.isEmpty()) {
        Text(
            "No commands yet — try the orb above.",
            color = InkDim,
            fontSize = 12.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Surface)
                .padding(16.dp)
                .fillMaxWidth()
        )
        return
    }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .heightIn(max = 200.dp)
    ) {
        LazyColumn {
            items(entries) { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        entry.text,
                        color = Ink,
                        fontSize = 12.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(fmt.format(Date(entry.timestamp)), color = InkDim, fontSize = 11.sp)
                }
            }
        }
    }
}
