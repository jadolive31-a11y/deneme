package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    val eyebrows = listOf(
        "FUTBOLCUM EKOSİSTEMİ",
        "DİJİTAL FUTBOL KİMLİĞİN",
        "FUTBOLDAKİ YERİNİ AL"
    )

    val titles = listOf(
        "FUTBOLUN TÜM DÜNYASI\nTEK YERDE",
        "KARİYERİNİ OLUŞTUR,\nGÖRÜNÜR OL",
        "BAĞLAN. KEŞFET.\nYÜKSEL."
    )

    val descriptions = listOf(
        "Futbolcular, kulüpler, akademiler, antrenörler, scoutlar ve futbol profesyonelleri tek bir güçlü ağda buluşuyor.",
        "Profesyonel profilini oluştur, futbol geçmişini kaydet, başarılarını sergile ve doğru insanların seni keşfetmesini sağla.",
        "Oyuncunu, takımını, antrenörünü, turnuvanı ve futbol fırsatlarını keşfet. Futboldaki yolculuğunu FUTBOLCUM ile büyüt."
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AlmostBlack)
            .drawBehind {
                // Subtle dotted tactical grid background
                val spacing = 24.dp.toPx()
                val dotColor = Color(0xFF1F2937).copy(alpha = 0.35f)
                for (x in 0..size.width.toInt() step spacing.toInt()) {
                    for (y in 0..size.height.toInt() step spacing.toInt()) {
                        drawCircle(
                            color = dotColor,
                            radius = 1.2f.dp.toPx(),
                            center = Offset(x.toFloat(), y.toFloat())
                        )
                    }
                }
            }
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Top Bar with Skip/Atla button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.End
        ) {
            if (pagerState.currentPage < 2) {
                Text(
                    text = "ATLA",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .testTag("skip_onboarding")
                        .clickable { onFinished() }
                        .padding(8.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 50.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Horizontal Pager for visual content and texts
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Visual Illustration Container (takes around 45% of available height)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.1f),
                        contentAlignment = Alignment.Center
                    ) {
                        when (page) {
                            0 -> OnboardingNetworkVisual()
                            1 -> OnboardingPlayerCardVisual()
                            2 -> OnboardingOpportunityVisual()
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Text Content Block (takes remaining space)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.9f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        // Category Eyebrow
                        Text(
                            text = eyebrows[page],
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Large Title
                        Text(
                            text = titles[page],
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            lineHeight = 32.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Description
                        Text(
                            text = descriptions[page],
                            color = TextGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        if (page == 2) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Futboldaki yerin burada başlıyor.",
                                color = GoldChampionship,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer: Indicators and Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicator Dots
                Row(
                    modifier = Modifier.padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val isSelected = index == pagerState.currentPage
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "indicator_width"
                        )
                        val color = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.15f)
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                // Action Button
                Button(
                    onClick = {
                        if (pagerState.currentPage < 2) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onFinished()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("onboarding_next_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = AlmostBlack
                    )
                ) {
                    Text(
                        text = if (pagerState.currentPage == 2) "FUTBOLCUM'A KATIL" else "DEVAM ET",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingNetworkVisual() {
    val infiniteTransition = rememberInfiniteTransition(label = "network")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val lightProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "light"
    )

    Box(
        modifier = Modifier
            .size(280.dp)
            .testTag("onboarding_network_visual"),
        contentAlignment = Alignment.Center
    ) {
        val outerNodes = listOf(
            "Futbolcu" to Icons.Default.Person,
            "Kulüp" to Icons.Default.Group,
            "Antrenör" to Icons.Default.Sports,
            "Scout" to Icons.Default.Search,
            "Akademi" to Icons.Default.Star,
            "Medya" to Icons.Default.PhotoCamera
        )

        // Draw connections Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radiusPx = 95.dp.toPx()

            outerNodes.forEachIndexed { index, _ ->
                val angle = (index * (2 * Math.PI) / outerNodes.size).toFloat()
                // Orbit offset
                val orbitX = kotlin.math.sin(phase + index * 1.0f) * 6.dp.toPx()
                val orbitY = kotlin.math.cos(phase + index * 1.0f) * 6.dp.toPx()

                val targetX = center.x + radiusPx * kotlin.math.cos(angle) + orbitX
                val targetY = center.y + radiusPx * kotlin.math.sin(angle) + orbitY
                val target = Offset(targetX, targetY)

                // Draw connecting line
                drawLine(
                    color = NeonCyan.copy(alpha = 0.25f),
                    start = center,
                    end = target,
                    strokeWidth = 1.5.dp.toPx()
                )

                // Draw moving light point
                val lightX = center.x + (targetX - center.x) * lightProgress
                val lightY = center.y + (targetY - center.y) * lightProgress
                drawCircle(
                    color = NeonCyan,
                    radius = 3.dp.toPx(),
                    center = Offset(lightX, lightY)
                )
            }
        }

        // Center Node
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(colors = listOf(NeonCyan.copy(alpha = 0.2f), AlmostBlack)))
                .border(2.dp, NeonCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SportsSoccer,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(32.dp)
            )
        }

        // Outer Node Composable placement using box offsets
        outerNodes.forEachIndexed { index, node ->
            val angle = (index * (2 * Math.PI) / outerNodes.size).toFloat()
            // Orbit offsets
            val xOffset = (95 * kotlin.math.cos(angle) + kotlin.math.sin(phase + index * 1.0f) * 6).dp
            val yOffset = (95 * kotlin.math.sin(angle) + kotlin.math.cos(phase + index * 1.0f) * 6).dp

            Box(
                modifier = Modifier
                    .offset(x = xOffset, y = yOffset)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DarkSlate.copy(alpha = 0.85f))
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = node.second,
                    contentDescription = node.first,
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun OnboardingPlayerCardVisual() {
    val infiniteTransition = rememberInfiniteTransition(label = "card")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = Modifier
            .size(280.dp)
            .testTag("onboarding_card_visual"),
        contentAlignment = Alignment.Center
    ) {
        // Subtle glow in the background of the card
        Box(
            modifier = Modifier
                .size(170.dp, 250.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonCyan.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
        )

        // Main Player Card
        Box(
            modifier = Modifier
                .size(160.dp, 230.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(colors = listOf(DarkSlate, AlmostBlack)))
                .border(1.5.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Card header: OVR & POSITION
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "88",
                            color = NeonCyan,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "OOS",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = GoldChampionship,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Player Silhouette
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(DarkBorder)
                        .border(1.dp, NeonCyan.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Name
                Text(
                    text = "DİJİTAL KİMLİK",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Divider line
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(1.dp)
                        .background(NeonCyan.copy(alpha = 0.3f))
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Stats block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("AYAK", color = TextGray, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        Text("SAĞ", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("KULÜP", color = TextGray, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        Text("SERBEST", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ŞEHİR", color = TextGray, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        Text("İST", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // Surrounding 4 floating tags
        val tags = listOf(
            Triple("Kulüp Daveti", Color(0xFF00E676), -100.dp to -80.dp),
            Triple("Scout Raporu", Color(0xFFE040FB), 100.dp to -60.dp),
            Triple("Takım Geçmişi", NeonCyan, -95.dp to 70.dp),
            Triple("Başarı Rozeti", GoldChampionship, 95.dp to 80.dp)
        )

        tags.forEachIndexed { index, (label, color, coords) ->
            val floatX = (kotlin.math.sin(phase + index * 1.5f) * 4).toFloat()
            val floatY = (kotlin.math.cos(phase + index * 1.5f) * 4).toFloat()

            Box(
                modifier = Modifier
                    .offset(x = coords.first + floatX.dp, y = coords.second + floatY.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AlmostBlack.copy(alpha = 0.9f))
                    .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingOpportunityVisual() {
    val infiniteTransition = rememberInfiniteTransition(label = "opportunities")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val lineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "line"
    )

    Box(
        modifier = Modifier
            .size(280.dp)
            .testTag("onboarding_opp_visual"),
        contentAlignment = Alignment.Center
    ) {
        // Draw stylized tactical soccer field background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2, h / 2)

            // Draw field boundaries (inset)
            val insetX = 20.dp.toPx()
            val insetY = 30.dp.toPx()
            val fieldWidth = w - 2 * insetX
            val fieldHeight = h - 2 * insetY

            // Outer field boundary
            drawRect(
                color = NeonCyan.copy(alpha = 0.08f),
                topLeft = Offset(insetX, insetY),
                size = Size(fieldWidth, fieldHeight),
                style = Stroke(1.dp.toPx())
            )

            // Center line and center circle
            drawLine(
                color = NeonCyan.copy(alpha = 0.08f),
                start = Offset(insetX, h / 2),
                end = Offset(w - insetX, h / 2),
                strokeWidth = 1.dp.toPx()
            )
            drawCircle(
                color = NeonCyan.copy(alpha = 0.08f),
                radius = 35.dp.toPx(),
                center = center,
                style = Stroke(1.dp.toPx())
            )

            // Penalty areas
            drawRect(
                color = NeonCyan.copy(alpha = 0.05f),
                topLeft = Offset(w / 2 - 50.dp.toPx(), insetY),
                size = Size(100.dp.toPx(), 40.dp.toPx()),
                style = Stroke(1.dp.toPx())
            )
            drawRect(
                color = NeonCyan.copy(alpha = 0.05f),
                topLeft = Offset(w / 2 - 50.dp.toPx(), h - insetY - 40.dp.toPx()),
                size = Size(100.dp.toPx(), 40.dp.toPx()),
                style = Stroke(1.dp.toPx())
            )

            // Draw connection lines to opportunity nodes
            val targets = listOf(
                Offset(w * 0.2f, h * 0.25f),  // Top Left
                Offset(w * 0.8f, h * 0.22f),  // Top Right
                Offset(w * 0.18f, h * 0.72f), // Bottom Left
                Offset(w * 0.82f, h * 0.75f)  // Bottom Right
            )

            targets.forEach { target ->
                drawLine(
                    color = NeonCyan.copy(alpha = 0.2f),
                    start = center,
                    end = target,
                    strokeWidth = 1.2.dp.toPx()
                )
                // Moving particle
                val pX = center.x + (target.x - center.x) * lineProgress
                val pY = center.y + (target.y - center.y) * lineProgress
                drawCircle(
                    color = NeonCyan,
                    radius = 2.5.dp.toPx(),
                    center = Offset(pX, pY)
                )
            }
        }

        // Center badge
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(colors = listOf(NeonCyan.copy(alpha = 0.25f), AlmostBlack)))
                .border(2.dp, NeonCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "F",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        // 4 Opportunity Cards
        val cards = listOf(
            // Title, Icon, coordinates
            Triple("KULÜP İLANI", Icons.Default.Group, -90.dp to -70.dp),
            Triple("ANTRENÖR", Icons.Default.Sports, 85.dp to -80.dp),
            Triple("TURNUVA", Icons.Default.EmojiEvents, -95.dp to 65.dp),
            Triple("SCOUTING", Icons.Default.Search, 90.dp to 70.dp)
        )

        cards.forEachIndexed { index, (label, icon, coords) ->
            val floatX = (kotlin.math.cos(phase + index * 1.5f) * 3).toFloat()
            val floatY = (kotlin.math.sin(phase + index * 1.5f) * 3).toFloat()

            Box(
                modifier = Modifier
                    .offset(x = coords.first + floatX.dp, y = coords.second + floatY.dp)
                    .width(105.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AlmostBlack.copy(alpha = 0.92f))
                    .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(6.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "AKTİF",
                        color = GoldChampionship,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
