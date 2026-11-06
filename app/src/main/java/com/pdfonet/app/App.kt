package com.pdfonet.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

enum class NavTab { Home, Scan, History, Settings }

fun iconFor(t: NavTab): String = when (t) {
    NavTab.Home -> "home"
    NavTab.Scan -> "cam"
    NavTab.History -> "folder"
    NavTab.Settings -> "gear"
}

@Composable
fun PdfOnetApp(s: AppSettings) {
    val ctx = LocalContext.current
    val ed = remember { EditorState(ctx) }
    var tab by remember { mutableStateOf(NavTab.Home) }
    var editor by remember { mutableStateOf(false) }
    var full by remember { mutableStateOf(false) }
    var templates by remember { mutableStateOf(false) }
    var options by remember { mutableStateOf(false) }
    var legal by remember { mutableStateOf(false) }
    var legalLast by remember { mutableStateOf(Pair("", "")) }
    var result by remember { mutableStateOf(false) }
    var resultLast by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var history by remember { mutableStateOf(Store.load(ctx)) }
    val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    fun update(list: List<HistoryItem>) {
        history = list
        Store.save(ctx, list)
    }

    fun finish(res: List<PdfResult>) {
        val now = System.currentTimeMillis()
        val items = res.mapIndexed { i, r -> HistoryItem(now + i, r.name, r.uri.toString(), r.pages, r.size, now) }
        update(items + history)
        resultLast = items
        result = true
        if (s.afterConvert == "Open PDF directly" && items.isNotEmpty()) openPdf(ctx, items[0].uri)
    }

    LaunchedEffect(Unit) {
        val days = s.autoClean
        if (days > 0) {
            val cutoff = System.currentTimeMillis() - days * 86400000L
            val old = history.filter { it.time < cutoff && !it.fav }
            if (old.isNotEmpty()) {
                withContext(Dispatchers.IO) { old.forEach { deleteDoc(ctx, it.uri) } }
                update(history.filter { it !in old })
            }
        }
    }
    LaunchedEffect(ed.tfv.text) {
        delay(700)
        withContext(Dispatchers.IO) { ed.saveDraft() }
    }

    BackHandler(enabled = legal || options || result || templates || editor || tab != NavTab.Home) {
        when {
            legal -> legal = false
            options -> options = false
            result -> result = false
            templates -> templates = false
            editor -> { editor = false; full = false }
            else -> tab = NavTab.Home
        }
    }

    val key = when {
        legal -> "legal"
        options -> "options"
        result -> "result"
        templates -> "templates"
        editor -> "editor"
        else -> tab.name
    }

    if (!s.onboarded || s.termsVersion != TERMS_VERSION) {
        OnboardingFlow(s)
    } else {
        Box(Modifier.fillMaxSize().background(CBg).systemBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).imePadding()) {
                    AnimatedContent(
                        targetState = key,
                        transitionSpec = {
                            if (!s.anim) {
                                EnterTransition.None togetherWith ExitTransition.None
                            } else {
                                val names = NavTab.values().map { it.name }
                                val a = names.indexOf(initialState)
                                val b = names.indexOf(targetState)
                                if (a >= 0 && b >= 0) {
                                    val dir = if (b > a) 1 else -1
                                    (slideInHorizontally(tween(260)) { it * dir / 4 } + fadeIn(tween(260))) togetherWith
                                        (slideOutHorizontally(tween(260)) { -it * dir / 4 } + fadeOut(tween(200)))
                                } else {
                                    (fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 12 }) togetherWith
                                        fadeOut(tween(180))
                                }
                            }
                        },
                        label = "screen"
                    ) { k ->
                        when (k) {
                            "legal" -> LegalScreen(legalLast.first, legalLast.second) { legal = false }
                            "options" -> OptionsScreen(s) { options = false }
                            "result" -> ResultScreen(resultLast, s) {
                                result = false; editor = false; full = false; tab = NavTab.Home
                            }
                            "templates" -> TemplatesScreen(ed, s, { templates = false }) { templates = false; editor = true }
                            "editor" -> EditorScreen(
                                ed, s, full, { full = it },
                                { editor = false; full = false }, { options = true }
                            ) { finish(it) }
                            "Home" -> HomeScreen(
                                ed, s, { editor = true }, { tab = NavTab.Scan }, { templates = true }
                            ) { finish(it) }
                            "Scan" -> ScanScreen(s) { finish(it) }
                            "History" -> HistoryScreen(history) { update(it) }
                            else -> SettingsScreen(s, { t, b -> legalLast = Pair(t, b); legal = true }) { update(emptyList()) }
                        }
                    }
                }
                AnimatedVisibility(
                    visible = !full && !legal && !options && !imeOpen,
                    enter = if (s.anim) slideInVertically(tween(220)) { it } + fadeIn(tween(220)) else EnterTransition.None,
                    exit = if (s.anim) slideOutVertically(tween(180)) { it } + fadeOut(tween(160)) else ExitTransition.None
                ) {
                    NavBar(tab, s) { result = false; templates = false; editor = false; tab = it }
                }
            }
        }
    }
}

@Composable
fun NavBar(tab: NavTab, s: AppSettings, onTab: (NavTab) -> Unit) {
    val shape = RoundedCornerShape(32.dp)
    val tick = rememberHaptic(s)
    val dur = if (s.anim) 280 else 0
    BoxWithConstraints(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp)
            .fillMaxWidth().height(64.dp)
            .clip(shape).background(CNav).border(1.dp, CLine, shape)
    ) {
        val itemW = (maxWidth - 16.dp - 12.dp) / 4
        val x by animateDpAsState(8.dp + (itemW + 4.dp) * tab.ordinal, tween(dur), label = "pill")
        Box(
            Modifier.offset(x = x, y = 8.dp).size(width = itemW, height = 48.dp)
                .clip(RoundedCornerShape(24.dp)).background(CAccent)
        )
        Row(Modifier.fillMaxSize().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NavTab.values().forEach { t ->
                val sel = t == tab
                val c by animateColorAsState(if (sel) COnAccent else CTxt, tween(dur), label = "icon")
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(24.dp))
                        .clickable { tick(); onTab(t) }
                        .semantics { contentDescription = t.name },
                    contentAlignment = Alignment.Center
                ) { SvgIcon(iconFor(t), c, 26.dp) }
            }
        }
    }
}

@Composable
fun ResultScreen(items: List<HistoryItem>, s: AppSettings, onAnother: () -> Unit) {
    val ctx = LocalContext.current
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (items.size == 1) {
            Title("PDF ready", "${pagesLabel(items[0].pages)} \u00B7 ${fmtSize(items[0].size)}")
        } else {
            Title("${items.size} PDFs ready", "Split into ${items.size} files")
        }
        Text("Saved to: " + folderLabel(s.tree), color = CMuted, fontSize = 13.sp)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.id }) { it ->
                val shape = RoundedCornerShape(14.dp)
                Row(
                    Modifier.fillMaxWidth().clip(shape).background(CCard).border(1.dp, CLine, shape)
                        .clickable { openPdf(ctx, it.uri) }.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Thumb(it.uri, Modifier.width(42.dp).height(56.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(it.name, color = CTxt, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("${pagesLabel(it.pages)} \u00B7 ${fmtSize(it.size)}", color = CMuted, fontSize = 12.sp)
                    }
                    Text("Open", color = CAccent, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                }
            }
        }
        if (items.size == 1) PrimaryButton("Open") { openPdf(ctx, items[0].uri) }
        GhostButton(if (items.size == 1) "Share" else "Share all") { sharePdfs(ctx, items.map { it.uri }) }
        GhostButton("Convert another", onAnother)
    }
}

@Composable
fun LegalScreen(title: String, body: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 8.dp)) {
        BackRow(title, onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(body, color = CTxt, fontSize = 15.sp, lineHeight = 22.sp)
            Spacer(Modifier.height(24.dp))
        }
    }
}
