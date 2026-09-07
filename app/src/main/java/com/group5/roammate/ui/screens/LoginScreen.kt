package com.group5.roammate.ui.screens

// LoginScreen 

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group5.roammate.R
import com.group5.roammate.ui.theme.RoamMateTheme

// RoamMate color
private val RoamMateTeal = Color(0xFF008B8F)
private val RoamMateCoral = Color(0xFFFF6F61)
private val RoamMateText = Color(0xFF17212B)
private val RoamMateMutedText = Color(0xFF8A949E)
private val RoamMateFieldBorder = Color(0xFFE3E8EF)

// The main body of the login page
@Composable
fun LoginScreen(
    onLoginClick: (email: String, password: String) -> Unit,
    onCreateAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Local status of the page: context + password
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    // Full-page layout
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // Mascot + Brand Name + Slogan
            TravelPetLogo(modifier = Modifier.size(88.dp))

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "RoamMate",
                color = RoamMateTeal,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 46.sp,
                textAlign = TextAlign.Center,
            )

            Text(
                text = "Travel smarter today",
                color = RoamMateMutedText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(44.dp))

            // email text
            LoginTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "Email",
                leadingIcon = LoginIcon.Email,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            )

            Spacer(modifier = Modifier.height(14.dp))

            // password text
            LoginTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Password",
                leadingIcon = LoginIcon.Lock,
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        LoginFieldIcon(
                            icon = if (passwordVisible) LoginIcon.EyeOff else LoginIcon.Eye,
                            tint = RoamMateMutedText,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                },
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Log in button
            Button(
                onClick = { onLoginClick(email, password) },
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
                    text = "Log in",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // “or” split line
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(RoamMateFieldBorder),
                )
                Text(
                    text = "or",
                    modifier = Modifier.padding(horizontal = 14.dp),
                    color = RoamMateMutedText.copy(alpha = 0.72f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(RoamMateFieldBorder),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Create account：Navigate to Registration Page
            TextButton(onClick = onCreateAccountClick) {
                Text(
                    text = "Create account",
                    color = RoamMateCoral,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// text
@Composable
private fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: LoginIcon,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp),
        singleLine = true,
        placeholder = {
            Text(
                text = placeholder,
                color = RoamMateMutedText,
            )
        },
        leadingIcon = {
            LoginFieldIcon(
                icon = leadingIcon,
                tint = RoamMateMutedText,
                modifier = Modifier.size(22.dp),
            )
        },
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RoamMateTeal,
            unfocusedBorderColor = RoamMateFieldBorder,
            focusedLeadingIconColor = RoamMateTeal,
            unfocusedLeadingIconColor = RoamMateMutedText,
            focusedTrailingIconColor = RoamMateTeal,
            unfocusedTrailingIconColor = RoamMateMutedText,
            focusedTextColor = RoamMateText,
            unfocusedTextColor = RoamMateText,
            cursorColor = RoamMateTeal,
        ),
    )
}

// The mascot image comes from res/drawable, so every page can use the same character.

@Composable
private fun TravelPetLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.roammate_wombat),
        contentDescription = "RoamMate wombat mascot",
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

// The form icons are still drawn with Canvas to avoid adding extra icon dependencies.

// The icon to be drawn
@Composable
private fun LoginFieldIcon(
    icon: LoginIcon,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        when (icon) {
            LoginIcon.Email -> drawEmailIcon(tint)
            LoginIcon.Lock -> drawLockIcon(tint)
            LoginIcon.Eye -> drawEyeIcon(tint, showSlash = false)
            LoginIcon.EyeOff -> drawEyeIcon(tint, showSlash = true)
        }
    }
}

// type of icon
private enum class LoginIcon {
    Email,
    Lock,
    Eye,
    EyeOff,
}

// email icon
private fun DrawScope.drawEmailIcon(tint: Color) {
    val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    drawRoundRect(
        color = tint,
        topLeft = Offset(size.width * 0.1f, size.height * 0.22f),
        size = Size(size.width * 0.8f, size.height * 0.58f),
        style = stroke,
    )
    drawLine(
        color = tint,
        start = Offset(size.width * 0.16f, size.height * 0.3f),
        end = Offset(size.width * 0.5f, size.height * 0.55f),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = tint,
        start = Offset(size.width * 0.84f, size.height * 0.3f),
        end = Offset(size.width * 0.5f, size.height * 0.55f),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

// lock icon
private fun DrawScope.drawLockIcon(tint: Color) {
    val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    drawRoundRect(
        color = tint,
        topLeft = Offset(size.width * 0.18f, size.height * 0.43f),
        size = Size(size.width * 0.64f, size.height * 0.42f),
        style = stroke,
    )
    drawArc(
        color = tint,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(size.width * 0.28f, size.height * 0.1f),
        size = Size(size.width * 0.44f, size.height * 0.62f),
        style = stroke,
    )
    drawCircle(
        color = tint,
        radius = size.minDimension * 0.05f,
        center = Offset(size.width * 0.5f, size.height * 0.62f),
    )
}

// eye icon
private fun DrawScope.drawEyeIcon(tint: Color, showSlash: Boolean) {
    val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    val eye = Path().apply {
        moveTo(size.width * 0.08f, size.height * 0.5f)
        quadraticBezierTo(size.width * 0.5f, size.height * 0.14f, size.width * 0.92f, size.height * 0.5f)
        quadraticBezierTo(size.width * 0.5f, size.height * 0.86f, size.width * 0.08f, size.height * 0.5f)
        close()
    }
    drawPath(path = eye, color = tint, style = stroke)
    drawCircle(
        color = tint,
        radius = size.minDimension * 0.12f,
        center = Offset(size.width * 0.5f, size.height * 0.5f),
        style = stroke,
    )
    if (showSlash) {
        drawLine(
            color = tint,
            start = Offset(size.width * 0.18f, size.height * 0.86f),
            end = Offset(size.width * 0.82f, size.height * 0.14f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}


@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    RoamMateTheme(dynamicColor = false) {
        LoginScreen(
            onLoginClick = { _, _ -> },
            onCreateAccountClick = {},
        )
    }
}
