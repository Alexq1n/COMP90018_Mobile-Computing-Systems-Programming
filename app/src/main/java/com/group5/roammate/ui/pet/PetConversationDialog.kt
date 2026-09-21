package com.group5.roammate.ui.pet

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.group5.roammate.pet.PetDialogueEngine
import com.group5.roammate.pet.PetDialogueLanguage
import com.group5.roammate.pet.PetDialogueTopic
import com.group5.roammate.pet.PetTripContext
import com.group5.roammate.pet.PetUiState

/** A short, user-initiated conversation. No microphone is opened just by showing this dialog. */
@Composable
fun PetConversationDialog(
    state: PetUiState,
    tripContext: PetTripContext,
    isMoving: Boolean,
    onDismiss: () -> Unit,
    onReply: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var language by rememberSaveable { mutableStateOf(PetDialogueLanguage.English) }
    var useSystemSpeech by rememberSaveable { mutableStateOf(false) }
    var readReplies by rememberSaveable { mutableStateOf(true) }
    var draft by rememberSaveable { mutableStateOf("") }
    var lastQuestion by rememberSaveable { mutableStateOf("") }
    var lastReply by rememberSaveable { mutableStateOf("") }
    var previousTopic by remember { mutableStateOf<PetDialogueTopic?>(null) }
    var recognized by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var resumed by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var pendingListen by remember { mutableStateOf(false) }
    val controller = remember(context) {
        PetVoiceController(context) { utterance -> recognized = (recognized?.first?.plus(1) ?: 1) to utterance }
    }

    fun submit(value: String) {
        val clean = value.trim().take(500)
        if (clean.isEmpty()) return
        controller.stop()
        val reply = PetDialogueEngine.reply(clean, state, tripContext, isMoving, language, previousTopic)
        lastQuestion = clean
        lastReply = reply.text
        previousTopic = reply.topic
        draft = ""
        onReply(reply.text)
        if (readReplies) controller.speak(reply.text)
    }

    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pendingListen = true else controller.permissionDenied()
    }
    DisposableEffect(lifecycle, controller) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> { resumed = true; controller.resume() }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                    resumed = false
                    controller.pause()
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) controller.resume()
        onDispose { lifecycle.removeObserver(observer); controller.pause() }
    }
    LaunchedEffect(language) { controller.changeLanguage(language) }
    LaunchedEffect(recognized) { recognized?.let { submit(it.second) } }
    LaunchedEffect(pendingListen, resumed) {
        if (pendingListen && resumed) {
            pendingListen = false
            controller.startListening(useSystemSpeech)
        }
    }

    val teal = Color(0xFF008F95)
    val busy = controller.phase == PetVoicePhase.Listening || controller.phase == PetVoicePhase.Processing
    val close = { controller.stop(); onDismiss() }
    Dialog(onDismissRequest = close) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().imePadding().heightIn(max = 680.dp),
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(language.text("Talk with Buddy", "和 Buddy 聊聊"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = close) { Text(language.text("Done", "完成")) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = language == PetDialogueLanguage.English, onClick = { language = PetDialogueLanguage.English }, label = { Text("English") })
                    FilterChip(selected = language == PetDialogueLanguage.Chinese, onClick = { language = PetDialogueLanguage.Chinese }, label = { Text("中文") })
                }
                if (lastReply.isEmpty()) {
                    Text(language.text("Weather, outfits, our next stop—or a little encouragement.", "聊聊天气、穿搭、下一站，或者给你一点鼓励。"), style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(language.text("You: ", "你：") + lastQuestion, style = MaterialTheme.typography.bodySmall)
                    Surface(color = Color(0xFFEAF6F5), shape = RoundedCornerShape(16.dp)) {
                        Text(lastReply, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { submit(language.text("What's the weather?", "今天天气怎么样？")) }) { Text(language.text("Weather", "天气")) }
                    TextButton(onClick = { submit(language.text("Where do we go next?", "下一站去哪？")) }) { Text(language.text("Next stop", "下一站")) }
                    TextButton(onClick = { submit(language.text("I'm tired", "我有点累")) }) { Text(language.text("A break", "休息")) }
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(500) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(language.text("Message Buddy", "给 Buddy 发消息")) },
                    minLines = 1,
                    maxLines = 3,
                )
                if (controller.partialText.isNotBlank()) Text(controller.partialText, style = MaterialTheme.typography.bodySmall, color = teal)
                if (controller.notice.isNotBlank()) Text(controller.notice, style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (controller.phase) {
                                PetVoicePhase.Listening -> controller.finishListening()
                                PetVoicePhase.Processing -> controller.stop()
                                else -> {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        pendingListen = true
                                    } else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        enabled = resumed && (busy || if (useSystemSpeech) controller.systemRecognitionAvailable else controller.offlineRecognitionAvailable),
                    ) {
                        Text(when (controller.phase) {
                            PetVoicePhase.Listening -> language.text("Finish", "说完了")
                            PetVoicePhase.Processing -> language.text("Cancel", "取消")
                            else -> language.text("Speak", "说话")
                        })
                    }
                    Button(
                        onClick = { submit(draft) }, enabled = draft.isNotBlank(), modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = teal),
                    ) { Text(language.text("Send", "发送")) }
                }
                if (controller.phase == PetVoicePhase.Listening) Text(language.text("Listening…", "正在听你说…"), color = teal)
                if (controller.phase == PetVoicePhase.Processing) Text(language.text("Recognising…", "正在识别…"), color = teal)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(language.text("Read replies aloud", "朗读回复"), modifier = Modifier.weight(1f).padding(top = 12.dp))
                    Switch(checked = readReplies, onCheckedChange = { readReplies = it; if (!it) controller.stop() })
                }
                if (controller.phase == PetVoicePhase.Speaking) TextButton(onClick = controller::stop) { Text(language.text("Stop reading", "停止朗读")) }
                if (!controller.offlineRecognitionAvailable) Text(
                    language.text("On-device speech isn't available on this phone. Typing always works.", "这台手机暂不支持端侧语音识别，仍可输入文字。"),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (controller.systemRecognitionAvailable) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(language.text("Use system speech service", "使用系统语音服务"), modifier = Modifier.weight(1f).padding(top = 12.dp))
                        Switch(checked = useSystemSpeech, onCheckedChange = { controller.stop(); useSystemSpeech = it })
                    }
                    Text(
                        if (useSystemSpeech) language.text(
                            "The phone's selected provider may send speech to its servers and need internet. Turn this off to use on-device recognition only.",
                            "手机选用的服务商可能将语音发往其服务器并需要网络。关闭后仅使用端侧识别。",
                        ) else language.text("Speech stays on-device in this mode. A language model must be installed on the phone.", "此模式在手机端识别，需要已安装相应语言模型。"),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
