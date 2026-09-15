package com.group5.roammate.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group5.roammate.ui.theme.RoamMateTheme

// buttons: Apply · Regenerate/Back (one button, toggles) · Keep current

// colors
private val RoamMateTeal = Color(0xFF008B8F)
private val RoamMateText = Color(0xFF17212B)
private val RoamMateMutedText = Color(0xFF8A949E)
private val RoamMateFieldBorder = Color(0xFFE3E8EF)
private val RoamMateCoral = Color(0xFFFF6F61)
private val RoamMateLightCoral = Color(0xFFFFEEE9)
private val RoamMateGreen = Color(0xFF22A866)
private val RoamMateLightGreen = Color(0xFFE5F8EC)
private val RoamMateNeutralPill = Color(0xFFE9EEF0)

// data: one regenerated plan (from Zewen)
data class AdjustItineraryPlan(
    val reasonLabel: String,
    val reasonDescription: String,
    val stops: List<AdjustItineraryStop>,
    val removedStop: AdjustRemovedStop?,
)

// data: one stop in the new plan
data class AdjustItineraryStop(
    val time: String,
    val title: String,
    val changeLabel: String,
    val changeTone: AdjustChangeTone,
)

// data: dropped stop (weather etc.)
data class AdjustRemovedStop(
    val title: String,
    val reason: String,
)

// badge tone (gray = kept, green = changed/new)
enum class AdjustChangeTone {
    Neutral,
    Positive,
}

// ---- dialog screen ----
@Composable
fun AdjustItineraryScreen(
    plan: AdjustItineraryPlan,
    canRegenerateAnother: Boolean,   // true = show "Regenerate", false = show "Back to previous"
    onApplyPlanClick: (AdjustItineraryPlan) -> Unit,
    onRegenerateAnotherClick: () -> Unit,
    onBackToPreviousPlanClick: () -> Unit,
    onKeepCurrentPlanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // scrim (dim background)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x99000000)),
        contentAlignment = Alignment.Center,
    ) {
        // dialog card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            shadowElevation = 10.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 26.dp),
            ) {
                // title
                Text(
                    text = "Adjust for ${plan.reasonLabel.lowercase()}?",
                    color = RoamMateTeal,
                    fontSize = 26.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // subtitle
                Text(
                    text = plan.reasonDescription,
                    color = RoamMateMutedText,
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(modifier = Modifier.height(20.dp))

                // new plan card
                NewPlanCard(plan = plan)

                Spacer(modifier = Modifier.height(18.dp))

                // button: Apply
                Button(
                    onClick = { onApplyPlanClick(plan) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoamMateTeal,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        text = "Apply this plan",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // button: Regenerate (once) -> then Back to previous
                OutlinedButton(
                    onClick = {
                        if (canRegenerateAnother) {
                            onRegenerateAnotherClick()
                        } else {
                            onBackToPreviousPlanClick()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, RoamMateTeal),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = RoamMateTeal,
                    ),
                ) {
                    Text(
                        text = if (canRegenerateAnother) {
                            "Regenerate another"
                        } else {
                            "Back to previous plan"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // button: Keep current (close)
                TextButton(
                    onClick = onKeepCurrentPlanClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Keep current plan",
                        color = RoamMateMutedText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

// ---- new plan card (stops + removed) ----
@Composable
private fun NewPlanCard(
    plan: AdjustItineraryPlan,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, RoamMateTeal.copy(alpha = 0.7f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            // card title
            Text(
                text = "New plan for today",
                color = RoamMateTeal,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // stop rows
            plan.stops.forEach { stop ->
                AdjustPlanStopRow(stop = stop)
                Spacer(modifier = Modifier.height(10.dp))
            }

            // removed row (optional)
            plan.removedStop?.let { removedStop ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(RoamMateFieldBorder),
                )

                Spacer(modifier = Modifier.height(12.dp))

                RemovedStopRow(removedStop = removedStop)
            }
        }
    }
}

// ---- stop row (time + title + badge) ----
@Composable
private fun AdjustPlanStopRow(
    stop: AdjustItineraryStop,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stop.time,
            modifier = Modifier.weight(0.9f),
            color = RoamMateTeal,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
        )

        Text(
            text = stop.title,
            modifier = Modifier.weight(2.0f),
            color = RoamMateText,
            fontSize = 17.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.ExtraBold,
        )

        ChangeBadge(
            label = stop.changeLabel,
            tone = stop.changeTone,
        )
    }
}

// ---- change badge (gray = kept, green = new/moved) ----
@Composable
private fun ChangeBadge(
    label: String,
    tone: AdjustChangeTone,
) {
    // pick colors by tone
    val backgroundColor = when (tone) {
        AdjustChangeTone.Neutral -> RoamMateNeutralPill
        AdjustChangeTone.Positive -> RoamMateLightGreen
    }
    val textColor = when (tone) {
        AdjustChangeTone.Neutral -> RoamMateMutedText
        AdjustChangeTone.Positive -> RoamMateGreen
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = textColor,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
    }
}

// ---- removed row (red, strikethrough) ----
@Composable
private fun RemovedStopRow(
    removedStop: AdjustRemovedStop,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "-",
            color = RoamMateCoral,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
        )

        Text(
            text = removedStop.title,
            modifier = Modifier.weight(1f),
            color = RoamMateCoral,
            fontSize = 15.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            textDecoration = TextDecoration.LineThrough,
        )

        // reason pill
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = RoamMateLightCoral,
        ) {
            Text(
                text = removedStop.reason,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                color = RoamMateCoral,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// preview
@Preview(showBackground = true)
@Composable
private fun AdjustItineraryScreenPreview() {
    RoamMateTheme(dynamicColor = false) {
        AdjustItineraryScreen(
            plan = AdjustItineraryPlan(
                reasonLabel = "rain",
                reasonDescription = "Heavy rain 2-4 PM - here's a rewritten plan for today:",
                stops = listOf(
                    AdjustItineraryStop("10:00", "Melbourne Museum", "Kept", AdjustChangeTone.Neutral),
                    AdjustItineraryStop("12:30", "State Library", "Moved earlier", AdjustChangeTone.Positive),
                    AdjustItineraryStop("14:00", "ACMI", "New · indoor", AdjustChangeTone.Positive),
                    AdjustItineraryStop("16:30", "Queen Victoria Market", "New · covered", AdjustChangeTone.Positive),
                ),
                removedStop = AdjustRemovedStop(
                    title = "Royal Botanic Gardens",
                    reason = "outdoor · rain",
                ),
            ),
            canRegenerateAnother = true,
            onApplyPlanClick = {},
            onRegenerateAnotherClick = {},
            onBackToPreviousPlanClick = {},
            onKeepCurrentPlanClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}