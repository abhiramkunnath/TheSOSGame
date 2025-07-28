package com.example.thesosgame.data

import androidx.compose.ui.graphics.Color

enum class CellValue {
    EMPTY, S, O
}

enum class SOSDirection {
    HORIZONTAL, VERTICAL, DIAGONAL_DOWN_RIGHT, DIAGONAL_DOWN_LEFT
}

data class Cell(
    val value: CellValue = CellValue.EMPTY,
    val playerId: Int = -1,
    val isPartOfSOS: Boolean = false,
    val sosPlayerIds: Set<Int> = emptySet(), // Multiple players who scored SOS patterns on this cell
    val sosDirections: Set<SOSDirection> = emptySet(), // Multiple directions for overlapping patterns
    val sosDirectionPlayerMap: Map<SOSDirection, Int> = emptyMap() // Maps each direction to the player who scored it
)

data class Player(
    val id: Int,
    val name: String,
    val color: Color,
    val score: Int = 0
)

data class SOSPattern(
    val positions: List<Pair<Int, Int>>,
    val playerId: Int,
    val direction: SOSDirection
)

data class GameState(
    val boardSize: Int,
    val board: List<List<Cell>>,
    val players: List<Player>,
    val currentPlayerIndex: Int,
    val gameEnded: Boolean = false,
    val winner: Player? = null,
    val sosPatterns: List<SOSPattern> = emptyList()
)

data class GameConfig(
    val boardSize: Int,
    val numberOfPlayers: Int,
    val playerNames: List<String> = emptyList()
)
