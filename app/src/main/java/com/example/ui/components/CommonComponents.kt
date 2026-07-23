package com.example.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.model.FootballPosition
import com.example.model.PhysicalStats
import com.example.ui.theme.*

@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    borderGlow: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "borderGlow")
    val glowColor by if (borderGlow) {
        infiniteTransition.animateColor(
            initialValue = NeonCyan.copy(alpha = 0.3f),
            targetValue = NeonCyan.copy(alpha = 0.8f),
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow"
        )
    } else {
        remember { mutableStateOf(DarkBorder) }
    }

    val cardShape = RoundedCornerShape(10.dp)

    // Interactive scale reaction on press
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1.0f,
        animationSpec = tween(100, easing = LinearOutSlowInEasing),
        label = "cardScale"
    )

    val baseModifier = modifier
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onClick() }
            } else Modifier
        )
        .background(DarkSlate, cardShape)
        .border(
            width = if (borderGlow) 1.5.dp else 1.dp,
            color = if (borderGlow) glowColor else Color(0x1EFFFFFF),
            shape = cardShape
        )
        .padding(16.dp)

    Column(
        modifier = baseModifier,
        content = content
    )
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSecondary: Boolean = false,
    testTag: String = "neon_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = tween(80, easing = FastOutSlowInEasing),
        label = "buttonScale"
    )

    // High performance cyber neon brush or sleek outline
    val bgBrush = if (isSecondary) {
        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    } else {
        Brush.horizontalGradient(listOf(NeonCyan, Color(0xFF00B0FF)))
    }

    val contentColor = if (isSecondary) NeonCyan else AlmostBlack
    val borderColor = if (isSecondary) NeonCyan else Color.Transparent

    Box(
        modifier = modifier
            .testTag(testTag)
            .height(48.dp)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(RoundedCornerShape(8.dp))
            .then(if (isSecondary) Modifier.background(Color.Transparent) else Modifier.background(bgBrush))
            .border(
                width = if (isSecondary) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick()
            }
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = if (enabled) contentColor else TextGray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FUTPlayerCard(
    name: String,
    position: String,
    rating: Int,
    nationality: String,
    club: String,
    stats: PhysicalStats,
    modifier: Modifier = Modifier,
    glow: Boolean = true,
    photoUrl: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "playerCardGlow")
    val animatedGlowColor by infiniteTransition.animateColor(
        initialValue = NeonCyan.copy(alpha = 0.4f),
        targetValue = NeonCyan.copy(alpha = 0.9f),
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderGlow"
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = maxWidth
        // Scale proportionally if container is narrower than 260.dp
        val cardWidth = minOf(260.dp, containerWidth - 16.dp)
        val cardHeight = cardWidth * (400f / 260f)
        val scale = (cardWidth / 260.dp)

        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .drawBehind {
                    val path = Path().apply {
                        val w = size.width
                        val h = size.height
                        moveTo(w * 0.5f, 0f) 
                        lineTo(w * 0.95f, h * 0.12f) 
                        lineTo(w * 0.95f, h * 0.78f) 
                        lineTo(w * 0.5f, h) 
                        lineTo(w * 0.05f, h * 0.78f) 
                        lineTo(w * 0.05f, h * 0.12f) 
                        close()
                    }

                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1B1D25),
                                AlmostBlack
                            )
                        )
                    )

                    val strokeColor = if (rating >= 85) GoldChampionship else NeonCyan
                    drawPath(
                        path = path,
                        color = if (glow) (if (rating >= 85) GoldChampionship else animatedGlowColor) else Color(0x22FFFFFF),
                        style = Stroke(width = if (glow) (3.5f * scale) else (1.5f * scale))
                    )

                    // Tactical tech grids in background
                    drawLine(
                        color = strokeColor.copy(alpha = 0.12f),
                        start = Offset(0f, size.height * 0.35f),
                        end = Offset(size.width, size.height * 0.35f),
                        strokeWidth = 1.5f * scale
                    )
                    drawLine(
                        color = strokeColor.copy(alpha = 0.08f),
                        start = Offset(0f, size.height * 0.72f),
                        end = Offset(size.width, size.height * 0.72f),
                        strokeWidth = 1.5f * scale
                    )
                }
                .padding(20.dp * scale)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp * scale),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Rating & position section aligned beautifully
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(start = 14.dp * scale, top = 8.dp * scale)
                    ) {
                        Text(
                            text = rating.toString(),
                            color = if (rating >= 85) GoldChampionship else TextWhite,
                            fontSize = 36.sp * scale,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = (-1).sp
                        )
                        Text(
                            text = position,
                            color = if (rating >= 85) GoldChampionship else NeonCyan,
                            fontSize = 12.sp * scale,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp * scale))
                        Box(
                            modifier = Modifier
                                .width(28.dp * scale)
                                .height(18.dp * scale)
                                .background(NeonCyan.copy(alpha = 0.15f))
                                .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(2.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "TR",
                                color = TextWhite,
                                fontSize = 9.sp * scale,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Circular portrait photo container aligned symmetrically
                    Box(
                        modifier = Modifier
                            .padding(end = 14.dp * scale, top = 8.dp * scale)
                            .size(90.dp * scale)
                            .clip(CircleShape)
                            .background(Color(0xFF16171D))
                            .border(2.dp, if (rating >= 85) GoldChampionship else NeonCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUrl != null) {
                            Image(
                                painter = rememberAsyncImagePainter(model = photoUrl),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Canvas(modifier = Modifier.size(50.dp * scale)) {
                                drawCircle(
                                    color = TextGray.copy(alpha = 0.25f),
                                    radius = size.minDimension / 3f,
                                    center = Offset(size.width / 2, size.height * 0.35f)
                                )
                                drawArc(
                                    color = TextGray.copy(alpha = 0.25f),
                                    startAngle = 180f,
                                    sweepAngle = 180f,
                                    useCenter = false,
                                    topLeft = Offset(0f, size.height * 0.55f),
                                    size = Size(size.width, size.height)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp * scale))

                Text(
                    text = name.uppercase(),
                    color = TextWhite,
                    fontSize = 18.sp * scale,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                val displayClub = if (club.isBlank() || 
                    club.trim().lowercase() == "free agent" || 
                    club.trim().lowercase() == "serbest" || 
                    club.trim().lowercase() == "kulüpsüz" || 
                    club.trim() == "-" || 
                    club.trim() == "Belirtilmemiş"
                ) {
                    "KULÜPSÜZ"
                } else {
                    club.uppercase()
                }

                Text(
                    text = displayClub,
                    color = TextGray,
                    fontSize = 10.sp * scale,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp * scale)
                )

                Spacer(modifier = Modifier.height(14.dp * scale))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.84f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF101216))
                        .border(width = 0.5.dp, color = Color(0x16FFFFFF), shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp * scale, vertical = 6.dp * scale)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(2.dp * scale)) {
                            StatRow(value = stats.pace, label = "HIZ", scale = scale)
                            StatRow(value = stats.shooting, label = "ŞUT", scale = scale)
                            StatRow(value = stats.passing, label = "PAS", scale = scale)
                        }

                        Box(
                            modifier = Modifier
                                .height(40.dp * scale)
                                .width(1.dp)
                                .background(Color(0x16FFFFFF))
                        )

                        Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(2.dp * scale)) {
                            StatRow(value = stats.dribbling, label = "DRİ", scale = scale)
                            StatRow(value = stats.defense, label = "DEF", scale = scale)
                            StatRow(value = stats.physical, label = "FİZ", scale = scale)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatRow(value: Int, label: String, scale: Float = 1f) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 1.dp * scale)
    ) {
        Text(
            text = value.toString(),
            color = TextWhite,
            fontSize = 12.sp * scale,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(20.dp * scale)
        )
        Spacer(modifier = Modifier.width(4.dp * scale))
        Text(
            text = label,
            color = NeonCyan,
            fontSize = 9.sp * scale,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun TacticalFieldPositionSelector(
    selectedPosition: FootballPosition,
    onPositionSelected: (FootballPosition) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .background(DarkSlate, RoundedCornerShape(10.dp))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(10.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "TAKTİKSEL POZİSYON SEÇİMİ",
            color = NeonCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .drawBehind {
                    val w = size.width
                    val h = size.height

                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0xFF0F1115),
                                Color(0xFF0A0B0E)
                            )
                        )
                    )

                    drawRect(
                        color = Color(0x1AFFFFFF),
                        style = Stroke(width = 2f)
                    )

                    drawRect(
                        color = Color(0x1AFFFFFF),
                        topLeft = Offset(w * 0.22f, 0f),
                        size = Size(w * 0.56f, h * 0.18f),
                        style = Stroke(width = 1.5f)
                    )
                    drawRect(
                        color = Color(0x1AFFFFFF),
                        topLeft = Offset(w * 0.35f, 0f),
                        size = Size(w * 0.3f, h * 0.06f),
                        style = Stroke(width = 1.5f)
                    )

                    drawLine(
                        color = Color(0x1AFFFFFF),
                        start = Offset(0f, h / 2),
                        end = Offset(w, h / 2),
                        strokeWidth = 1.5f
                    )

                    drawCircle(
                        color = Color(0x1AFFFFFF),
                        radius = w * 0.15f,
                        center = Offset(w / 2, h / 2),
                        style = Stroke(width = 1.5f)
                    )

                    drawRect(
                        color = Color(0x1AFFFFFF),
                        topLeft = Offset(w * 0.22f, h * 0.82f),
                        size = Size(w * 0.56f, h * 0.18f),
                        style = Stroke(width = 1.5f)
                    )
                }
        ) {
            TacticalNode(FootballPosition.ST, 0.43f, 0.08f, selectedPosition, onPositionSelected)

            TacticalNode(FootballPosition.LW, 0.12f, 0.14f, selectedPosition, onPositionSelected)
            TacticalNode(FootballPosition.RW, 0.74f, 0.14f, selectedPosition, onPositionSelected)

            TacticalNode(FootballPosition.SS, 0.43f, 0.23f, selectedPosition, onPositionSelected)

            TacticalNode(FootballPosition.AM, 0.43f, 0.38f, selectedPosition, onPositionSelected)
            TacticalNode(FootballPosition.CM, 0.22f, 0.50f, selectedPosition, onPositionSelected)
            TacticalNode(FootballPosition.DM, 0.64f, 0.50f, selectedPosition, onPositionSelected)

            TacticalNode(FootballPosition.LB, 0.08f, 0.70f, selectedPosition, onPositionSelected)
            TacticalNode(FootballPosition.CB, 0.43f, 0.73f, selectedPosition, onPositionSelected)
            TacticalNode(FootballPosition.RB, 0.78f, 0.70f, selectedPosition, onPositionSelected)

            TacticalNode(FootballPosition.GK, 0.43f, 0.88f, selectedPosition, onPositionSelected)
        }
    }
}

@Composable
fun BoxScope.TacticalNode(
    pos: FootballPosition,
    fractionX: Float,
    fractionY: Float,
    selected: FootballPosition,
    onSelect: (FootballPosition) -> Unit
) {
    val isSelected = pos == selected

    // Animation when selected vs unselected
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "nodeScale"
    )

    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) NeonCyan else Color(0xFF1E212A),
        animationSpec = tween(150),
        label = "nodeColor"
    )

    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth(0.14f)
            .aspectRatio(1.0f)
            .offset(
                x = (fractionX * 280).dp,
                y = (fractionY * 260).dp
            )
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(CircleShape)
            .background(animatedColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color.White else Color(0x3DFFFFFF),
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onSelect(pos) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = pos.shortName,
            color = if (isSelected) AlmostBlack else TextWhite,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black
        )
    }
}
