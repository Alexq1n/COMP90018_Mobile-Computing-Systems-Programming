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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

// These colors match the Login page, so both auth pages feel like one design.
private val RoamMateTeal = Color(0xFF008B8F)
private val RoamMateCoral = Color(0xFFFF6F61)
private val RoamMateText = Color(0xFF17212B)
private val RoamMateMutedText = Color(0xFF8A949E)
private val RoamMateFieldBorder = Color(0xFFE3E8EF)

@Composable
fun CreateAccountScreen(
    onCreateAccountClick: (fullName: String, email: String, password: String) -> Unit,
    onLoginClick: () -> Unit,
    onBackClick: () -> Unit,
    onValidationError: (message: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // These states store what the user types into each text field.
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

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
            Spacer(modifier = Modifier.height(24.dp))

            // Back button: returns to the previous auth page.
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = RoamMateTeal.copy(alpha = 0.10f),
                            shape = CircleShape,
                        ),
                ) {
                    CreateAccountIconCanvas(
                        icon = CreateAccountIcon.Back,
                        tint = RoamMateTeal,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Mascot image drawn with Canvas to match the Login page.
            CreateAccountPetLogo(modifier = Modifier.size(84.dp))

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Create account",
                color = RoamMateTeal,
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 42.sp,
                textAlign = TextAlign.Center,
            )

            Text(
                text = "Start planning smarter trips",
                color = RoamMateMutedText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Full name will become part of the user's profile data.
            CreateAccountTextField(
                value = fullName,
                onValueChange = { fullName = it },
                placeholder = "Full name",
                leadingIcon = CreateAccountIcon.User,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Email will be used by Firebase Authentication later.
            CreateAccountTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "Email",
                leadingIcon = CreateAccountIcon.Email,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Password input can be hidden or shown with the eye icon.
            CreateAccountTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Password",
                leadingIcon = CreateAccountIcon.Lock,
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        CreateAccountIconCanvas(
                            icon = if (passwordVisible) {
                                CreateAccountIcon.EyeOff
                            } else {
                                CreateAccountIcon.Eye
                            },
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
                    imeAction = ImeAction.Next,
                ),
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Confirm password lets the page check whether both passwords match.
            CreateAccountTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = "Confirm password",
                leadingIcon = CreateAccountIcon.Lock,
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        CreateAccountIconCanvas(
                            icon = if (confirmPasswordVisible) {
                                CreateAccountIcon.EyeOff
                            } else {
                                CreateAccountIcon.Eye
                            },
                            tint = RoamMateMutedText,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                },
                visualTransformation = if (confirmPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
            )

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {
                    // Keep simple validation in the UI for now.
                    // Firebase registration will be connected outside this screen later.
                    when {
                        fullName.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() -> {
                            onValidationError("Please fill in all fields")
                        }

                        password != confirmPassword -> {
                            onValidationError("Passwords do not match")
                        }

                        else -> {
                            onCreateAccountClick(
                                fullName.trim(),
                                email.trim(),
                                password,
                            )
                        }
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
                    text = "Create account",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Small divider between create account and log in.
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Already have an account?",
                    color = RoamMateMutedText,
                    fontSize = 15.sp,
                )
                TextButton(onClick = onLoginClick) {
                    Text(
                        text = "Log in",
                        color = RoamMateCoral,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CreateAccountTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: CreateAccountIcon,
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
            CreateAccountIconCanvas(
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

@Composable
private fun CreateAccountPetLogo(modifier: Modifier = Modifier) {
    // Use the same image resource as LoginScreen for a consistent mascot.
    Image(
        painter = painterResource(id = R.drawable.roammate_wombat),
        contentDescription = "RoamMate wombat mascot",
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun CreateAccountIconCanvas(
    icon: CreateAccountIcon,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        when (icon) {
            CreateAccountIcon.Back -> drawBackIcon(tint)
            CreateAccountIcon.User -> drawUserIcon(tint)
            CreateAccountIcon.Email -> drawEmailIcon(tint)
            CreateAccountIcon.Lock -> drawLockIcon(tint)
            CreateAccountIcon.Eye -> drawEyeIcon(tint, showSlash = false)
            CreateAccountIcon.EyeOff -> drawEyeIcon(tint, showSlash = true)
        }
    }
}

private enum class CreateAccountIcon {
    Back,
    User,
    Email,
    Lock,
    Eye,
    EyeOff,
}

private fun DrawScope.drawBackIcon(tint: Color) {
    drawLine(
        color = tint,
        start = Offset(size.width * 0.62f, size.height * 0.18f),
        end = Offset(size.width * 0.28f, size.height * 0.50f),
        strokeWidth = 2.6.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = tint,
        start = Offset(size.width * 0.28f, size.height * 0.50f),
        end = Offset(size.width * 0.62f, size.height * 0.82f),
        strokeWidth = 2.6.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawUserIcon(tint: Color) {
    val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(
        color = tint,
        radius = size.minDimension * 0.18f,
        center = Offset(size.width * 0.5f, size.height * 0.32f),
        style = stroke,
    )
    drawArc(
        color = tint,
        startAngle = 205f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = Offset(size.width * 0.20f, size.height * 0.48f),
        size = Size(size.width * 0.60f, size.height * 0.62f),
        style = stroke,
    )
}

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
private fun CreateAccountScreenPreview() {
    RoamMateTheme(dynamicColor = false) {
        CreateAccountScreen(
            onCreateAccountClick = { _, _, _ -> },
            onLoginClick = {},
            onBackClick = {},
            onValidationError = {},
        )
    }
}
