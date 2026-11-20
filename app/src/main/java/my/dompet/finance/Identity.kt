package my.dompet.finance

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.BreakIterator
import java.util.Locale

private val CardTeal = Color(0xFF154C44)
private val CardIce = Color(0xFF325569)
private val CardIvory = Color(0xFFFFFBEC)
private val CardMint = Color(0xFFDEF0DA)

@Composable
fun CardScreen(name: String, frozen: Boolean, onFreeze: (Boolean) -> Unit) {
    var showBack by rememberSaveable { mutableStateOf(false) }
    val angle by animateFloatAsState(if (showBack) 180f else 0f, tween(440), label = "Putaran kad")
    val tint by animateColorAsState(if (frozen) CardIce else CardTeal, label = "Status kad")
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("Kad saya", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Column(
            Modifier.fillMaxWidth().graphicsLayer {
                rotationY = if (angle <= 90f) angle else angle - 180f
                cameraDistance = 12f * density
            }.clip(RoundedCornerShape(22.dp)).background(tint).drawBehind {
                val center = Offset(size.width * 1.04f, size.height * .04f)
                for (ring in 1..5) {
                    drawCircle(Color.White.copy(alpha = .13f), size.width * (.10f + ring * .09f), center, style = Stroke(1.dp.toPx()))
                }
            }.clickable(role = Role.Button, onClickLabel = "Balikkan kad") { showBack = !showBack }
                .heightIn(min = 206.dp).padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (angle <= 90f) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Kad Dompet", Modifier.weight(1f), color = CardIvory, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (frozen) "Dibekukan" else "Aktif", color = CardIvory, style = MaterialTheme.typography.labelLarge)
                }
                Text("••••   ••••   ••••   0001", color = CardIvory, fontSize = 21.sp)
                Text(name.ifBlank { "Pemilik kad" }, color = CardIvory, maxLines = 2, overflow = TextOverflow.Ellipsis)
            } else {
                Text("Kad demonstrasi", color = CardIvory, style = MaterialTheme.typography.titleLarge)
                Box(Modifier.fillMaxWidth().height(34.dp).background(Color.Black.copy(alpha = .35f)))
                Text("Untuk simulasi dalam aplikasi sahaja.", color = CardIvory)
            }
        }
        TextButton(onClick = { showBack = !showBack }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(if (showBack) "Lihat depan kad" else "Lihat belakang kad")
        }
        HorizontalDivider()
        Row(
            Modifier.fillMaxWidth().toggleable(value = frozen, role = Role.Switch, onValueChange = onFreeze)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text("Bekukan kad demo", style = MaterialTheme.typography.titleMedium)
                Text("Tetapan ini hanya melibatkan kad dalam aplikasi.", style = MaterialTheme.typography.bodyMedium)
            }
            Switch(checked = frozen, onCheckedChange = null)
        }
    }
}

@Composable
fun ProfileScreen(name: String, onSave: (String) -> Unit) {
    var input by rememberSaveable(name) { mutableStateOf(name) }
    val focus = LocalFocusManager.current
    val trimmed = input.trim()
    val valid = trimmed.isNotEmpty() && trimmed.length <= 60
    val canSave = valid && trimmed != name
    fun saveName() {
        if (canSave) {
            focus.clearFocus()
            onSave(trimmed)
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("Profil", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val initial = remember(name) {
                val text = name.trim()
                if (text.isEmpty()) "D" else {
                    val breaks = BreakIterator.getCharacterInstance(Locale.getDefault())
                    breaks.setText(text)
                    text.substring(0, breaks.next()).uppercase(Locale.getDefault())
                }
            }
            Box(Modifier.size(60.dp).background(CardMint, CircleShape), contentAlignment = Alignment.Center) {
                Text(initial, color = CardTeal, style = MaterialTheme.typography.headlineMedium)
            }
            Text(name.ifBlank { "Pemilik Dompet" }, style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider()
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Nama paparan") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = !valid && input != name,
            supportingText = {
                Text(when {
                    trimmed.isEmpty() && input != name -> "Masukkan nama."
                    trimmed.length > 60 -> "Gunakan maksimum 60 aksara."
                    else -> "Nama ini dipaparkan pada kad demo."
                })
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { saveName(); focus.clearFocus() })
        )
        Button(onClick = { saveName() }, enabled = canSave, modifier = Modifier.fillMaxWidth()) {
            Text("Simpan nama")
        }
    }
}
