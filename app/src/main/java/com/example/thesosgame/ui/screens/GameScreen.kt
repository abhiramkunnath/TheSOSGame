package com.example.thesosgame.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thesosgame.data.*
import com.example.thesosgame.game.GameLogic

@Composable
fun GameScreen(
    gameConfig: GameConfig,
    onBackToOnboarding: () -> Unit
) {
    val gameLogic = remember { GameLogic() }
    var gameState by remember { 
        mutableStateOf(gameLogic.initializeGame(gameConfig))
    }
    var selectedCellValue by remember { mutableStateOf(CellValue.S) }
    var showRestartConfirmation by remember { mutableStateOf(false) }
    var showNewGameConfirmation by remember { mutableStateOf(false) }
    var lastMoveScored by remember { mutableStateOf(false) }
    var showScoredToast by remember { mutableStateOf(false) }
    
    // Auto-hide toast after 2 seconds
    LaunchedEffect(showScoredToast) {
        if (showScoredToast) {
            kotlinx.coroutines.delay(2000)
            showScoredToast = false
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .padding(top = 32.dp) // Add top padding to avoid notification panel
        ) {
            // Top Bar with game info
            GameHeader(
                gameState = gameState,
                onRestart = { 
                    showRestartConfirmation = true
                },
                onBackToOnboarding = {
                    showNewGameConfirmation = true
                }
            )
            
            // Score Board
            ScoreBoard(gameState = gameState)
            
            // Game Board - takes up remaining space
            Box(
                modifier = Modifier.weight(1f)
            ) {
                GameBoard(
                    gameState = gameState,
                    selectedCellValue = selectedCellValue,
                    onCellClick = { row, col ->
                        val previousPatternCount = gameState.sosPatterns.size
                        val newGameState = gameLogic.makeMove(gameState, row, col, selectedCellValue)
                        val newPatternCount = newGameState.sosPatterns.size
                        val scored = newPatternCount > previousPatternCount
                        lastMoveScored = scored
                        if (scored) {
                            showScoredToast = true
                        }
                        gameState = newGameState
                    }
                )
            }
            
            // Current player and S/O selection - moved to bottom
            CurrentPlayerSection(
                gameState = gameState,
                selectedCellValue = selectedCellValue,
                onCellValueSelected = { selectedCellValue = it }
            )
        }
        
        // Floating Toast for scoring notification
        AnimatedVisibility(
            visible = showScoredToast,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
        ) {
            if (gameState.players.isNotEmpty()) {
                val currentPlayer = gameState.players[gameState.currentPlayerIndex]
                ScoredToast(playerColor = currentPlayer.color)
            }
        }
        
        // Game Over Dialog
        if (gameState.gameEnded) {
            GameOverDialog(
                winner = gameState.winner,
                players = gameState.players,
                onRestart = { 
                    gameState = gameLogic.initializeGame(gameConfig)
                    lastMoveScored = false
                    showScoredToast = false
                },
                onBackToOnboarding = onBackToOnboarding
            )
        }
        
        // Restart Confirmation Dialog
        if (showRestartConfirmation) {
            ConfirmationDialog(
                title = "Restart Game",
                message = "Are you sure you want to restart the current game? All progress will be lost.",
                onConfirm = {
                    gameState = gameLogic.initializeGame(gameConfig)
                    lastMoveScored = false
                    showScoredToast = false
                    showRestartConfirmation = false
                },
                onDismiss = {
                    showRestartConfirmation = false
                }
            )
        }
        
        // New Game Confirmation Dialog
        if (showNewGameConfirmation) {
            ConfirmationDialog(
                title = "New Game",
                message = "Are you sure you want to start a new game? Current progress will be lost.",
                onConfirm = {
                    showNewGameConfirmation = false
                    onBackToOnboarding()
                },
                onDismiss = {
                    showNewGameConfirmation = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameHeader(
    gameState: GameState,
    onRestart: () -> Unit,
    onBackToOnboarding: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SOS Game",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onBackToOnboarding) {
                    Text("New Game")
                }
                
                IconButton(onClick = onRestart) {
                    Icon(Icons.Default.Refresh, contentDescription = "Restart")
                }
            }
        }
    }
}

@Composable
private fun CurrentPlayerSection(
    gameState: GameState,
    selectedCellValue: CellValue,
    onCellValueSelected: (CellValue) -> Unit
) {
    if (!gameState.gameEnded) {
        val currentPlayer = gameState.players[gameState.currentPlayerIndex]
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${currentPlayer.name}'s Turn",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = currentPlayer.color
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Large S and O buttons spanning full width
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onCellValueSelected(CellValue.S) },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedCellValue == CellValue.S) {
                                currentPlayer.color
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (selectedCellValue == CellValue.S) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    ) {
                        Text(
                            text = "S",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Button(
                        onClick = { onCellValueSelected(CellValue.O) },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedCellValue == CellValue.O) {
                                currentPlayer.color
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (selectedCellValue == CellValue.O) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    ) {
                        Text(
                            text = "O",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameBoard(
    gameState: GameState,
    selectedCellValue: CellValue,
    onCellClick: (Int, Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gameState.boardSize),
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .wrapContentHeight(), // Remove aspectRatio to prevent overflow
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            userScrollEnabled = false // Disable scrolling
        ) {
            itemsIndexed(gameState.board.flatten()) { index, cell ->
                val row = index / gameState.boardSize
                val col = index % gameState.boardSize
                
                GameCell(
                    cell = cell,
                    gameState = gameState,
                    onClick = { onCellClick(row, col) }
                )
            }
        }
    }
}

@Composable
private fun GameCell(
    cell: Cell,
    gameState: GameState,
    onClick: () -> Unit
) {
    val backgroundColor = if (cell.value != CellValue.EMPTY && cell.playerId >= 0) {
        gameState.players[cell.playerId].color.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    // Keep the original border color - use the cell's original player color, not SOS players
    val borderColor = if (cell.value != CellValue.EMPTY && cell.playerId >= 0) {
        gameState.players[cell.playerId].color.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outline
    }
    
    Box(
        modifier = Modifier
            .aspectRatio(1f) // Force square aspect ratio
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                2.dp,
                borderColor,
                RoundedCornerShape(8.dp)
            )
            .clickable(enabled = cell.value == CellValue.EMPTY && !gameState.gameEnded) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // Draw strike-through lines behind the text
        if (cell.isPartOfSOS && cell.sosDirections.isNotEmpty()) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                // Make stroke width thinner for larger boards
                val strokeWidth = when {
                    gameState.boardSize <= 8 -> 6.dp.toPx()
                    gameState.boardSize <= 10 -> 4.dp.toPx()
                    gameState.boardSize <= 12 -> 3.dp.toPx()
                    else -> 2.dp.toPx() // For 13-14 size boards
                }
                cell.sosDirections.forEach { direction ->
                    val playerId = cell.sosDirectionPlayerMap[direction] ?: -1
                    if (playerId >= 0 && playerId < gameState.players.size) {
                        val sosColor = gameState.players[playerId].color
                        when (direction) {
                            SOSDirection.HORIZONTAL -> {
                                drawLine(
                                    color = sosColor,
                                    start = Offset(size.width * 0.1f, size.height / 2),
                                    end = Offset(size.width * 0.9f, size.height / 2),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                            SOSDirection.VERTICAL -> {
                                drawLine(
                                    color = sosColor,
                                    start = Offset(size.width / 2, size.height * 0.1f),
                                    end = Offset(size.width / 2, size.height * 0.9f),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                            SOSDirection.DIAGONAL_DOWN_RIGHT -> {
                                drawLine(
                                    color = sosColor,
                                    start = Offset(size.width * 0.1f, size.height * 0.1f),
                                    end = Offset(size.width * 0.9f, size.height * 0.9f),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                            SOSDirection.DIAGONAL_DOWN_LEFT -> {
                                drawLine(
                                    color = sosColor,
                                    start = Offset(size.width * 0.9f, size.height * 0.1f),
                                    end = Offset(size.width * 0.1f, size.height * 0.9f),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Text rendered on top of strike-through lines
        if (cell.value != CellValue.EMPTY) {
            val hasStrikeThrough = cell.isPartOfSOS && cell.sosDirections.isNotEmpty()
            val textColor = if (cell.playerId >= 0) {
                gameState.players[cell.playerId].color
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            
            if (hasStrikeThrough) {
                // Create a stroke effect by rendering multiple text layers with slight offsets
                // Use the board's background color (MaterialTheme surface) for stroke
                val strokeColor = MaterialTheme.colorScheme.surface
                val strokeWidth = 1f
                
                Box {
                    // Stroke layers (8 directions for smooth outline)
                    for (dx in -1..1) {
                        for (dy in -1..1) {
                            if (dx != 0 || dy != 0) {
                                Text(
                                    text = cell.value.name,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = strokeColor,
                                    modifier = Modifier.offset(
                                        x = (dx * strokeWidth).dp,
                                        y = (dy * strokeWidth).dp
                                    )
                                )
                            }
                        }
                    }
                    
                    // Main text on top
                    Text(
                        text = cell.value.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor.copy(alpha = 1f)
                    )
                }
            } else {
                Text(
                    text = cell.value.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun ScoreBoard(gameState: GameState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp) // Reduced padding
    ) {
        Column(
            modifier = Modifier.padding(12.dp) // Reduced padding
        ) {
            Text(
                text = "Scores",
                fontSize = 16.sp, // Slightly smaller
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp) // Reduced bottom padding
            )
            
            // Use grid layout for many players to save vertical space
            if (gameState.players.size > 4) {
                // Grid layout for 5-6 players
                val rows = (gameState.players.size + 1) / 2 // 2 columns
                for (rowIndex in 0 until rows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp), // Minimal vertical padding
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (colIndex in 0 until 2) {
                            val playerIndex = rowIndex * 2 + colIndex
                            if (playerIndex < gameState.players.size) {
                                val player = gameState.players[playerIndex]
                                CompactPlayerScore(
                                    player = player,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                // Single column layout for 2-4 players
                gameState.players.forEach { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp), // Reduced vertical padding
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp) // Reduced spacing
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp) // Smaller color indicator
                                    .background(player.color, RoundedCornerShape(6.dp))
                            )
                            Text(
                                text = player.name,
                                fontSize = 14.sp, // Smaller font
                                color = player.color
                            )
                        }
                        
                        Text(
                            text = "${player.score}",
                            fontSize = 14.sp, // Smaller font
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactPlayerScore(
    player: Player,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp) // Very small color indicator
                .background(player.color, RoundedCornerShape(5.dp))
        )
        Text(
            text = player.name,
            fontSize = 12.sp, // Smaller font for compact layout
            color = player.color,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Text(
            text = "${player.score}",
            fontSize = 12.sp, // Smaller font
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GameOverDialog(
    winner: Player?,
    players: List<Player>,
    onRestart: () -> Unit,
    onBackToOnboarding: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                text = "Game Over!",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                if (winner != null) {
                    Text(
                        text = "${winner.name} wins with ${winner.score} points!",
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = winner.color,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "It's a tie!",
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Final Scores:",
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                players.sortedByDescending { it.score }.forEach { player ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = player.name,
                            color = player.color
                        )
                        Text(
                            text = "${player.score}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onBackToOnboarding) {
                    Text("New Game")
                }
                Button(onClick = onRestart) {
                    Text("Play Again")
                }
            }
        }
    )
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = message,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ScoredToast(
    playerColor: Color
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = playerColor.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🎉",
                fontSize = 20.sp
            )
            Text(
                text = "Scored! Play again!",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
