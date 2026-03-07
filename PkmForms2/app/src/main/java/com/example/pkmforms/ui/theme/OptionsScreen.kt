package com.example.pkmforms.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OptionsScreen(
    onBack: () -> Unit,
    onOpenCodeFile: () -> Unit,
    onSaveCodeFile: () -> Unit,
    onOpenFormFile: () -> Unit,
    onSaveFormFile: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Primary),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Options",
            color = AppColors.Text,
            fontSize = 22.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        OptionButton(label = "Open code file", onClick = onOpenCodeFile)
        Spacer(modifier = Modifier.height(12.dp))
        OptionButton(label = "Save code file", onClick = onSaveCodeFile)
        Spacer(modifier = Modifier.height(12.dp))
        OptionButton(label = "Open form file", onClick = onOpenFormFile)
        Spacer(modifier = Modifier.height(12.dp))
        OptionButton(label = "Save form file", onClick = onSaveFormFile)
        Spacer(modifier = Modifier.height(12.dp))
        OptionButton(label = "Finish", onClick = onFinish)
        Spacer(modifier = Modifier.height(12.dp))
        OptionButton(label = "Back", onClick = onBack)
    }
}

@Composable
private fun OptionButton(
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(240.dp)
            .height(48.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.OptionButton
        )
    ) {
        Text(text = label, color = AppColors.Text, fontSize = 14.sp)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OptionsScreenPreview() {
    OptionsScreen(
        onBack = {},
        onOpenCodeFile = {},
        onSaveCodeFile = {},
        onOpenFormFile = {},
        onSaveFormFile = {},
        onFinish = {}
    )
}