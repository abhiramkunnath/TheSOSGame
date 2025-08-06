package com.example.thesosgame.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// Color serializer
object ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: Color) = encoder.encodeInt(value.toArgb())
    override fun deserialize(decoder: Decoder): Color = Color(decoder.decodeInt())
}

@Serializable
enum class CellValue {
    EMPTY, S, O
}

@Serializable
enum class SOSDirection {
    HORIZONTAL, VERTICAL, DIAGONAL_DOWN_RIGHT, DIAGONAL_DOWN_LEFT
}

@Serializable
data class Cell(
    val value: CellValue = CellValue.EMPTY,
    val playerId: Int = -1,
    val isPartOfSOS: Boolean = false,
    val sosPlayerIds: Set<Int> = emptySet(), // Multiple players who scored SOS patterns on this cell
    val sosDirections: Set<SOSDirection> = emptySet(), // Multiple directions for overlapping patterns
    val sosDirectionPlayerMap: Map<SOSDirection, Int> = emptyMap() // Maps each direction to the player who scored it
)

@Serializable
data class Player(
    val id: Int,
    val name: String,
    @Serializable(with = ColorSerializer::class)
    val color: Color,
    val score: Int = 0
)

@Serializable
data class SOSPattern(
    val positions: List<Pair<Int, Int>>,
    val playerId: Int,
    val direction: SOSDirection
)

@Serializable
data class GameState(
    val boardSize: Int,
    val board: List<List<Cell>>,
    val players: List<Player>,
    val currentPlayerIndex: Int,
    val gameEnded: Boolean = false,
    val winner: Player? = null,
    val sosPatterns: List<SOSPattern> = emptyList(),
    val timeLeftInSeconds: Int = 10,
    val timerRunning: Boolean = true,
    val timerDurationSeconds: Int = 10
)

@Serializable
data class GameConfig(
    val boardSize: Int,
    val numberOfPlayers: Int,
    val playerNames: List<String> = emptyList(),
    val timerEnabled: Boolean = true,
    val timerDurationSeconds: Int = 10
)
