package com.example.thesosgame.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thesosgame.data.*
import com.example.thesosgame.game.GameLogic
import com.example.thesosgame.ui.theme.*
import com.example.thesosgame.utils.SoundManager
import com.example.thesosgame.utils.createSoundManager

@Composable
fun GameScreen(
    gameConfig: GameConfig,
    onBackToOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val soundManager = remember { context.createSoundManager() }
    val gameLogic = remember { GameLogic() }
    var gameState by remember { 
        mutableStateOf(gameLogic.initializeGame(gameConfig))
    }
    var selectedCellValue by remember { mutableStateOf(CellValue.S) }
    var showRestartConfirmation by remember { mutableStateOf(false) }
    var showNewGameConfirmation by remember { mutableStateOf(false) }
    var lastMoveScored by remember { mutableStateOf(false) }
    var showScoredToast by remember { mutableStateOf(false) }
    
    // Cleanup sound manager when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            soundManager.release()
        }
    }
    
    // Auto-hide toast after 2 seconds
    LaunchedEffect(showScoredToast) {
        if (showScoredToast) {
            kotlinx.coroutines.delay(2000)
            showScoredToast = false
        }
    }
    
    // Timer countdown
    LaunchedEffect(gameState.currentPlayerIndex, gameState.gameEnded) {
        if (!gameState.gameEnded && gameState.timerRunning) {
            var timeLeft = gameState.timeLeftInSeconds
            while (timeLeft > 0 && !gameState.gameEnded) {
                kotlinx.coroutines.delay(1000)
                timeLeft--
                gameState = gameLogic.updateTimer(gameState, timeLeft)
                
                // Check if game ended during this update
                if (gameState.gameEnded) break
            }
            // Time expired, switch to next player
            if (timeLeft <= 0 && !gameState.gameEnded) {
                gameState = gameLogic.handleTimerExpiration(gameState)
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        Primary.copy(alpha = 0.02f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(
                if (gameState.players.size > 4) 8.dp else 12.dp
            )
        ) {
            // Modern Top Bar
            ModernGameHeader(
                gameState = gameState,
                onRestart = { 
                    showRestartConfirmation = true
                },
                onBackToOnboarding = {
                    showNewGameConfirmation = true
                }
            )
            
            // Modern Score Board
            ModernScoreBoard(gameState = gameState)
            
            // Game Board - takes up remaining space
            Box(
                modifier = Modifier.weight(1f)
            ) {
                ModernGameBoard(
                    gameState = gameState,
                    selectedCellValue = selectedCellValue,
                    onCellClick = { row, col ->
                        val previousPatternCount = gameState.sosPatterns.size
                        val newGameState = gameLogic.makeMove(gameState, row, col, selectedCellValue)
                        val newPatternCount = newGameState.sosPatterns.size
                        val scored = newPatternCount > previousPatternCount
                        val multiplePatterns = (newPatternCount - previousPatternCount) > 1
                        
                        lastMoveScored = scored
                        if (scored) {
                            showScoredToast = true
                            // Play multiple chimes for multiple patterns
                            if (multiplePatterns) {
                                // Play multiple chimes with slight delay for multiple SOS patterns
                                repeat(newPatternCount - previousPatternCount) { index ->
                                    // Use LaunchedEffect for proper coroutine scope in the parent composable
                                    soundManager.playChime()
                                }
                            } else {
                                soundManager.playChime() // Single chime for one SOS pattern
                            }
                        }
                        gameState = newGameState
                    }
                )
            }
            
            // Modern Current player section
            ModernCurrentPlayerSection(
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
            // Play winner sound when game ends
            LaunchedEffect(gameState.gameEnded) {
                soundManager.playWinnerSound()
            }
            
            EnthusiasticGameOverDialog(
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
private fun EnthusiasticGameOverDialog(
    winner: Player?,
    players: List<Player>,
    onRestart: () -> Unit,
    onBackToOnboarding: () -> Unit
) {
    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "celebration")
    val starRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "star rotation"
    )
    
    val bounceScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutBounce),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce scale"
    )
    
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    AlertDialog(
        onDismissRequest = { },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Animated stars
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(3) { index ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .rotate(starRotation + (index * 120f))
                                .padding(4.dp),
                            tint = Color(0xFFFFD700) // Gold color
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Enthusiastic title with animation
                Text(
                    text = if (winner != null) "🎉 VICTORY! 🎉" else "🤝 TIE GAME! 🤝",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .scale(bounceScale)
                        .fillMaxWidth(),
                    color = if (winner != null) winner.color else MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column {
                if (winner != null) {
                    // Winner announcement with shimmer effect
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        winner.color.copy(alpha = 0.1f),
                                        winner.color.copy(alpha = 0.3f),
                                        winner.color.copy(alpha = 0.1f)
                                    ),
                                    start = Offset(shimmerOffset - 200f, 0f),
                                    end = Offset(shimmerOffset + 200f, 0f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${winner.name} is the Champion!",
                                textAlign = TextAlign.Center,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = winner.color,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "${winner.score} Points!",
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = winner.color,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    Text(
                        text = "🤝 Amazing game everyone! 🤝",
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Final scores with enhanced styling
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Final Scores",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        players.sortedByDescending { it.score }.forEachIndexed { index, player ->
                            val isWinner = player == winner
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(
                                        if (isWinner) player.color.copy(alpha = 0.1f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when (index) {
                                            0 -> "🥇"
                                            1 -> "🥈" 
                                            2 -> "🥉"
                                            else -> "${index + 1}."
                                        },
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    
                                    Text(
                                        text = player.name,
                                        color = player.color,
                                        fontWeight = if (isWinner) FontWeight.ExtraBold else FontWeight.Medium,
                                        fontSize = if (isWinner) 16.sp else 14.sp
                                    )
                                }
                                
                                Text(
                                    text = "${player.score}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isWinner) 18.sp else 16.sp,
                                    color = if (isWinner) player.color else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onBackToOnboarding,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "New Game",
                        maxLines = 1
                    )
                }
                Button(
                    onClick = onRestart,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Play Again",
                        maxLines = 1
                    )
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

// Modern UI Components
@Composable
private fun ModernGameHeader(
    gameState: GameState,
    onRestart: () -> Unit,
    onBackToOnboarding: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp), // Increased padding to bring header further down
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SOS Game",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Primary
                )
                Text(
                    text = "${gameState.boardSize}×${gameState.boardSize} • ${gameState.players.size} Players",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary.copy(alpha = 0.1f),
                        contentColor = Primary
                    )
                ) {
                    Text("Restart")
                }
                
                Button(
                    onClick = onBackToOnboarding,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Secondary.copy(alpha = 0.1f),
                        contentColor = Secondary
                    )
                ) {
                    Text("New Game")
                }
            }
        }
    }
}

@Composable
private fun ModernScoreBoard(gameState: GameState) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp, 
                vertical = if (gameState.players.size > 2) 12.dp else 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (gameState.players.size > 2) 8.dp else 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Accent)
                )
                Text(
                    text = "Scores",
                    fontSize = if (gameState.players.size > 2) 16.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            if (gameState.players.size > 2) {
                // Two-column grid layout for 3+ players
                val rows = (gameState.players.size + 1) / 2
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (rowIndex in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (colIndex in 0 until 2) {
                                val playerIndex = rowIndex * 2 + colIndex
                                if (playerIndex < gameState.players.size) {
                                    CompactPlayerScore(
                                        player = gameState.players[playerIndex],
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            } else {
                // Regular layout for 2 players
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    gameState.players.forEach { player ->
                        ModernPlayerScore(player = player)
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = player.color.copy(alpha = 0.04f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(player.color)
                )
                Text(
                    text = player.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = player.color,
                    maxLines = 1
                )
            }
            
            Text(
                text = "${player.score}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = player.color
            )
        }
    }
}

@Composable
private fun ModernPlayerScore(
    player: Player,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = player.color.copy(alpha = 0.06f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(player.color)
                )
                Text(
                    text = player.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = player.color,
                    maxLines = 1
                )
            }
            
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = player.color.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "${player.score}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = player.color,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ModernGameBoard(
    gameState: GameState,
    selectedCellValue: CellValue,
    onCellClick: (Int, Int) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gameState.boardSize),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            userScrollEnabled = false
        ) {
            itemsIndexed(gameState.board.flatten()) { index, cell ->
                val row = index / gameState.boardSize
                val col = index % gameState.boardSize
                
                ModernGameCell(
                    cell = cell,
                    gameState = gameState,
                    onClick = { onCellClick(row, col) }
                )
            }
        }
    }
}

@Composable
private fun ModernGameCell(
    cell: Cell,
    gameState: GameState,
    onClick: () -> Unit
) {
    val backgroundColor = if (cell.value != CellValue.EMPTY && cell.playerId >= 0) {
        gameState.players[cell.playerId].color.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    
    val borderColor = if (cell.value != CellValue.EMPTY && cell.playerId >= 0) {
        gameState.players[cell.playerId].color.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = cell.value == CellValue.EMPTY && !gameState.gameEnded) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // Strike-through lines behind text
        if (cell.isPartOfSOS && cell.sosDirections.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = when {
                    gameState.boardSize <= 8 -> 5.dp.toPx()
                    gameState.boardSize <= 10 -> 3.5.dp.toPx()
                    gameState.boardSize <= 12 -> 2.5.dp.toPx()
                    else -> 1.5.dp.toPx()
                }
                cell.sosDirections.forEach { direction ->
                    val playerId = cell.sosDirectionPlayerMap[direction] ?: -1
                    if (playerId >= 0 && playerId < gameState.players.size) {
                        val sosColor = gameState.players[playerId].color
                        when (direction) {
                            SOSDirection.HORIZONTAL -> {
                                drawLine(
                                    color = sosColor,
                                    start = Offset(size.width * 0.15f, size.height / 2),
                                    end = Offset(size.width * 0.85f, size.height / 2),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                            SOSDirection.VERTICAL -> {
                                drawLine(
                                    color = sosColor,
                                    start = Offset(size.width / 2, size.height * 0.15f),
                                    end = Offset(size.width / 2, size.height * 0.85f),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                            SOSDirection.DIAGONAL_DOWN_RIGHT -> {
                                drawLine(
                                    color = sosColor,
                                    start = Offset(size.width * 0.15f, size.height * 0.15f),
                                    end = Offset(size.width * 0.85f, size.height * 0.85f),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                            SOSDirection.DIAGONAL_DOWN_LEFT -> {
                                drawLine(
                                    color = sosColor,
                                    start = Offset(size.width * 0.85f, size.height * 0.15f),
                                    end = Offset(size.width * 0.15f, size.height * 0.85f),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Text on top
        if (cell.value != CellValue.EMPTY) {
            val hasStrikeThrough = cell.isPartOfSOS && cell.sosDirections.isNotEmpty()
            val textColor = if (cell.playerId >= 0) {
                gameState.players[cell.playerId].color
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            
            if (hasStrikeThrough) {
                // Enhanced text with stroke effect for visibility
                Box {
                    // Stroke layers
                    for (dx in -1..1) {
                        for (dy in -1..1) {
                            if (dx != 0 || dy != 0) {
                                Text(
                                    text = cell.value.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.offset(
                                        x = (dx * 0.8f).dp,
                                        y = (dy * 0.8f).dp
                                    )
                                )
                            }
                        }
                    }
                    
                    // Main text
                    Text(
                        text = cell.value.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor
                    )
                }
            } else {
                Text(
                    text = cell.value.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun ModernCurrentPlayerSection(
    gameState: GameState,
    selectedCellValue: CellValue,
    onCellValueSelected: (CellValue) -> Unit
) {
    if (!gameState.gameEnded) {
        val currentPlayer = gameState.players[gameState.currentPlayerIndex]
        
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(currentPlayer.color)
                        )
                        Text(
                            text = "${currentPlayer.name}'s Turn",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentPlayer.color
                        )
                    }
                    
                    // Timer display - only show if timer is enabled
                    if (gameState.timerRunning) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (gameState.timeLeftInSeconds <= 3) {
                                Color.Red.copy(alpha = 0.1f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "⏱",
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${gameState.timeLeftInSeconds}s",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (gameState.timeLeftInSeconds <= 3) {
                                        Color.Red
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                            }
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModernLetterButton(
                        letter = "S",
                        selected = selectedCellValue == CellValue.S,
                        playerColor = currentPlayer.color,
                        onClick = { onCellValueSelected(CellValue.S) },
                        modifier = Modifier.weight(1f)
                    )
                    
                    ModernLetterButton(
                        letter = "O",
                        selected = selectedCellValue == CellValue.O,
                        playerColor = currentPlayer.color,
                        onClick = { onCellValueSelected(CellValue.O) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernLetterButton(
    letter: String,
    selected: Boolean,
    playerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) playerColor else MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = if (selected) 8.dp else 2.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = letter,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (selected) MaterialTheme.colorScheme.surface else playerColor
            )
        }
    }
}
