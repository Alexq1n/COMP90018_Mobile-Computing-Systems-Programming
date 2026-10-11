package com.group5.roammate.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group5.roammate.R
import com.group5.roammate.ui.theme.RoamMateTheme

// code map:
//   back button    -> top left circle
//   avatar         -> fixed mascot
//   name field     -> editable name
//   save button    -> save and return

// colors
private val RoamMateTeal = Color(0xFF008B8F)
private val RoamMateLightTeal = Color(0xFFE6F5F3)
private val RoamMateText = Color(0xFF17212B)
private val RoamMateMutedText = Color(0xFF8A949E)
private val RoamMateFieldBorder = Color(0xFFE3E8EF)

// screen
@Composable
fun EditProfileScreen(
    initialName: String,
    onBackClick: () -> Unit,
    onSaveChangesClick: (String) -> Unit,
    onValidationError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // state
    var name by rememberSaveable { mutableStateOf(initialName) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
        ) {

            // back button
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = RoamMateLightTeal,
                            shape = CircleShape,
                        ),
                ) {
                    BackArrowIcon(
                        tint = RoamMateTeal,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // title
            Text(
                text = "Edit profile",
                color = RoamMateTeal,
                fontSize = 38.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(modifier = Modifier.height(34.dp))

            // avatar
            FixedMascotAvatar(
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(modifier = Modifier.height(38.dp))

            // label
            Text(
                text = "Name",
                color = RoamMateText,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(modifier = Modifier.height(10.dp))

            // name field
            NameInputField(
                name = name,
                onNameChange = { name = it },
            )

            Spacer(modifier = Modifier.height(38.dp))

            // save button
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    if (trimmedName.isBlank()) {
                        onValidationError("Please enter your name")
                    } else {
                        onSaveChangesClick(trimmedName)
                    }
                },
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
                    text = "Save changes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// avatar
@Composable
private fun FixedMascotAvatar(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(148.dp)
            .background(
                color = RoamMateLightTeal,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.roammate_wombat),
            contentDescription = "RoamMate mascot avatar",
            modifier = Modifier.size(108.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

// text field
@Composable
private fun NameInputField(
    name: String,
    onNameChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        leadingIcon = {
            PersonIcon(
                tint = RoamMateMutedText,
                modifier = Modifier.size(24.dp),
            )
        },
        textStyle = androidx.compose.ui.text.TextStyle(
            color = RoamMateText,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = RoamMateText,
            unfocusedTextColor = RoamMateText,
            focusedBorderColor = RoamMateTeal,
            unfocusedBorderColor = RoamMateFieldBorder,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            cursorColor = RoamMateTeal,
        ),
    )
}

// back icon
@Composable
private fun BackArrowIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawLine(
            color = tint,
            start = Offset(size.width * 0.62f, size.height * 0.22f),
            end = Offset(size.width * 0.36f, size.height * 0.50f),
            strokeWidth = 3.dp.toPx(),
        )
        drawLine(
            color = tint,
            start = Offset(size.width * 0.36f, size.height * 0.50f),
            end = Offset(size.width * 0.62f, size.height * 0.78f),
            strokeWidth = 3.dp.toPx(),
        )
    }
}

// person icon
@Composable
private fun PersonIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawPersonIcon(tint)
    }
}

// icon draw
private fun DrawScope.drawPersonIcon(
    tint: Color,
) {
    drawCircle(
        color = tint,
        radius = size.minDimension * 0.16f,
        center = Offset(size.width * 0.50f, size.height * 0.32f),
        style = Stroke(width = 2.dp.toPx()),
    )
    drawArc(
        color = tint,
        startAngle = 205f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = Offset(size.width * 0.25f, size.height * 0.50f),
        size = Size(size.width * 0.50f, size.height * 0.38f),
        style = Stroke(width = 2.dp.toPx()),
    )
}

// preview
@Preview(showBackground = true)
@Composable
private fun EditProfileScreenPreview() {
    RoamMateTheme(dynamicColor = false) {
        EditProfileScreen(
            initialName = "Yufei",
            onBackClick = {},
            onSaveChangesClick = {},
            onValidationError = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
