package my.dompet.finance

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val paper = Color(0xFFFAF9F5)
private val pine = Color(0xFF104C46)
private val ink = Color(0xFF182B29)
private val muted = Color(0xFF526661)
private val mint = Color(0xFFE2EFE7)
private val line = Color(0xFFDAE1DA)
private val localeMs = Locale("ms", "MY")
private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", localeMs)
private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", localeMs)
private val categories = listOf("Makanan", "Barangan dapur", "Pengangkutan", "Bil", "Beli-belah", "Kesihatan", "Lain-lain")

private fun money(amount: BigDecimal): String = "RM " + NumberFormat.getNumberInstance(Locale.UK).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}.format(amount)

private fun amountInSen(input: String): Long? {
    val value = input.trim()
    if (!Regex("""[0-9]+(?:[.,][0-9]{1,2})?""").matches(value)) return null
    return runCatching { BigDecimal(value.replace(',', '.')).movePointRight(2).longValueExact() }
        .getOrNull()?.takeIf { it > 0 }
}

@Composable
fun LedgerScreen(expenses: List<Expense>, onAdd: () -> Unit, onScan: () -> Unit) {
    val current = YearMonth.now()
    var monthKey by rememberSaveable { mutableStateOf(current.toString()) }
    val selected = YearMonth.parse(monthKey)
    val latest = remember(expenses, current) {
        val lastDate = expenses.maxOfOrNull { it.date }.orEmpty()
        maxOf(current, runCatching { YearMonth.from(LocalDate.parse(lastDate)) }.getOrDefault(current))
    }
    val visible = remember(expenses, monthKey) {
        expenses.filter { it.date.startsWith(monthKey + "-") }
            .sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.id })
    }
    val total = remember(visible) {
        visible.fold(BigDecimal.ZERO) { sum, expense -> sum + BigDecimal.valueOf(expense.amountSen, 2) }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(paper),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp)
    ) {
        item {
            Text("Perbelanjaan", color = ink, fontFamily = FontFamily.Serif, fontSize = 34.sp, lineHeight = 42.sp)
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(pine)) {
                Canvas(Modifier.matchParentSize()) {
                    val center = Offset(size.width * 1.08f, size.height * 0.10f)
                    for (index in 1..7) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.09f),
                            radius = size.height * (0.19f + 0.14f * index),
                            center = center, style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }
                Column(Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(selected.format(monthFormat), modifier = Modifier.weight(1f),
                            color = Color.White, style = MaterialTheme.typography.titleMedium)
                        MonthArrow(true, true) { monthKey = selected.minusMonths(1).toString() }
                        MonthArrow(false, selected < latest) { monthKey = selected.plusMonths(1).toString() }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Jumlah belanja", color = Color(0xFFCCE1D8), style = MaterialTheme.typography.bodyMedium)
                    Text(money(total), color = Color.White, fontWeight = FontWeight.Medium,
                        fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-1).sp)
                    Spacer(Modifier.height(10.dp))
                    Text("${visible.size} transaksi", color = Color(0xFFCCE1D8), style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onAdd, modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                    shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = pine),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)) {
                    Text("Tambah belanja", color = Color.White)
                }
                OutlinedButton(onClick = onScan, modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                    shape = RoundedCornerShape(13.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)) {
                    Text("Imbas resit QR", color = pine)
                }
            }
            Spacer(Modifier.height(29.dp))
            Text("Transaksi", color = ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
        }
        if (visible.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(48.dp).background(mint, CircleShape), contentAlignment = Alignment.Center) {
                        ReceiptGlyph()
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Belum ada transaksi bulan ini", color = ink, style = MaterialTheme.typography.titleSmall)
                    Text("Tambah belanja atau imbas kod QR resit.", color = muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            items(visible, key = { it.id }) { expense ->
                Row(Modifier.fillMaxWidth().padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).background(mint, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        ReceiptGlyph()
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(expense.merchant, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            color = ink, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
                        val readableDate = runCatching { LocalDate.parse(expense.date).format(dateFormat) }.getOrDefault(expense.date)
                        Text("${expense.category} · $readableDate", color = muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(money(BigDecimal.valueOf(expense.amountSen, 2)), color = ink, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(line))
            }
        }
    }
}

@Composable
private fun ReceiptGlyph() {
    Canvas(Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val shape = Path().apply {
            moveTo(.21f * w, .14f * h); lineTo(.79f * w, .14f * h); lineTo(.79f * w, .86f * h)
            lineTo(.65f * w, .76f * h); lineTo(.50f * w, .86f * h); lineTo(.35f * w, .76f * h)
            lineTo(.21f * w, .86f * h); close()
        }
        drawPath(shape, pine, style = Stroke(width = 1.5.dp.toPx()))
        drawLine(pine, Offset(.34f * w, .36f * h), Offset(.66f * w, .36f * h), 1.4.dp.toPx())
        drawLine(pine, Offset(.34f * w, .53f * h), Offset(.57f * w, .53f * h), 1.4.dp.toPx())
    }
}

@Composable
private fun MonthArrow(previous: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val description = if (previous) "Bulan sebelumnya" else "Bulan seterusnya"
    IconButton(onClick, Modifier.size(48.dp).semantics { contentDescription = description }, enabled = enabled) {
        Canvas(Modifier.size(16.dp)) {
            val a = if (previous) .65f else .35f
            val b = 1f - a
            val c = Color.White.copy(alpha = if (enabled) 1f else .30f)
            drawLine(c, Offset(a * size.width, .19f * size.height), Offset(b * size.width, .50f * size.height), 2.dp.toPx())
            drawLine(c, Offset(b * size.width, .50f * size.height), Offset(a * size.width, .81f * size.height), 2.dp.toPx())
        }
    }
}

@Composable
fun ExpenseEditor(draft: ExpenseDraft, onSave: (Expense) -> Unit, onClose: () -> Unit) {
    var merchant by rememberSaveable(draft) { mutableStateOf(draft.merchant) }
    var amount by rememberSaveable(draft) { mutableStateOf(draft.amount) }
    var dateText by rememberSaveable(draft) { mutableStateOf(draft.date) }
    var category by rememberSaveable(draft) { mutableStateOf(categories.first()) }
    var menu by remember { mutableStateOf(false) }
    var checked by rememberSaveable(draft) { mutableStateOf(false) }
    val context = LocalContext.current
    val sen = amountInSen(amount)
    val parsedDate = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = paper) {
            Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState())
                .padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClose) { Text("Tutup", color = pine) }
                }
                Text("Tambah belanja", color = ink, fontFamily = FontFamily.Serif, fontSize = 31.sp)
                if (draft.note.isNotBlank()) {
                    Text(draft.note, color = pine, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().background(mint, RoundedCornerShape(12.dp)).padding(14.dp))
                }
                OutlinedTextField(value = amount, onValueChange = { amount = it }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, label = { Text("Amaun (RM)") }, placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = checked && sen == null)
                if (checked && sen == null) Text("Masukkan amaun positif dengan maksimum dua tempat perpuluhan.",
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = merchant, onValueChange = { merchant = it }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, label = { Text("Nama kedai atau penerima") }, isError = checked && merchant.isBlank())
                if (checked && merchant.isBlank()) Text("Masukkan nama kedai atau penerima.",
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Text("Kategori", color = muted, style = MaterialTheme.typography.labelLarge)
                Box {
                    OutlinedButton(onClick = { menu = true }, shape = RoundedCornerShape(10.dp)) { Text("$category  ▾", color = ink) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        categories.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { category = option; menu = false })
                        }
                    }
                }
                OutlinedTextField(value = dateText, onValueChange = { dateText = it }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, label = { Text("Tarikh (YYYY-MM-DD)") }, isError = checked && parsedDate == null,
                    trailingIcon = {
                        TextButton(onClick = {
                            val initial = parsedDate ?: LocalDate.now()
                            DatePickerDialog(context, { _, y, m, d -> dateText = LocalDate.of(y, m + 1, d).toString() },
                                initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
                        }) { Text("Pilih", color = pine) }
                    })
                if (checked && parsedDate == null) Text("Pilih tarikh daripada kalendar atau guna format YYYY-MM-DD.",
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(2.dp))
                Button(onClick = {
                    checked = true
                    if (merchant.isNotBlank() && sen != null && parsedDate != null) {
                        onSave(Expense(merchant = merchant.trim(), amountSen = sen, date = parsedDate.toString(), category = category))
                    }
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = pine)) { Text("Simpan belanja", color = Color.White) }
            }
        }
    }
}
