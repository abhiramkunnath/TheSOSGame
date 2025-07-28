package com.example.thesosgame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thesosgame.data.GameConfig
import com.example.thesosgame.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onStartGame: (GameConfig) -> Unit
) {
    var selectedBoardSize by remember { mutableIntStateOf(8) }
    var customBoardSize by remember { mutableStateOf("") }
    var numberOfPlayers by remember { mutableIntStateOf(2) }
    var isCustomSize by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Primary.copy(alpha = 0.1f),
                        Secondary.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            // Modern Hero Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title with gradient effect
                Text(
                    text = "SOS",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Primary,
                    letterSpacing = 2.sp
                )
                
                Text(
                    text = "The Strategic Pattern Game",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Create SOS patterns to score points!\nFind horizontal, vertical, and diagonal sequences.",
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 24.sp
                )
            }
            
            // Modern Board Size Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Primary)
                        )
                        Text(
                            text = "Board Size",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Preset sizes with modern chips
                    val presetSizes = listOf(8, 10, 14)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        presetSizes.forEach { size ->
                            ModernChip(
                                text = "${size}×${size}",
                                selected = selectedBoardSize == size && !isCustomSize,
                                onClick = { 
                                    selectedBoardSize = size
                                    isCustomSize = false
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    // Custom size option
                    ModernChip(
                        text = "Custom Size",
                        selected = isCustomSize,
                        onClick = { isCustomSize = !isCustomSize },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (isCustomSize) {
                        ModernTextField(
                            value = customBoardSize,
                            onValueChange = { 
                                customBoardSize = it
                                val size = it.toIntOrNull()
                                if (size != null && size in 4..14) {
                                    selectedBoardSize = size
                                }
                            },
                            label = "Board Size",
                            placeholder = "Enter 4-14",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = {
                                val size = customBoardSize.toIntOrNull()
                                when {
                                    customBoardSize.isEmpty() -> "Enter a size between 4 and 14"
                                    size == null -> "Please enter a valid number"
                                    size !in 4..14 -> "Size must be between 4 and 14"
                                    else -> "Board will be ${size}×${size}"
                                }
                            },
                            isError = customBoardSize.isNotEmpty() && 
                                     (customBoardSize.toIntOrNull() == null || 
                                      customBoardSize.toIntOrNull()?.let { it !in 4..14 } == true)
                        )
                    }
                }
            }
            
            // Modern Players Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Secondary)
                        )
                        Text(
                            text = "Number of Players",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        (2..6).forEach { count ->
                            ModernChip(
                                text = "$count",
                                selected = numberOfPlayers == count,
                                onClick = { numberOfPlayers = count },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            
            // Modern Start Button
            Button(
                onClick = {
                    val finalBoardSize = if (isCustomSize && customBoardSize.isNotEmpty()) {
                        customBoardSize.toIntOrNull()?.takeIf { it in 4..14 } ?: selectedBoardSize
                    } else {
                        selectedBoardSize
                    }
                    onStartGame(GameConfig(finalBoardSize, numberOfPlayers))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Start Game",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ModernChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Primary else MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = if (selected) 8.dp else 2.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    supportingText: @Composable () -> String,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        keyboardOptions = keyboardOptions,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            focusedLabelColor = Primary,
            cursorColor = Primary
        ),
        supportingText = {
            Text(
                text = supportingText(),
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        isError = isError
    )
}
