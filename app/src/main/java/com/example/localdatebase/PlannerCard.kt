package com.example.localdatebase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PlannerCard(model: PlannerViewModel, modifier: Modifier = Modifier) {
    val trip = model.itinerary
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("RoamMate 行程算法")
            Text("数据来源：本机 SQLite POI · 用户规划偏好和行程：Firebase")
            if (trip != null) {
                val poiCount = trip.days.sumOf { it.pois.size }
                Text("行程 ${trip.tripId.take(8)} · ${trip.days.size} 天 · $poiCount 个地点")
                Text("预期价值 ${"%.1f".format(trip.totalExpectedValue)} · 费用 ${"%.1f".format(trip.totalEstimatedCost)}")
                trip.days.forEach { day ->
                    Text(
                        "第 ${day.dayNumber} 天 · ${day.date}",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                    )
                    day.pois.forEachIndexed { index, stop ->
                        Text(
                            "${index + 1}. ${stop.startTime}–${stop.endTime}  ${stop.poi.name}",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "   ${stop.poi.category.name} · 路程 ${stop.travelTimeFromPrevious} 分钟 · 评分 ${"%.1f".format(stop.expectedValue)}",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = model::generateMelbourneTrip, enabled = !model.busy) {
                    Text("生成墨尔本行程")
                }
                OutlinedButton(onClick = model::saveToCloud, enabled = !model.busy && trip != null) {
                    Text("保存到云端")
                }
            }
            OutlinedButton(onClick = model::loadCloudProfile, enabled = !model.busy) {
                Text("读取云端规划偏好")
            }
            if (model.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(model.feedback)
        }
    }
}
