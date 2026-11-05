package com.pdfonet.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PhotoThumb(uri: Uri, onRemove: () -> Unit) {
    val ctx = LocalContext.current
    val bmp by produceState<ImageBitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) { PdfEngine.decode(ctx, uri, 300)?.asImageBitmap() }
    }
    val shape = RoundedCornerShape(10.dp)
    Box(Modifier.size(width = 84.dp, height = 112.dp).clip(shape).background(CCard).border(1.dp, CLine, shape)) {
        val b = bmp
        if (b != null) {
            Image(bitmap = b, contentDescription = "Photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Box(
            Modifier.align(Alignment.TopEnd).size(40.dp).clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "\u2715", color = Color.White, fontSize = 13.sp,
                modifier = Modifier.background(Color(0x99000000), CircleShape).padding(6.dp)
            )
        }
    }
}

@Composable
fun ScanScreen(s: AppSettings, onResult: (List<PdfResult>) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var shots by remember { mutableStateOf(listOf<Uri>()) }
    var filter by remember { mutableStateOf("Original") }
    var busy by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<Uri?>(null) }

    val cam = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val u = pending
        if (ok && u != null) shots = shots + u
        pending = null
    }
    val gal = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        shots = shots + uris
    }

    fun takePhoto() {
        try {
            val dir = File(ctx.cacheDir, "shots").apply { mkdirs() }
            val f = File(dir, "s" + System.currentTimeMillis() + ".jpg")
            val u = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
            pending = u
            cam.launch(u)
        } catch (e: Exception) {
            toast(ctx, "Camera not available")
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Title("Scan", "Photos to PDF, one page each")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { takePhoto() }, modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CAccent, contentColor = COnAccent)
            ) { Text("Take photo", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            OutlinedButton(
                onClick = { gal.launch("image/*") }, modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, CLine),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CTxt)
            ) { Text("From gallery", fontSize = 16.sp) }
        }
        if (shots.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(16.dp)).background(CCard)
                    .border(1.dp, CLine, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) { Text("No pages yet. Take a photo or pick from gallery.", color = CMuted, fontSize = 14.sp) }
        } else {
            Text("${shots.size} page(s)", color = CMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                shots.forEach { u -> PhotoThumb(u) { shots = shots - u } }
            }
        }
        Text("FILTER", color = CMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Original", "Grayscale", "B&W").forEach { f -> FilterChipX(f, filter == f) { filter = f } }
        }
        PrimaryButton(if (busy) "Creating..." else "Create PDF", enabled = shots.isNotEmpty() && !busy) {
            busy = true
            scope.launch {
                try {
                    val base = "PDF-ONET-SCAN-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                    val list = shots
                    val res = withContext(Dispatchers.Default) {
                        PdfEngine.fromImages(ctx, list, s.style, s.tree, base, filter)
                    }
                    shots = emptyList()
                    onResult(res)
                } catch (e: Exception) {
                    toast(ctx, "Failed: " + (e.message ?: "error"))
                }
                busy = false
            }
        }
        Text(
            "Auto edge detection and OCR (searchable scans) arrive in a later update.",
            color = CMuted, fontSize = 12.sp
        )
    }
}
