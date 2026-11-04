package com.pdfonet.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(items: List<HistoryItem>, onChange: (List<HistoryItem>) -> Unit) {
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    var sort by remember { mutableStateOf("Newest") }
    var sel by remember { mutableStateOf(setOf<Long>()) }
    var askDel by remember { mutableStateOf(false) }
    val sorts = listOf("Newest", "Oldest", "Name", "Size")
    BackHandler(enabled = sel.isNotEmpty()) { sel = emptySet() }

    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    var shown = items.filter { it.name.contains(q, ignoreCase = true) }
    shown = when (filter) {
        "Today" -> shown.filter { it.time >= startOfToday }
        "Favorites" -> shown.filter { it.fav }
        else -> shown
    }
    shown = when (sort) {
        "Oldest" -> shown.sortedBy { it.time }
        "Name" -> shown.sortedBy { it.name.lowercase() }
        "Size" -> shown.sortedByDescending { it.size }
        else -> shown.sortedByDescending { it.time }
    }
    val df = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (sel.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${sel.size} selected", color = CTxt, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Chip("Share") { sharePdfs(ctx, items.filter { it.id in sel }.map { it.uri }) }
                Chip("Delete") { askDel = true }
                Chip("Cancel") { sel = emptySet() }
            }
        } else {
            Title("History")
            OutlinedTextField(
                value = q, onValueChange = { q = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search PDFs") },
                shape = RoundedCornerShape(16.dp), colors = fieldColors()
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Today", "Favorites").forEach { f -> Chip(if (filter == f) "\u25CF $f" else f) { filter = f } }
                Chip("Sort: $sort") { sort = sorts[(sorts.indexOf(sort) + 1) % sorts.size] }
            }
        }
        if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No PDFs yet. Convert something first.", color = CMuted, fontSize = 15.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.id }) { it ->
                    val on = it.id in sel
                    val shape = RoundedCornerShape(14.dp)
                    Row(
                        Modifier.fillMaxWidth().clip(shape).background(CCard)
                            .border(if (on) 2.dp else 1.dp, if (on) CAccent else CLine, shape)
                            .combinedClickable(
                                onClick = { if (sel.isNotEmpty()) sel = if (on) sel - it.id else sel + it.id else openPdf(ctx, it.uri) },
                                onLongClick = { sel = if (on) sel - it.id else sel + it.id }
                            )
                            .padding(start = 10.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Thumb(it.uri, Modifier.width(42.dp).height(56.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(it.name, color = CTxt, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("${it.pages} pages \u00B7 ${fmtSize(it.size)} \u00B7 ${df.format(Date(it.time))}", color = CMuted, fontSize = 12.sp)
                        }
                        Box(
                            Modifier.size(48.dp).clickable { onChange(items.map { x -> if (x.id == it.id) x.copy(fav = !x.fav) else x }) },
                            contentAlignment = Alignment.Center
                        ) { Text(if (it.fav) "\u2605" else "\u2606", color = CAccent, fontSize = 22.sp) }
                    }
                }
            }
        }
    }

    if (askDel) {
        val targets = items.filter { it.id in sel }
        AlertDialog(
            onDismissRequest = { askDel = false },
            containerColor = CCard, titleContentColor = CTxt, textContentColor = CMuted,
            title = { Text("Delete ${targets.size} PDF(s)?") },
            text = { Text(targets.joinToString("\n") { it.name }.take(300)) },
            confirmButton = {
                Column {
                    TextButton(onClick = {
                        onChange(items.filter { it.id !in sel }); sel = emptySet(); askDel = false
                    }) { Text("Remove from list only", color = CAccent) }
                    TextButton(onClick = {
                        targets.forEach { t -> deleteDoc(ctx, t.uri) }
                        onChange(items.filter { it.id !in sel }); sel = emptySet(); askDel = false
                    }) { Text("Delete file too", color = CAccent) }
                    TextButton(onClick = { askDel = false }) { Text("Cancel", color = CMuted) }
                }
            }
        )
    }
}
