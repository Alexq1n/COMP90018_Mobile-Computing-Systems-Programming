package com.example.localdatebase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.localdatebase.ui.theme.LocalDatebaseTheme
import com.example.localdatebase.database.RecordData
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val model = ViewModelProvider(this)[RecordsViewModel::class.java]
        val cloudModel = ViewModelProvider(this)[CloudAccountViewModel::class.java]
        val plannerModel = ViewModelProvider(this)[PlannerViewModel::class.java]
        setContent {
            LocalDatebaseTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LocalRecordsScreen(model, cloudModel, plannerModel, Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
private fun LocalRecordsScreen(
    model: RecordsViewModel,
    cloudModel: CloudAccountViewModel,
    plannerModel: PlannerViewModel,
    modifier: Modifier = Modifier
) {
    var showCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var newCategory by rememberSaveable { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<RecordData?>(null) }
    var searchText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val total = model.categories.sumOf { it.count }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().testTag("recordsList"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("我的分类资料库", style = MaterialTheme.typography.headlineMedium)
            Text("本地保存 · ${model.categories.size} 个分类 · $total 条记录",
                style = MaterialTheme.typography.bodyMedium)
            Text("POI 数据库 · ${model.poiCount} 个地点",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary)
        }
        item { CloudAccountCard(cloudModel) }
        item { PlannerCard(plannerModel) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (model.editingId == null) "新增记录" else "编辑记录",
                        style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box {
                            OutlinedButton(onClick = { showPicker = true }, enabled = !model.busy) {
                                Text("分类：" + (model.categories.find { it.id == model.draftCategory }?.name ?: "加载中"))
                            }
                            DropdownMenu(expanded = showPicker, onDismissRequest = { showPicker = false }) {
                                model.categories.forEach { category ->
                                    DropdownMenuItem(text = { Text(category.name) }, onClick = {
                                        model.draftCategory = category.id
                                        showPicker = false
                                    })
                                }
                            }
                        }
                        TextButton(onClick = { showCategoryDialog = true }, enabled = !model.busy) {
                            Text("新建分类")
                        }
                    }
                    OutlinedTextField(
                        value = model.title, onValueChange = { if (it.length <= 100) model.title = it },
                        label = { Text("标题（必填）") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("recordTitle"), enabled = !model.busy
                    )
                    OutlinedTextField(
                        value = model.content, onValueChange = { if (it.length <= 10000) model.content = it },
                        label = { Text("内容") }, minLines = 3, maxLines = 6,
                        modifier = Modifier.fillMaxWidth().testTag("recordContent"), enabled = !model.busy
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { model.save() },
                            enabled = !model.busy && model.title.isNotBlank() && model.draftCategory != null,
                            modifier = Modifier.testTag("saveRecord")) {
                            Text(if (model.editingId == null) "保存记录" else "保存修改")
                        }
                        if (model.editingId != null) TextButton(onClick = { model.clearDraft() }, enabled = !model.busy) {
                            Text("取消编辑")
                        }
                    }
                }
            }
        }
        item {
            if (model.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (model.feedback.isNotEmpty()) Text(model.feedback, style = MaterialTheme.typography.bodyMedium)
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = model.selectedCategory == null, onClick = { model.filter(null) },
                    label = { Text("全部 ($total)") }, enabled = !model.busy)
                model.categories.forEach { category ->
                    FilterChip(selected = model.selectedCategory == category.id,
                        onClick = { model.filter(category.id) },
                        label = { Text("${category.name} (${category.count})") }, enabled = !model.busy)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = searchText, onValueChange = { searchText = it },
                    label = { Text("搜索标题或内容") }, singleLine = true,
                    modifier = Modifier.weight(1f), enabled = !model.busy)
                Button(onClick = { model.search(searchText) }, enabled = !model.busy) { Text("搜索") }
            }
            if (model.query.isNotEmpty()) TextButton(onClick = {
                searchText = ""
                model.search("")
            }, enabled = !model.busy) { Text("清除搜索") }
        }
        item {
            Text("记录 · ${model.records.size}", style = MaterialTheme.typography.titleMedium)
            if (!model.busy && model.records.isEmpty()) {
                Text(if (total == 0) "还没有记录，在上方填写并保存第一条吧。" else "该分类或搜索条件下没有记录。")
            }
        }
        items(model.records, key = { it.id }) { record ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(record.title, style = MaterialTheme.typography.titleMedium)
                    Text(record.categoryName, color = MaterialTheme.colorScheme.primary)
                    if (record.content.isNotEmpty()) Text(record.content)
                    Text("更新于 " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(record.updatedAt)),
                        style = MaterialTheme.typography.bodySmall)
                    Row {
                        TextButton(enabled = !model.busy, onClick = {
                            model.edit(record)
                            scope.launch { listState.animateScrollToItem(1) }
                        }) { Text("编辑") }
                        TextButton(enabled = !model.busy, onClick = { pendingDelete = record }) { Text("删除") }
                    }
                }
            }
        }
        item { Text("记录保存在本机，关闭应用后仍会保留。卸载应用或清除应用数据会移除本地记录。",
            style = MaterialTheme.typography.bodySmall) }
    }

    if (showCategoryDialog) AlertDialog(
        onDismissRequest = { if (!model.busy) showCategoryDialog = false },
        title = { Text("新建分类") },
        text = {
            Column {
                OutlinedTextField(value = newCategory, onValueChange = { if (it.length <= 30) newCategory = it },
                    label = { Text("分类名称") }, singleLine = true, enabled = !model.busy)
                if (model.feedback.isNotEmpty()) Text(model.feedback)
            }
        },
        confirmButton = {
            TextButton(enabled = !model.busy && newCategory.isNotBlank(), onClick = {
                model.addCategory(newCategory) { newCategory = ""; showCategoryDialog = false }
            }) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = { showCategoryDialog = false }, enabled = !model.busy) { Text("取消") } }
    )
    pendingDelete?.let { record ->
        AlertDialog(onDismissRequest = { pendingDelete = null }, title = { Text("删除记录？") },
            text = { Text("将删除“${record.title}”，此操作无法撤销。") },
            confirmButton = { TextButton(onClick = { pendingDelete = null; model.delete(record) }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } })
    }
}



