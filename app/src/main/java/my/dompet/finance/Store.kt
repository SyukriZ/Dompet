package my.dompet.finance

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String,
    val amountSen: Long,
    val category: String,
    val date: String
)

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<Expense>>

    @Insert
    suspend fun add(expense: Expense)
}

@Database(entities = [Expense::class], version = 1, exportSchema = false)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
}

class DompetModel(app: Application) : AndroidViewModel(app) {
    private val database = Room.databaseBuilder(app, LedgerDatabase::class.java, "dompet.db").build()
    private val prefs = app.getSharedPreferences("profile", Application.MODE_PRIVATE)
    val expenses = database.expenseDao().observeAll()
    var name = MutableStateFlow(prefs.getString("name", "").orEmpty())
        private set
    var frozen = MutableStateFlow(prefs.getBoolean("frozen", false))
        private set
    val saving = MutableStateFlow(false)
    val saveError = MutableStateFlow(false)
    val draft = MutableStateFlow<ExpenseDraft?>(null)

    fun updateName(value: String) {
        prefs.edit().putString("name", value).apply()
        name.value = value
    }
    fun updateFrozen(value: Boolean) {
        prefs.edit().putBoolean("frozen", value).apply()
        frozen.value = value
    }
    fun edit(value: ExpenseDraft?) {
        if (saving.value) return
        saveError.value = false
        draft.value = value
    }
    fun save(expense: Expense) {
        if (draft.value == null || !saving.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                database.expenseDao().add(expense)
                draft.value = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                saveError.value = true
            } finally {
                saving.value = false
            }
        }
    }

    override fun onCleared() {
        database.close()
    }
}
