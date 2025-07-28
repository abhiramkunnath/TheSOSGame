package com.example.thesosgame.game

import androidx.compose.ui.graphics.Color
import com.example.thesosgame.data.*

class GameLogic {
    
    fun initializeGame(config: GameConfig): GameState {
        val playerColors = listOf(
            Color(0xFFE57373), // Soft Red
            Color(0xFF64B5F6), // Soft Blue
            Color(0xFF81C784), // Soft Green
            Color(0xFFBA68C8), // Soft Purple
            Color(0xFF4DB6AC), // Soft Teal
            Color(0xFFFFB74D)  // Soft Orange
        )
        
        val players = (0 until config.numberOfPlayers).map { index ->
            Player(
                id = index,
                name = if (config.playerNames.isNotEmpty() && index < config.playerNames.size) {
                    config.playerNames[index]
                } else {
                    "Player ${index + 1}"
                },
                color = playerColors[index % playerColors.size]
            )
        }
        
        val board = List(config.boardSize) { 
            List(config.boardSize) { Cell() } 
        }
        
        return GameState(
            boardSize = config.boardSize,
            board = board,
            players = players,
            currentPlayerIndex = 0
        )
    }
    
    fun makeMove(
        gameState: GameState, 
        row: Int, 
        col: Int, 
        cellValue: CellValue
    ): GameState {
        // Check if the cell is already occupied
        if (gameState.board[row][col].value != CellValue.EMPTY) {
            return gameState
        }
        
        // Check if game has ended
        if (gameState.gameEnded) {
            return gameState
        }
        
        // Create new board with the move
        val newBoard = gameState.board.mapIndexed { r, rowList ->
            rowList.mapIndexed { c, cell ->
                if (r == row && c == col) {
                    Cell(cellValue, gameState.currentPlayerIndex)
                } else {
                    cell
                }
            }
        }
        
        // Find all SOS patterns involving this move
        val newSOSPatterns = findNewSOSPatterns(newBoard, row, col, gameState.currentPlayerIndex)
        val allSOSPatterns = gameState.sosPatterns + newSOSPatterns
        
        // Mark cells that are part of SOS patterns
        val boardWithSOSMarks = if (newSOSPatterns.isNotEmpty()) {
            markSOSCells(newBoard, newSOSPatterns, gameState.currentPlayerIndex)
        } else {
            newBoard
        }
        
        // Update player scores
        val updatedPlayers = gameState.players.mapIndexed { index, player ->
            val additionalScore = newSOSPatterns.count { it.playerId == index }
            player.copy(score = player.score + additionalScore)
        }
        
        // Check if board is full
        val isBoardFull = boardWithSOSMarks.all { row -> row.all { it.value != CellValue.EMPTY } }
        
        // Determine winner if game ended
        val winner = if (isBoardFull) {
            updatedPlayers.maxByOrNull { it.score }
        } else null
        
        // Move to next player - if current player scored, they play again
        val playerScored = newSOSPatterns.isNotEmpty()
        val nextPlayerIndex = if (playerScored && !isBoardFull) {
            // Player scored, so they play again
            gameState.currentPlayerIndex
        } else {
            // No score or game ended, move to next player
            (gameState.currentPlayerIndex + 1) % gameState.players.size
        }
        
        return gameState.copy(
            board = boardWithSOSMarks,
            players = updatedPlayers,
            currentPlayerIndex = nextPlayerIndex,
            gameEnded = isBoardFull,
            winner = winner,
            sosPatterns = allSOSPatterns
        )
    }
    
    private fun findNewSOSPatterns(
        board: List<List<Cell>>, 
        row: Int, 
        col: Int, 
        playerId: Int
    ): List<SOSPattern> {
        val patterns = mutableListOf<SOSPattern>()
        val boardSize = board.size
        val currentCell = board[row][col]
        
        // All 8 directions with their corresponding SOSDirection
        val directions = listOf(
            Triple(0, 1, SOSDirection.HORIZONTAL),     // right
            Triple(1, 0, SOSDirection.VERTICAL),       // down
            Triple(1, 1, SOSDirection.DIAGONAL_DOWN_RIGHT),   // diagonal down-right
            Triple(1, -1, SOSDirection.DIAGONAL_DOWN_LEFT),   // diagonal down-left
            Triple(0, -1, SOSDirection.HORIZONTAL),    // left
            Triple(-1, 0, SOSDirection.VERTICAL),      // up
            Triple(-1, -1, SOSDirection.DIAGONAL_DOWN_RIGHT), // diagonal up-left
            Triple(-1, 1, SOSDirection.DIAGONAL_DOWN_LEFT)    // diagonal up-right
        )
        
        for ((dr, dc, sosDirection) in directions) {
            // Check if current position can be middle of SOS (only if current is O)
            if (currentCell.value == CellValue.O) {
                val prevRow = row - dr
                val prevCol = col - dc
                val nextRow = row + dr
                val nextCol = col + dc
                
                if (isValidPosition(prevRow, prevCol, boardSize) && 
                    isValidPosition(nextRow, nextCol, boardSize)) {
                    
                    val prevCell = board[prevRow][prevCol]
                    val nextCell = board[nextRow][nextCol]
                    
                    if (prevCell.value == CellValue.S && nextCell.value == CellValue.S) {
                        val positions = listOf(
                            Pair(prevRow, prevCol),
                            Pair(row, col),
                            Pair(nextRow, nextCol)
                        )
                        patterns.add(SOSPattern(positions, playerId, sosDirection))
                    }
                }
            }
            
            // Check if current position can be start of SOS (only if current is S)
            if (currentCell.value == CellValue.S) {
                val midRow = row + dr
                val midCol = col + dc
                val endRow = row + 2 * dr
                val endCol = col + 2 * dc
                
                if (isValidPosition(midRow, midCol, boardSize) && 
                    isValidPosition(endRow, endCol, boardSize)) {
                    
                    val midCell = board[midRow][midCol]
                    val endCell = board[endRow][endCol]
                    
                    if (midCell.value == CellValue.O && endCell.value == CellValue.S) {
                        val positions = listOf(
                            Pair(row, col),
                            Pair(midRow, midCol),
                            Pair(endRow, endCol)
                        )
                        patterns.add(SOSPattern(positions, playerId, sosDirection))
                    }
                }
            }
            
            // Check if current position can be end of SOS (only if current is S)
            if (currentCell.value == CellValue.S) {
                val midRow = row - dr
                val midCol = col - dc
                val startRow = row - 2 * dr
                val startCol = col - 2 * dc
                
                if (isValidPosition(midRow, midCol, boardSize) && 
                    isValidPosition(startRow, startCol, boardSize)) {
                    
                    val midCell = board[midRow][midCol]
                    val startCell = board[startRow][startCol]
                    
                    if (startCell.value == CellValue.S && midCell.value == CellValue.O) {
                        val positions = listOf(
                            Pair(startRow, startCol),
                            Pair(midRow, midCol),
                            Pair(row, col)
                        )
                        patterns.add(SOSPattern(positions, playerId, sosDirection))
                    }
                }
            }
        }
        
        return patterns.distinctBy { pattern -> pattern.positions.sortedWith(compareBy({ it.first }, { it.second })) }
    }
    
    private fun markSOSCells(
        board: List<List<Cell>>,
        sosPatterns: List<SOSPattern>,
        playerId: Int
    ): List<List<Cell>> {
        val updatedBoard = board.map { it.toMutableList() }.toMutableList()
        
        sosPatterns.forEach { pattern ->
            pattern.positions.forEach { (row, col) ->
                val currentCell = updatedBoard[row][col]
                updatedBoard[row][col] = currentCell.copy(
                    isPartOfSOS = true,
                    sosPlayerIds = currentCell.sosPlayerIds + playerId,
                    sosDirections = currentCell.sosDirections + pattern.direction,
                    sosDirectionPlayerMap = currentCell.sosDirectionPlayerMap + (pattern.direction to playerId)
                )
            }
        }
        
        return updatedBoard.map { it.toList() }
    }
    
    private fun isValidPosition(row: Int, col: Int, boardSize: Int): Boolean {
        return row in 0 until boardSize && col in 0 until boardSize
    }
}
