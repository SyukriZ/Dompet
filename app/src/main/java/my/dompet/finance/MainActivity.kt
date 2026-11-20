package my.dompet.finance

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : ComponentActivity() {
    private val model by viewModels<DompetModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { Dompet(model) }
    }
}

@Composable
private fun Dompet(model: DompetModel) {
    val context = LocalContext.current
    var page by rememberSaveable { mutableStateOf("Belanja") }
    val name by model.name.collectAsState()
    val frozen by model.frozen.collectAsState()
    val expenses by model.expenses.collectAsState(emptyList())
    val draft by model.draft.collectAsState()
    val saveError by model.saveError.collectAsState()
    val scan = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { model.edit(parseReceipt(it)) }
    }
    fun launchScan() = scan.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        .setPrompt("Halakan kamera pada QR resit").setBeepEnabled(false).setOrientationLocked(false))
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchScan() else model.edit(ExpenseDraft(note = "Untuk mengimbas, benarkan kamera dalam tetapan aplikasi. Kau juga boleh isi butiran di sini."))
    }
    val scheme = lightColorScheme(primary = Color(0xFF15564A), onPrimary = Color.White,
        surface = Color(0xFFFAF9F5), background = Color(0xFFFAF9F5), onSurface = Color(0xFF182B29),
        secondaryContainer = Color(0xFFDCEDE2), onSecondaryContainer = Color(0xFF14473A))
    MaterialTheme(colorScheme = scheme) {
        BackHandler(enabled = page != "Belanja" && draft == null) { page = "Belanja" }
        Scaffold(bottomBar = {
            NavigationBar(containerColor = scheme.surface) {
                listOf("Belanja" to Icons.Default.List, "Kad" to Icons.Default.AccountBox,
                    "Profil" to Icons.Default.Person).forEach { (label, icon) ->
                    NavigationBarItem(selected = page == label, onClick = { page = label },
                        icon = { Icon(icon, contentDescription = null) }, label = { Text(label) })
                }
            }
        }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (page) {
                    "Belanja" -> LedgerScreen(expenses, onAdd = { model.edit(ExpenseDraft()) }, onScan = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchScan()
                        else permission.launch(Manifest.permission.CAMERA)
                    })
                    "Kad" -> CardScreen(name, frozen, model::updateFrozen)
                    else -> ProfileScreen(name, model::updateName)
                }
            }
        }
        draft?.let { ExpenseEditor(it, model::save) { model.edit(null) } }
        if (saveError) {
            AlertDialog(onDismissRequest = { model.saveError.value = false },
                text = { Text("Belanja gagal disimpan. Cuba lagi.") },
                confirmButton = { TextButton(onClick = { model.saveError.value = false }) { Text("OK") } })
        }
    }
}
