package com.example.localdatebase

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.localdatebase.database.CategoryData
import com.example.localdatebase.database.CategoryRecordStore
import com.example.localdatebase.database.LocalDatabaseProvider
import com.example.localdatebase.database.RecordData
import com.example.localdatebase.poi.PoiRepository
import java.util.concurrent.Executors

class RecordsViewModel(application: Application) : AndroidViewModel(application) {
    private val database: CategoryRecordStore = LocalDatabaseProvider.records(application)
    private val poiRepository = PoiRepository(application)
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var disposed = false
    var categories by mutableStateOf<List<CategoryData>>(emptyList())
        private set
    var records by mutableStateOf<List<RecordData>>(emptyList())
        private set
    var poiCount by mutableStateOf(0)
        private set
    var selectedCategory by mutableStateOf<Long?>(null)
        private set
    var query by mutableStateOf("")
        private set
    var busy by mutableStateOf(false)
        private set
    var feedback by mutableStateOf("")
        private set
    var title by mutableStateOf("")
    var content by mutableStateOf("")
    var draftCategory by mutableStateOf<Long?>(null)
    var editingId by mutableStateOf<Long?>(null)
        private set

    init { runOperation { poiRepository.importBundledPois() } }

    // One queue performs all database work off the UI thread and closes it last.
    private fun runOperation(message: String = "", onSuccess: () -> Unit = {}, action: () -> Unit = {}) {
        if (busy) return
        busy = true
        val filter = selectedCategory
        val search = query
        executor.execute {
            try {
                action()
                val nextCategories = database.getCategories()
                val nextRecords = database.getRecords(filter, search)
                val nextPoiCount = poiRepository.count()
                main.post {
                    if (!disposed) {
                        categories = nextCategories
                        records = nextRecords
                        poiCount = nextPoiCount
                        if (draftCategory == null) draftCategory = nextCategories.firstOrNull()?.id
                        busy = false
                        feedback = message
                        onSuccess()
                    }
                }
            } catch (error: Exception) {
                main.post {
                    if (!disposed) {
                        busy = false
                        feedback = if (error is SQLiteConstraintException)
                            "保存失败：分类名称已存在或所选分类无效。"
                        else "操作失败：${error.localizedMessage ?: "请重试"}"
                    }
                }
            }
        }
    }

    fun refresh() = runOperation()
    fun filter(category: Long?) {
        if (busy) return
        selectedCategory = category
        refresh()
    }
    fun search(text: String) {
        if (busy) return
        query = text
        refresh()
    }
    fun addCategory(name: String, onSuccess: () -> Unit) {
        var newId = 0L
        runOperation("分类已创建", {
            draftCategory = newId
            onSuccess()
        }) { newId = database.addCategory(name) }
    }
    fun save() {
        val category = draftCategory ?: return
        val id = editingId
        val savedTitle = title
        val savedContent = content
        runOperation("已保存到本机数据库", { clearDraft() }) {
            database.saveRecord(id, category, savedTitle, savedContent)
        }
    }
    fun edit(record: RecordData) {
        editingId = record.id
        draftCategory = record.categoryId
        title = record.title
        content = record.content
    }
    fun clearDraft() {
        editingId = null
        title = ""
        content = ""
    }
    fun delete(record: RecordData) = runOperation("记录已删除", {
        if (editingId == record.id) clearDraft()
    }) { database.deleteRecord(record.id) }

    override fun onCleared() {
        disposed = true
        executor.shutdown()
    }
}
