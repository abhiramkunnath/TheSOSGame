package com.example.thesosgame.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thesosgame.data.GameConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onStartGame: (GameConfig) -> Unit
) {
    var selectedBoardSize by remember { mutableIntStateOf(8) }
    var customBoardSize by remember { mutableStateOf("") }
    var numberOfPlayers by remember { mutableIntStateOf(2) }
    var isCustomSize by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // Title
        Text(
            text = "SOS Game",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Create SOS patterns to score points!\nFind horizontal, vertical, and diagonal SOS sequences.",
            textAlign = TextAlign.Center,
            fontSize = 16.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Board Size Selection
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Board Size",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Preset sizes
                val presetSizes = listOf(8, 10, 14)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetSizes.forEach { size ->
                        FilterChip(
                            onClick = { 
                                selectedBoardSize = size
                                isCustomSize = false
                            },
                            label = { Text("${size}×${size}") },
                            selected = selectedBoardSize == size && !isCustomSize,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Custom size option
                FilterChip(
                    onClick = { isCustomSize = !isCustomSize },
                    label = { Text("Custom") },
                    selected = isCustomSize,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (isCustomSize) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Custom Board Size",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            OutlinedTextField(
                                value = customBoardSize,
                                onValueChange = { 
                                    customBoardSize = it
                                    val size = it.toIntOrNull()
                                    if (size != null && size in 4..14) {
                                        selectedBoardSize = size
                                    }
                                },
                                label = { Text("Size (4-14)") },
                                placeholder = { Text("8") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                supportingText = {
                                    val size = customBoardSize.toIntOrNull()
                                    when {
                                        customBoardSize.isEmpty() -> Text("Enter a board size between 4 and 14")
                                        size == null -> Text("Please enter a valid number", color = MaterialTheme.colorScheme.error)
                                        size !in 4..14 -> Text("Size must be between 4 and 14", color = MaterialTheme.colorScheme.error)
                                        else -> Text("Board will be ${size}×${size}", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Number of Players Selection
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Number of Players",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (2..6).forEach { count ->
                        FilterChip(
                            onClick = { numberOfPlayers = count },
                            label = { Text("$count") },
                            selected = numberOfPlayers == count,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Start Game Button
        Button(
            onClick = { 
                val finalBoardSize = if (isCustomSize) {
                    customBoardSize.toIntOrNull()?.coerceIn(4, 14) ?: 8
                } else {
                    selectedBoardSize
                }
                
                onStartGame(
                    GameConfig(
                        boardSize = finalBoardSize,
                        numberOfPlayers = numberOfPlayers
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Start Game",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
