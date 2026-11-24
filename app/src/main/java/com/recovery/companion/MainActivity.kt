package com.recovery.companion

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.recovery.companion.blocking.ProtectionManager
import com.recovery.companion.data.ContentChunk
import com.recovery.companion.data.DisableRequest
import com.recovery.companion.data.Triggers
import com.recovery.companion.ui.SafeWebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as RecoveryApp
        setContent {
            // Arabic-first app: force RTL layout regardless of system locale.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = lightColorScheme(background = Color(0xFFF7F3EC))) {
                    AppNav(app)
                }
            }
        }
    }
}

private val BG = Color(0xFFF7F3EC)
private val CARD = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav(app: RecoveryApp) {
    val nav = rememberNavController()
    var start by remember { mutableStateOf("home") }
    LaunchedEffect(Unit) {
        start = if (app.settings.isOnboardingDone()) "home" else "onboarding"
    }
    Scaffold(
        containerColor = BG,
        topBar = {
            TopAppBar(
                title = { Text("رفيق التعافي", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { nav.navigate("settings") }) { Text("الإعدادات") }
                }
            )
        }
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.padding(pad).fillMaxSize()
        ) {
            composable("onboarding") { OnboardingScreen(app) { nav.navigate("home") } }
            composable("home") { HomeScreen(app, nav) }
            composable("urge") { UrgeScreen(app) { nav.popBackStack() } }
            composable("programme") { ProgrammeScreen(app) }
            composable("coach") { CoachScreen(app) }
            composable("protection") { ProtectionScreen(app) }
            composable("browser") { SafeWebView() }
            composable("settings") { SettingsScreen(app) }
        }
    }
}

@Composable
fun Screen(body: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) { body() }
}

@Composable
fun StatCard(title: String, value: String, note: String) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CARD)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 14.sp, color = Color.Gray)
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(note, fontSize = 13.sp)
        }
    }
}

@Composable
fun HomeScreen(app: RecoveryApp, nav: androidx.navigation.NavController) {
    val scope = rememberCoroutineScope()
    var practiceDays by remember { mutableIntStateOf(0) }
    var checkIns by remember { mutableIntStateOf(0) }
    var lapses by remember { mutableIntStateOf(0) }
    var protectionOn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            practiceDays = app.db.urgeDao().practiceDays()
            checkIns = app.db.dayDao().checkInDays()
            lapses = app.db.urgeDao().lapseCount()
            protectionOn = app.settings.isProtectionEnabled()
        }
    }
    Screen {
        Text("النهاردة", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "مفيش أي بيانات بتطلع من موبايلك. مفيش سيرفر، مفيش حساب، مفيش تتبّع.",
            fontSize = 13.sp
        )
        Button(
            onClick = { nav.navigate("urge") },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text("عندي رغبة دلوقتي — ساعدني", fontSize = 17.sp) }
        StatCard(
            "أيام الممارسة",
            "$practiceDays",
            "أيام اشتغلت فيها على نفسك. الانتكاسة مش بتصفّر العدّاد — هي بيانات بنتعلم منها."
        )
        StatCard("أيام المتابعة", "$checkIns", "أيام سجلت فيها متابعتك اليومية.")
        StatCard("انتكاسات مسجلة", "$lapses", "كل واحدة فيهم معلومة عن محفز، مش فشل.")
        Text(if (protectionOn) "الحماية: شغالة" else "الحماية: متوقفة", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { nav.navigate("programme") }, modifier = Modifier.weight(1f)) {
                Text("برنامج الست أسابيع")
            }
            OutlinedButton(onClick = { nav.navigate("coach") }, modifier = Modifier.weight(1f)) {
                Text("المدرب")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { nav.navigate("protection") }, modifier = Modifier.weight(1f)) {
                Text("الحماية")
            }
            OutlinedButton(onClick = { nav.navigate("browser") }, modifier = Modifier.weight(1f)) {
                Text("متصفح آمن")
            }
        }
        Text(
            "التطبيق ده دعم وتعليم، مش بديل عن طبيب أو معالج نفسي. لو حاسس إنك في خطر على نفسك، اطلب مساعدة إنسان فورًا.",
            fontSize = 12.sp, color = Color.Gray
        )
    }
}

@Composable
fun UrgeScreen(app: RecoveryApp, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var secondsLeft by remember { mutableIntStateOf(90) }
    var running by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var urgeLevel by remember { mutableFloatStateOf(5f) }
    var trigger by remember { mutableStateOf("loneliness") }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
        finished = true
        running = false
    }
    val step = when {
        secondsLeft > 60 -> "١ — تنفس: شهيق 4 ثواني، احبس 4، زفير 4. كرر."
        secondsLeft > 30 -> "٢ — لاحظ: الرغبة موجة بتعلى وبتنزل. سمّي اللي حاسس بيه من غير حكم."
        secondsLeft > 0 -> "٣ — اختار: إيه أصغر خطوة مفيدة تعملها دلوقتي؟"
        else -> "خلصت الـ 90 ثانية. الرغبة أضعف دلوقتي — سجّل اللي حصل."
    }
    Screen {
        Text("ركوب الموجة — 90 ثانية", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("فاضل $secondsLeft ثانية", fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Text(step, fontSize = 16.sp)
        if (!running && !finished) {
            Button(onClick = { running = true }, modifier = Modifier.fillMaxWidth()) {
                Text("ابدأ التمرين")
            }
        }
        if (finished) {
            Text("سجّل الموجة دي (أرقام بس، مفيش كتابة حرة):", fontWeight = FontWeight.Bold)
            Text("شدة الرغبة: ${urgeLevel.toInt()} / 10")
            Slider(value = urgeLevel, onValueChange = { urgeLevel = it }, valueRange = 0f..10f, steps = 9)
            Triggers.ALL.forEach { t ->
                OutlinedButton(
                    onClick = { trigger = t.id },
                    modifier = Modifier.fillMaxWidth()
                ) { Text((if (trigger == t.id) "✓ " else "") + t.labelAr) }
            }
            Button(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        val today = LocalDate.now()
                        val dayEpoch = TimeUnit.MILLISECONDS.toDays(
                            System.currentTimeMillis()
                        )
                        app.db.urgeDao().insert(
                            com.recovery.companion.data.UrgeEvent(
                                dayEpoch = dayEpoch,
                                urgeLevel = urgeLevel.toInt(),
                                triggerId = trigger,
                                exerciseId = "urge-surfing",
                                completed = true,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                        app.db.dayDao().upsert(
                            com.recovery.companion.data.DayLog(
                                date = today.format(DateTimeFormatter.ISO_DATE),
                                checkInDone = true,
                                lapseFlag = false,
                                practiceMinutes = 2
                            )
                        )
                        withContext(Dispatchers.Main) { saved = true }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("حفظ (محلي ومشفر)") }
            if (saved) {
                Text("اتحفظ. عاش — كل موجة بتعديها بتقوّي المهارة.")
                OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text("رجوع للرئيسية")
                }
            }
        }
    }
}

@Composable
fun ProgrammeScreen(app: RecoveryApp) {
    val scope = rememberCoroutineScope()
    var week by remember { mutableIntStateOf(1) }
    var chunks by remember { mutableStateOf<List<ContentChunk>>(emptyList()) }
    var selected by remember { mutableStateOf<ContentChunk?>(null) }
    LaunchedEffect(week) {
        scope.launch(Dispatchers.IO) { chunks = app.content.week(week) }
    }
    Screen {
        Text("برنامج الست أسابيع", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("كل أسبوع 5 دروس قصيرة. الانتكاسة في أي أسبوع بيانات مش فشل — كمّل من مكانك.")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..6).forEach { w ->
                OutlinedButton(onClick = { week = w }) { Text("أسبوع $w") }
            }
        }
        chunks.forEach { c ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CARD),
                onClick = { selected = c }
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(c.title, fontWeight = FontWeight.Bold)
                    Text("يوم ${c.day} — ${c.source}", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
        selected?.let { c ->
            AlertDialog(
                onDismissRequest = { selected = null },
                title = { Text(c.title) },
                text = {
                    Column {
                        Text(c.body)
                        Spacer(Modifier.height(8.dp))
                        Text("المصدر: ${c.source}", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                confirmButton = { TextButton(onClick = { selected = null }) { Text("تمام") } }
            )
        }
    }
}

@Composable
fun CoachScreen(app: RecoveryApp) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var trigger by remember { mutableStateOf<String?>(null) }
    var chunks by remember { mutableStateOf<List<ContentChunk>>(emptyList()) }
    var note by remember { mutableStateOf("") }
    var llmStatus by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        scope.launch { llmStatus = app.llm.status().toString() }
    }
    Screen {
        Text("المدرب", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "المدرب بيبحث في المحتوى المعتمد على جهازك بس، وبيعرضه زي ما هو مع مصدره. " +
            "سؤالك مبيتخزنش. حالة الموديل على الجهاز: $llmStatus"
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("اسأل عن الرغبة، النوم، الوحدة، الانتكاسة…") },
            modifier = Modifier.fillMaxWidth()
        )
        Text("أو اختار محفز:", fontWeight = FontWeight.Bold)
        Triggers.ALL.forEach { t ->
            OutlinedButton(
                onClick = { trigger = t.id },
                modifier = Modifier.fillMaxWidth()
            ) { Text((if (trigger == t.id) "✓ " else "") + t.labelAr) }
        }
        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    val reply = app.coach.respond(query.ifBlank { trigger ?: "" }, trigger)
                    withContext(Dispatchers.Main) {
                        chunks = reply.chunks
                        note = reply.note
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("ابحث في المحتوى المعتمد") }
        if (note.isNotEmpty()) Text(note, fontWeight = FontWeight.Bold)
        chunks.forEach { c ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CARD)) {
                Column(Modifier.padding(12.dp)) {
                    Text(c.title, fontWeight = FontWeight.Bold)
                    Text(c.body)
                    Text("المصدر: ${c.source}", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
        if (chunks.isEmpty() && note.isNotEmpty()) {
            Text("مفيش نتيجة مطابقة — جرّب كلمة تانية أو اختار محفز من القايمة.")
        }
    }
}

@Composable
fun ProtectionScreen(app: RecoveryApp) {
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()
    var protectionOn by remember { mutableStateOf(false) }
    var tampered by remember { mutableStateOf(false) }
    var blockCount by remember { mutableIntStateOf(0) }
    var request by remember { mutableStateOf<DisableRequest?>(null) }
    var codeInput by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            app.protection.startServiceDirect()
            scope.launch { app.settings.setProtectionEnabled(true); protectionOn = true }
        }
    }
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            protectionOn = app.settings.isProtectionEnabled()
            tampered = app.protection.isTampered()
            blockCount = com.recovery.companion.blocking.Blocklist.count()
            request = app.db.disableDao().latest()
        }
    }
    Screen {
        Text("الحماية", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "الحجب هنا احتكاك مش قفل حديد: بيبطّأ الوصول وبيخليه واضح، لكن مفيش حجب " +
            "على موبايل بتملكه ميتفكّش. القوة الحقيقية في المهارات والدعم."
        )
        Text(if (protectionOn) "الحالة: الحماية شغالة" else "الحالة: الحماية متوقفة", fontWeight = FontWeight.Bold)
        if (tampered) Text("تنبيه: الحماية كانت شغالة واتوقفت. اتسجلت محليًا بس.", color = Color(0xFF9C2B2B))
        Text("النطاقات المحجوبة في القايمة: $blockCount (+ قايمة Cloudflare Family 1.1.1.3)")
        Button(
            onClick = {
                if (protectionOn) {
                    message = "عشان توقف الحماية لازم تطلب الإيقاف الأول — في 24 ساعة مهلة وموافقة شخص موثوق."
                } else {
                    val intent = app.protection.prepareIntent()
                    if (intent != null) launcher.launch(intent)
                    else {
                        app.protection.startServiceDirect()
                        scope.launch { app.settings.setProtectionEnabled(true); protectionOn = true }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (protectionOn) "الحماية شغالة" else "شغّل الحماية (VPN محلي)") }
        Text("طبقة تانية يدوية — Private DNS:", fontWeight = FontWeight.Bold)
        Text("Settings ← Network ← Private DNS ← hostname:")
        Text(ProtectionManager.PRIVATE_DNS_HOSTNAME, fontWeight = FontWeight.Bold)
        OutlinedButton(
            onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("dns", ProtectionManager.PRIVATE_DNS_HOSTNAME))
                message = "اتنسخ: ${ProtectionManager.PRIVATE_DNS_HOSTNAME}"
            }
        ) { Text("انسخ الـ hostname") }
        Text("إيقاف الحماية (قفل اجتماعي — 24 ساعة + موافقة شخص موثوق):", fontWeight = FontWeight.Bold)
        OutlinedButton(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    val r = app.protection.requestDisable(app.db)
                    withContext(Dispatchers.Main) {
                        request = r
                        message = "اتسجل طلب الإيقاف. الكود ده ادّيه للشخص الموثوق: ${r.peerCode}"
                    }
                }
            }
        ) { Text("اطلب إيقاف الحماية") }
        request?.let { r ->
            Text("طلب إيقاف: ${java.util.Date(r.requestedAt)} — الكود: ${r.peerCode}")
            OutlinedTextField(
                value = codeInput,
                onValueChange = { codeInput = it },
                label = { Text("اكتب كود موافقة الشخص الموثوق") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        val ok = app.protection.approveDisable(app.db, r.id, codeInput)
                        val can = app.protection.canDisableNow(app.db)
                        withContext(Dispatchers.Main) {
                            message = when {
                                !ok -> "الكود غلط."
                                can -> "الـ 24 ساعة عدّت والموافقة موجودة — تقدر توقف الحماية من زر الإيقاف."
                                else -> "الموافقة اتسجلت. لسه لازم تعدي 24 ساعة من الطلب."
                            }
                        }
                    }
                }
            ) { Text("تأكيد الموافقة") }
            Button(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        if (app.protection.canDisableNow(app.db)) {
                            app.protection.executeDisable(app.db)
                            withContext(Dispatchers.Main) {
                                protectionOn = false
                                message = "الحماية اتوقفت بعد المهلة والموافقة."
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                message = "لسه مينفعش: لازم 24 ساعة + موافقة."
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("نفّذ الإيقاف (بعد المهلة والموافقة)") }
        }
        if (message.isNotEmpty()) Text(message)
    }
}

@Composable
fun SettingsScreen(app: RecoveryApp) {
    val scope = rememberCoroutineScope()
    var lite by remember { mutableStateOf(false) }
    var ram by remember { mutableIntStateOf(0) }
    var showDelete by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        scope.launch {
            lite = app.settings.isLiteMode()
            ram = app.settings.totalRamGb().toInt()
        }
    }
    Screen {
        Text("الإعدادات", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("ذاكرة الجهاز الكلية: $ram GB. الوضع الخفيف (من غير موديل لغوي) هو الافتراضي لأجهزة 2–3 GB.")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("الوضع الخفيف (Lite — من غير LLM)")
            Switch(checked = lite, onCheckedChange = { v ->
                lite = v
                scope.launch { app.settings.setLiteMode(v) }
            })
        }
        Text("الخصوصية: كل بياناتك في قاعدة مشفرة على جهازك (SQLCipher AES-256). مفيش سيرفر، مفيش تتبع، مفيش إعلانات.")
        Text("الإصدار: 0.1.0-phase0 — بنية Phase 0. الموديل اللغوي والـ NSFW بيتنزلوا عند الطلب على واي فاي بس، بعد تثبيت بصمتهم.")
        OutlinedButton(
            onClick = { showDelete = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text("امسح كل بياناتي من الجهاز") }
        if (showDelete) {
            AlertDialog(
                onDismissRequest = { showDelete = false },
                title = { Text("متأكد؟") },
                text = { Text("ده هيمسح قاعدة البيانات المشفرة ومفتاحها. مفيش نسخة احتياطية — البيانات هتضيع نهائيًا.") },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            runCatching { app.db.clearAllTables() }
                            app.keyManager.wipeAll()
                            withContext(Dispatchers.Main) {
                                showDelete = false
                                message = "اتمسحت. اقفل التطبيق وافتحه تاني عشان يبدأ من جديد."
                            }
                        }
                    }) { Text("امسح كل حاجة") }
                },
                dismissButton = { TextButton(onClick = { showDelete = false }) { Text("إلغاء") } }
            )
        }
        if (message.isNotEmpty()) Text(message)
        Text("قانونيًا: التطبيق تعليمي وداعم، مش تشخيص ولا علاج. بيانات الصحة النفسية حساسة بموجب القانون المصري 151/2020 — استشر محامي قبل النشر العام.", fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun OnboardingScreen(app: RecoveryApp, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var page by remember { mutableIntStateOf(0) }
    var ram by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { ram = app.settings.totalRamGb().toInt() }
    Screen {
        when (page) {
            0 -> {
                Text("أهلًا بيك في رفيق التعافي", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("تطبيق عربي، على جهازك، من غير سيرفر ومن غير حساب. هيساعدك بمهارات عملية وبرنامج 6 أسابيع، مش بعدّاد عار.")
                Button(onClick = { page = 1 }, modifier = Modifier.fillMaxWidth()) { Text("كمّل") }
            }
            1 -> {
                Text("خصوصيتك أولًا", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("كل حاجة بتتسجل هنا مشفرة على موبايلك بس: مستوى الرغبة (رقم)، المحفز (اختيار)، التمارين. مفيش كتابة حرة بتتخزن، ومفيش أي حاجة بتطلع من الجهاز.")
                Text("الحجب بيبطّأ الوصول وبيخليه واضح — مش قفل ميتفكّش. ولو وقعت، الانتكاسة بيانات مش فشل.")
                Button(onClick = { page = 2 }, modifier = Modifier.fillMaxWidth()) { Text("فهمت، كمّل") }
            }
            else -> {
                Text("اختار وضعك", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("ذاكرة جهازك: $ram GB. ${if (ram < 4) "جهازك هيشتغل في الوضع الخفيف (من غير موديل لغوي) — وده كفاية تمامًا." else "تقدر تشغّل الوضع الكامل لاحقًا وتنزّل الموديل على واي فاي."}")
                Button(
                    onClick = {
                        scope.launch {
                            app.settings.setLiteMode(ram < 4)
                            app.settings.setStartDate(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                            app.settings.setOnboardingDone(true)
                            onDone()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("ابدأ — الوضع ${if (ram < 4) "الخفيف" else "الكامل"}") }
            }
        }
    }
}
