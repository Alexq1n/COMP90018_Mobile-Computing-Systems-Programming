package com.example.localdatebase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun CloudAccountCard(model: CloudAccountViewModel, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("云端账号与用户偏好", style = MaterialTheme.typography.titleLarge)
            Text(
                if (model.session == null) "Firebase Authentication · 未登录"
                else "Firebase Authentication · ${model.session?.email}",
                color = MaterialTheme.colorScheme.primary
            )

            if (model.session == null) {
                OutlinedTextField(
                    value = model.email,
                    onValueChange = { model.email = it },
                    label = { Text("邮箱") },
                    singleLine = true,
                    enabled = !model.busy,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = model.password,
                    onValueChange = { model.password = it },
                    label = { Text("密码（至少 6 个字符）") },
                    singleLine = true,
                    enabled = !model.busy,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = model::signIn,
                        enabled = !model.busy && model.email.isNotBlank() && model.password.length >= 6
                    ) { Text("登录") }
                    OutlinedButton(
                        onClick = model::register,
                        enabled = !model.busy && model.email.isNotBlank() && model.password.length >= 6
                    ) { Text("注册") }
                }
                Text("密码只交给 Firebase Authentication，不会写入 Firestore 或本地数据库。",
                    style = MaterialTheme.typography.bodySmall)
            } else {
                OutlinedTextField(
                    value = model.theme,
                    onValueChange = { model.theme = it },
                    label = { Text("主题：system / light / dark") },
                    singleLine = true,
                    enabled = !model.busy,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = model.language,
                    onValueChange = { model.language = it },
                    label = { Text("语言，例如 zh-CN") },
                    singleLine = true,
                    enabled = !model.busy,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = model.preferredCategories,
                    onValueChange = { model.preferredCategories = it },
                    label = { Text("偏好分类，用逗号分隔") },
                    singleLine = true,
                    enabled = !model.busy,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("接收通知")
                    Switch(
                        checked = model.notificationsEnabled,
                        onCheckedChange = { model.notificationsEnabled = it },
                        enabled = !model.busy
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = model::savePreferences, enabled = !model.busy) {
                        Text("保存到云端")
                    }
                    OutlinedButton(onClick = { model.loadPreferences() }, enabled = !model.busy) {
                        Text("读取云端")
                    }
                    TextButton(onClick = model::signOut, enabled = !model.busy) { Text("退出") }
                }
            }

            if (model.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (model.feedback.isNotEmpty()) Text(model.feedback, style = MaterialTheme.typography.bodySmall)
        }
    }
}
