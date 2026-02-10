package com.example.dilworldtv

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ekran kararmasın
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val scheduleJson = assets.open("schedule.json").bufferedReader().use { it.readText() }
        val nobetJson = assets.open("nobet.json").bufferedReader().use { it.readText() }

        val scheduleEngine = ScheduleEngine(scheduleJson)
        val nobetEngine = NobetEngine(nobetJson)

        setContent {
            MaterialTheme {
                DilworldTvScreen(scheduleEngine, nobetEngine)
            }
        }
    }
}

@Composable
private fun DilworldTvScreen(
    scheduleEngine: ScheduleEngine,
    nobetEngine: NobetEngine
) {
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    val dateFmt = DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", Locale("tr", "TR"))

    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var schedule by remember { mutableStateOf<ScheduleState?>(null) }
    var duties by remember { mutableStateOf<List<DutyUi>>(emptyList()) }

    var weather by remember { mutableStateOf<WeatherUi?>(null) }
    var fx by remember { mutableStateOf<FxUi?>(null) }
    var news by remember { mutableStateOf<NewsUi?>(null) }

    val weatherRepo = remember { WeatherRepository() }
    val fxRepo = remember { FxRepository() }
    val newsRepo = remember { NewsRepository() }

    // clock
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }

    // schedule + duty
    LaunchedEffect(now) {
        val s = scheduleEngine.compute(now)
        schedule = s
        duties = nobetEngine.compute(now, s?.nowType ?: "OFF")
    }

    // weather (30 dk)
    LaunchedEffect(Unit) {
        fun fetch() = kotlinx.coroutines.GlobalScope.launch {
            try { weather = weatherRepo.fetchCurrent() } catch (_: Exception) {}
        }
        fetch()
        while (true) { delay(30 * 60 * 1000L); fetch() }
    }

    // fx (30 dk)
    LaunchedEffect(Unit) {
        fun fetch() = kotlinx.coroutines.GlobalScope.launch {
            try { fx = fxRepo.fetch() } catch (_: Exception) {}
        }
        fetch()
        while (true) { delay(30 * 60 * 1000L); fetch() }
    }

    // news (10 dk)
    LaunchedEffect(Unit) {
        fun fetch() = kotlinx.coroutines.GlobalScope.launch {
            try { news = newsRepo.fetchTitles(limit = 12) } catch (_: Exception) {}
        }
        fetch()
        while (true) { delay(10 * 60 * 1000L); fetch() }
    }

    val bgBrush = Brush.verticalGradient(
        listOf(Color(0xFF1B2C52), Color(0xFF14213D))
    )

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
                .padding(36.dp)
        ) {
            // Main layout
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    GlassCard(modifier = Modifier.width(420.dp)) {
                        Text(now.format(timeFmt), fontSize = 88.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(6.dp))
                        Text(now.format(dateFmt), fontSize = 22.sp, color = Color(0xFFE7ECFF))
                    }

                    GlassCard(modifier = Modifier.weight(1f)) {
                        Text("Dilworld TV", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE7ECFF))
                        Spacer(Modifier.height(12.dp))
                        val s = schedule
                        if (s != null) {
                            Text(
                                s.nowLabel,
                                fontSize = 54.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFFD166)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("Sonraki: ${s.nextLabel}", fontSize = 26.sp, color = Color(0xFFE7ECFF))
                            Spacer(Modifier.height(10.dp))
                            Text("${s.minutesLeft} Dakika Kaldı", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        } else {
                            Text("Bugün program yok", fontSize = 34.sp, color = Color.White)
                        }
                    }

                    GlassCard(modifier = Modifier.width(420.dp)) {
                        Text("Bayrampaşa", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(10.dp))
                        if (weather != null) {
                            Text("${weather!!.tempC}°C", fontSize = 54.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text(weather!!.descriptionTr, fontSize = 26.sp, color = Color(0xFFE7ECFF))
                        } else {
                            Text("Hava durumu…", fontSize = 26.sp, color = Color(0xFFE7ECFF))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    GlassCard(modifier = Modifier.width(620.dp)) {
                        Text("Nöbetçi Öğretmenler", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFD166))
                        Spacer(Modifier.height(14.dp))
                        duties.take(2).forEach { d ->
                            val line = when {
                                d.current != null -> "${d.area}: ${d.current}"
                                d.next != null && d.nextInMinutes != null -> "${d.area}: ${d.next} • ${d.nextInMinutes} dk sonra"
                                d.next != null -> "${d.area}: ${d.next}"
                                else -> "${d.area}: —"
                            }
                            Text(line, fontSize = 24.sp, color = Color.White)
                            Spacer(Modifier.height(10.dp))
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // Empty space (like your reference design)
                    Spacer(Modifier.width(420.dp))
                }

                // Bottom ticker
                GlassTicker {
                    val fxPart = buildString {
                        val u = fx?.usd
                        val e = fx?.eur
                        val g = fx?.gramGoldTry
                        if (u != null) append("USD ₺$u   ")
                        if (e != null) append("EUR ₺$e   ")
                        if (g != null) append("GRAM ALTIN ₺$g   ")
                        if (isNotEmpty()) append("•   ")
                        append("AA: ")
                    }

                    val newsPart = news?.titles?.joinToString("   •   ") ?: "Haberler yükleniyor…"
                    MarqueeText(text = fxPart + newsPart, speedDpPerSec = 130f)
                }
            }
        }
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0x33FFFFFF))
            .padding(24.dp),
        content = content
    )
}

@Composable
private fun GlassTicker(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x66000000))
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
