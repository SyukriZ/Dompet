package my.dompet.finance

import org.json.JSONObject
import java.math.BigDecimal
import java.net.URI
import java.net.URLDecoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

data class ExpenseDraft(
    val merchant: String = "",
    val amount: String = "",
    val date: String = LocalDate.now().toString(),
    val note: String = ""
)

private val totalKeys = setOf("total", "grandtotal", "amount", "jumlah", "jumlahakhir", "totalamount")
private val merchantKeys = setOf("merchant", "merchantname", "store", "shop", "kedai", "peniaga")
private val dateKeys = setOf("date", "tarikh", "receiptdate")
private val currencyKeys = setOf("currency", "currencycode", "matauang", "matawang")
private val rmNumber = Regex("""(?i)(?:RM|MYR)?\s*((?:0|[1-9]\d{0,8}|[1-9]\d{0,2}(?:,\d{3}){1,2})(?:\.\d{1,2})?)""")
private val fieldLine = Regex("""^([\p{L} _-]{1,28})\s*[:=]\s*(.+)$""")
private val totalLine = Regex("""(?i)^(grand total|total amount|total|jumlah akhir|jumlah)\s+(.+)$""")
private val currencyLine = Regex("""(?i)^(?:RM|MYR)\s*\d[\d.,]*$""")
private val foreignCurrency = Regex("""(?i)(?:\p{Sc}|\b(?:USD|SGD|EUR|GBP|JPY|CNY|AUD|CAD|HKD|THB|IDR|INR|PHP|KRW|VND)(?=\b|\d))""")
private val localDateFormat = DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT)

private fun keyName(value: String) = value.trim().lowercase(Locale.ROOT).replace(Regex("[\\s_-]"), "")

private fun parseAmount(value: String): String? {
    val number = rmNumber.matchEntire(value.trim())?.groupValues?.get(1) ?: return null
    val decimal = number.replace(",", "").toBigDecimalOrNull() ?: return null
    return decimal.takeIf { it > BigDecimal.ZERO }?.setScale(2)?.toPlainString()
}

/** Reads the QR payload locally. Any resulting values are only suggestions for the editor. */
fun parseReceipt(raw: String): ExpenseDraft {
    val manual = ExpenseDraft(note = "Isi butiran resit secara manual.")
    val content = raw.trim()
    if (content.isEmpty() || content.length > 8192) return manual
    val isLink = content.startsWith("https://", true) || content.startsWith("http://", true)
    val fields: List<Pair<String, String>> = when {
        isLink -> runCatching {
            val query = URI(content).rawQuery.orEmpty()
            query.split('&').mapNotNull { part ->
                val separator = part.indexOf('=')
                if (separator < 1) return@mapNotNull null
                URLDecoder.decode(part.take(separator), "UTF-8") to
                    URLDecoder.decode(part.substring(separator + 1), "UTF-8")
            }
        }.getOrDefault(emptyList())
        content.startsWith('{') -> runCatching {
            val json = JSONObject(content)
            json.keys().asSequence().filterIsInstance<String>().mapNotNull { key ->
                when (val value = json.opt(key)) {
                    is String, is Number -> key to value.toString()
                    else -> null
                }
            }.toList()
        }.getOrDefault(emptyList())
        else -> content.lineSequence().mapNotNull { line ->
            val clean = line.trim()
            val match = fieldLine.matchEntire(clean) ?: totalLine.matchEntire(clean)
            when {
                match != null -> match.groupValues[1] to match.groupValues[2]
                currencyLine.matches(clean) -> "unlabelled" to clean
                else -> null
            }
        }.toList()
    }
    val pairs = fields.map { (key, value) -> keyName(key) to value.trim() }
    fun values(keys: Set<String>) = pairs.filter { it.first in keys }.map { it.second }
    val totals = values(totalKeys).ifEmpty { values(setOf("unlabelled")) }
    val currencies = values(currencyKeys)
    val isForeign = currencies.any { it.uppercase(Locale.ROOT) !in setOf("MYR", "RM") } ||
        totals.any { foreignCurrency.containsMatchIn(it) } ||
        (!isLink && !content.startsWith('{') && foreignCurrency.containsMatchIn(content))
    val amount = if (isForeign) null else totals.map(::parseAmount).distinct().singleOrNull()
    val merchant = values(merchantKeys).distinct().singleOrNull()?.take(100).orEmpty()
    val dateText = values(dateKeys).distinct().singleOrNull()
    val date = dateText?.let {
        runCatching { LocalDate.parse(it) }.getOrNull()
            ?: runCatching { LocalDate.parse(it, localDateFormat) }.getOrNull()
    }
    val note = when {
        isForeign -> "Mata wang resit perlu disemak. Masukkan amaun dalam RM."
        isLink && amount == null -> "QR mengandungi pautan. Semak resit dan masukkan jumlah dalam RM."
        amount == null -> "Jumlah belum dikenal pasti. Masukkan jumlah pada resit."
        date == null -> "Semak amaun dalam RM. Tarikh diisi dengan tarikh hari ini."
        else -> "Semak butiran dan pastikan jumlah dalam RM sebelum simpan."
    }
    return ExpenseDraft(merchant, amount.orEmpty(), (date ?: LocalDate.now()).toString(), note)
}
