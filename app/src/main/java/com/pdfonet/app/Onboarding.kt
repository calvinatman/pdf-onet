package com.pdfonet.app

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay

// Change this date when the Privacy Policy or Terms change: everyone will be asked to agree again.
const val TERMS_VERSION = "2026-11-05"

private val StageTop = Color(0xFF0B0B0C)
private val StageBottom = Color(0xFF17100F)
private val Ink = Color(0xFFF5F5F5)
private val InkMuted = Color(0xFFB9B9BD)
private val Panel = Color(0xFF1B1B1E)
private val PanelLine = Color(0xFF303034)
private val BrandRed = Color(0xFFE5303A)
private val LinkRed = Color(0xFFFF7A7A)

private data class ObGoal(
    val id: String, val title: String, val sub: String, val icon: String, val pins: Set<String>
)

private val ObGoals = listOf(
    ObGoal("write", "Write & convert", "Text to PDF, templates, images", "text", setOf("text", "templates", "image")),
    ObGoal("scan", "Scan documents", "Camera, gallery, OCR", "cam", setOf("scan", "ocr", "pdf2img")),
    ObGoal("merge", "Merge & split", "Join, cut and bundle PDFs", "merge", setOf("merge", "split", "mix", "splithalf", "zip")),
    ObGoal("protect", "Protect & shrink", "Passwords and smaller files", "lock", setOf("compress", "setpw", "rmpw", "ocr")),
    ObGoal("markup", "Mark up & sign", "Sign, watermark, number pages", "pen", setOf("sign", "watermark", "numbers", "redact", "details"))
)

@Composable
fun OnboardingFlow(s: AppSettings) {
    val tick = rememberHaptic(s)
    val dur = if (s.anim) 320 else 0
    var page by remember { mutableStateOf(0) }
    var forward by remember { mutableStateOf(true) }
    var goals by remember { mutableStateOf(setOf<String>()) }
    var agreed by remember { mutableStateOf(false) }
    var legal by remember { mutableStateOf<Pair<String, String>?>(null) }
    val view = LocalView.current

    DisposableEffect(Unit) {
        val act = view.context as? Activity
        val c = if (act != null) WindowCompat.getInsetsController(act.window, view) else null
        c?.isAppearanceLightStatusBars = false
        c?.isAppearanceLightNavigationBars = false
        onDispose {
            val light = CBg.luminance() > 0.5f
            c?.isAppearanceLightStatusBars = light
            c?.isAppearanceLightNavigationBars = light
        }
    }

    fun go(p: Int) {
        forward = p > page
        page = p
    }

    BackHandler(enabled = legal != null || page > 0) {
        if (legal != null) legal = null else go(page - 1)
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(StageTop, StageBottom)))) {
        ObGlow(s.anim)
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (page > 0) IconBtnTint("chevL", "Back", Ink) { go(page - 1) } else Spacer(Modifier.size(44.dp))
                if (page < 2) {
                    Text(
                        "Skip", color = InkMuted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { go(2) }.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                } else {
                    Spacer(Modifier.size(44.dp))
                }
            }
            Box(Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        val d = if (forward) 1 else -1
                        (slideInHorizontally(tween(dur)) { it * d / 3 } + fadeIn(tween(dur))) togetherWith
                            (slideOutHorizontally(tween(dur)) { -it * d / 3 } + fadeOut(tween(dur / 2)))
                    },
                    label = "intro"
                ) { p ->
                    when (p) {
                        0 -> ObWelcome(s.anim)
                        1 -> ObGoalsPage(goals, s.anim) { id ->
                            goals = if (id in goals) goals - id else goals + id
                            tick()
                        }
                        else -> ObPrivacy(
                            agreed, s.anim,
                            { tick(); agreed = !agreed },
                            { legal = it }
                        )
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 0..2) {
                        val w by animateDpAsState(if (i == page) 26.dp else 8.dp, tween(dur), label = "dot")
                        Box(
                            Modifier.height(8.dp).width(w).clip(CircleShape)
                                .background(if (i == page) BrandRed else PanelLine)
                        )
                    }
                }
                val enabled = page != 2 || agreed
                val bg by animateColorAsState(if (enabled) Ink else Panel, tween(dur), label = "btn")
                Box(
                    Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp)).background(bg)
                        .then(
                            if (enabled) Modifier.clickable {
                                tick()
                                when (page) {
                                    0 -> go(1)
                                    1 -> {
                                        if (goals.isNotEmpty()) {
                                            s.replacePins(ObGoals.filter { it.id in goals }.flatMap { it.pins }.toSet())
                                        }
                                        go(2)
                                    }
                                    else -> s.acceptTerms(TERMS_VERSION)
                                }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (page == 0) "Get started" else "Continue",
                        color = if (enabled) Color(0xFF111111) else InkMuted,
                        fontSize = 17.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        legal?.let { l ->
            Box(Modifier.fillMaxSize().background(CBg).systemBarsPadding()) {
                LegalScreen(l.first, l.second) { legal = null }
            }
        }
    }
}

@Composable
private fun ObGlow(anim: Boolean) {
    val t = rememberInfiniteTransition(label = "glow")
    val a by t.animateFloat(
        initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )
    val k = if (anim) a else 0.8f
    Canvas(Modifier.fillMaxSize()) {
        val c = Offset(size.width * 0.5f, size.height * 0.28f)
        val r = size.width * 0.95f
        drawCircle(
            brush = Brush.radialGradient(listOf(BrandRed.copy(alpha = 0.30f * k), Color.Transparent), center = c, radius = r),
            radius = r, center = c
        )
    }
}

@Composable
private fun ObWelcome(anim: Boolean) {
    var iconIn by remember { mutableStateOf(!anim) }
    var textIn by remember { mutableStateOf(!anim) }
    LaunchedEffect(Unit) {
        if (anim) {
            iconIn = true
            delay(380)
            textIn = true
        }
    }
    val iconScale by animateFloatAsState(
        if (iconIn) 1f else 0.35f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "iconScale"
    )
    val iconAlpha by animateFloatAsState(if (iconIn) 1f else 0f, tween(260), label = "iconAlpha")
    val textAlpha by animateFloatAsState(if (textIn) 1f else 0f, tween(420), label = "textAlpha")
    val textShift by animateFloatAsState(if (textIn) 0f else 24f, tween(420), label = "textShift")
    var step by remember { mutableStateOf(0) }
    LaunchedEffect(anim) {
        while (anim) {
            delay(1500)
            step = (step + 1) % 4
        }
    }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(0.5f))
        Box(
            Modifier.size(104.dp)
                .graphicsLayer { scaleX = iconScale; scaleY = iconScale; alpha = iconAlpha }
                .clip(RoundedCornerShape(28.dp)).background(Color.Black)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_fg), contentDescription = "PDio icon",
                contentScale = ContentScale.Fit, modifier = Modifier.size(104.dp).scale(1.35f)
            )
        }
        Spacer(Modifier.height(20.dp))
        Column(
            Modifier.graphicsLayer { alpha = textAlpha; translationY = textShift },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.wordmark), contentDescription = "PDio",
                contentScale = ContentScale.Fit, modifier = Modifier.height(44.dp).aspectRatio(420f / 196f)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "Your PDFs. Your phone.\nNothing leaves.", color = Ink, fontSize = 26.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 32.sp
            )
        }
        Spacer(Modifier.height(26.dp))
        ObSheets(step, anim)
        Spacer(Modifier.weight(0.5f))
    }
}

@Composable
private fun ObSheets(step: Int, anim: Boolean) {
    val xs = listOf(
        floatArrayOf(0f, 0f, 0f), floatArrayOf(-48f, 0f, 48f),
        floatArrayOf(0f, 0f, 0f), floatArrayOf(-22f, 0f, 22f)
    )
    val rs = listOf(
        floatArrayOf(0f, 0f, 0f), floatArrayOf(-6f, 0f, 6f),
        floatArrayOf(0f, 0f, 0f), floatArrayOf(-12f, 0f, 12f)
    )
    val scs = listOf(1f, 1f, 0.78f, 1f)
    val spec: AnimationSpec<Float> =
        if (anim) spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow) else snap()
    val sc by animateFloatAsState(scs[step], spec, label = "sheetScale")
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.height(130.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            for (i in 0..2) {
                val x by animateFloatAsState(xs[step][i], spec, label = "sheetX$i")
                val r by animateFloatAsState(rs[step][i], spec, label = "sheetR$i")
                val shape = RoundedCornerShape(8.dp)
                Column(
                    Modifier.size(width = 88.dp, height = 120.dp)
                        .graphicsLayer { translationX = x.dp.toPx(); rotationZ = r; scaleX = sc; scaleY = sc }
                        .background(Color.White, shape).border(1.dp, Color(0xFFE0E0E3), shape)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(Modifier.fillMaxWidth(0.55f).height(8.dp).background(Color(0xFF2A2A2E), RoundedCornerShape(3.dp)))
                    listOf(1f, 0.9f, 1f, 0.7f, 0.95f).forEach { w ->
                        Box(Modifier.fillMaxWidth(w).height(5.dp).background(Color(0xFFD2D2D6), RoundedCornerShape(3.dp)))
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Merge", "Split", "Compress", "Sign & scan").forEachIndexed { i, t ->
                val on = i == step
                val bg by animateColorAsState(if (on) BrandRed else Panel, tween(220), label = "chip")
                Text(
                    t, color = if (on) Color.White else InkMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(bg)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ObGoalsPage(goals: Set<String>, anim: Boolean, onToggle: (String) -> Unit) {
    val count = ObGoals.filter { it.id in goals }.flatMap { it.pins }.toSet().size
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(rememberScrollState())
    ) {
        Text("What will you do most?", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Pick any. We pin the matching tools on your Home.", color = InkMuted, fontSize = 15.sp)
        Spacer(Modifier.height(18.dp))
        ObGoals.forEachIndexed { i, g ->
            ObGoalCard(g, g.id in goals, i, anim) { onToggle(g.id) }
            Spacer(Modifier.height(10.dp))
        }
        Text(
            if (count == 0) "Nothing picked: we keep the default tools." else "$count tools will be pinned.",
            color = InkMuted, fontSize = 13.sp
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ObGoalCard(g: ObGoal, sel: Boolean, index: Int, anim: Boolean, onClick: () -> Unit) {
    var shown by remember { mutableStateOf(!anim) }
    LaunchedEffect(Unit) {
        if (anim) {
            delay(120L + 80L * index)
            shown = true
        }
    }
    val scale by animateFloatAsState(
        if (sel) 1.02f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "goalScale"
    )
    val border by animateColorAsState(if (sel) BrandRed else PanelLine, tween(200), label = "goalBorder")
    val shape = RoundedCornerShape(18.dp)
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }
    ) {
        Row(
            Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(shape).background(Panel).border(if (sel) 2.dp else 1.dp, border, shape)
                .clickable(onClick = onClick).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF26262A)),
                contentAlignment = Alignment.Center
            ) { SvgIcon(g.icon, if (sel) BrandRed else Ink, 22.dp) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(g.title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(g.sub, color = InkMuted, fontSize = 13.sp)
            }
            ObCheck(sel)
        }
    }
}

@Composable
private fun ObCheck(on: Boolean) {
    val bg by animateColorAsState(if (on) BrandRed else Color.Transparent, tween(180), label = "check")
    val sc by animateFloatAsState(if (on) 1f else 0f, spring(Spring.DampingRatioMediumBouncy), label = "checkScale")
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier.size(26.dp).clip(shape).background(bg).border(2.dp, if (on) BrandRed else InkMuted, shape),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.graphicsLayer { scaleX = sc; scaleY = sc }) { SvgIcon("check", Color.White, 16.dp) }
    }
}

@Composable
private fun ObReveal(index: Int, anim: Boolean, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(!anim) }
    LaunchedEffect(Unit) {
        if (anim) {
            delay(120L + 110L * index)
            shown = true
        }
    }
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(tween(320)) + slideInHorizontally(tween(320)) { it / 4 }
    ) { content() }
}

@Composable
private fun ObPrivacy(agreed: Boolean, anim: Boolean, onToggle: () -> Unit, onOpen: (Pair<String, String>) -> Unit) {
    val rows = listOf(
        "lock" to "Everything stays on this phone",
        "close" to "No account, no ads, no tracking",
        "check" to "No internet permission at all"
    )
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Private by design", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Take a look, then you are in.", color = InkMuted, fontSize = 15.sp)
        Spacer(Modifier.height(22.dp))
        rows.forEachIndexed { i, (ic, t) ->
            ObReveal(i, anim) {
                Row(
                    Modifier.fillMaxWidth().clip(shape).background(Panel).border(1.dp, PanelLine, shape).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF26262A)),
                        contentAlignment = Alignment.Center
                    ) { SvgIcon(ic, BrandRed, 20.dp) }
                    Spacer(Modifier.width(14.dp))
                    Text(t, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.clip(RoundedCornerShape(20.dp)).background(BrandRed.copy(alpha = 0.16f))
                .border(1.dp, BrandRed.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SvgIcon("check", LinkRed, 16.dp)
            Spacer(Modifier.width(8.dp))
            Text("Internet permission: none", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(26.dp))
        Row(
            Modifier.fillMaxWidth().clip(shape).background(Panel).border(1.dp, PanelLine, shape)
                .clickable(onClick = onToggle).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ObCheck(agreed)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("I agree to the", color = Ink, fontSize = 15.sp)
                Row {
                    Text(
                        "Privacy Policy", color = LinkRed, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { onOpen(Pair("Privacy Policy", LegalTexts.privacy)) }
                            .padding(vertical = 6.dp)
                    )
                    Text(" and ", color = Ink, fontSize = 15.sp, modifier = Modifier.padding(vertical = 6.dp))
                    Text(
                        "Terms of Use", color = LinkRed, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { onOpen(Pair("Terms of Use", LegalTexts.terms)) }
                            .padding(vertical = 6.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Last updated 5 November 2026", color = InkMuted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
    }
}
