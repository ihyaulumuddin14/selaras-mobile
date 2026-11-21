package com.example.selaras.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.selaras.core.ui.components.SelarasIcons

/**
 * 5 Main Navigation Tabs in Selaras (PRD Section 6.3 & Figma 12:245).
 */
enum class NavTab(
    val title: String,
    val icon: ImageVector
) {
    HOME("Beranda", SelarasIcons.Home),
    CHECK_IN("Check-in", SelarasIcons.EditNote),
    SCREENING("Skrining", SelarasIcons.FactCheck),
    HISTORY("Riwayat", SelarasIcons.History),
    HELP("Bantuan", SelarasIcons.SupportAgent)
}

/**
 * V2/BottomNav Component (Node ID: 12:245).
 *
 * Design Specs (Section 6.5):
 * - Height: 80dp, Background: Surface (#FFFFFF), Divider: #E2E8E3.
 * - 5 Tabs: Beranda, Check-in, Skrining, Riwayat, Bantuan.
 * - Active tab: pill container (#DCECE3) with pine green (#356859) icon & label.
 * - Inactive tab: #52675F.
 * - Priority Help Path (FR-DSH-03): Help tab is always active and non-blocking.
 */
@Composable
fun BottomNavBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColor = Color(0xFFFFFFFF)
    val dividerColor = Color(0xFFE2E8E3)
    val activePillColor = Color(0xFFDCECE3)
    val activeContentColor = Color(0xFF356859)
    val inactiveContentColor = Color(0xFF52675F)

    Surface(
        color = surfaceColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 6.dp)
        ) {
            HorizontalDivider(
                thickness = 1.dp,
                color = dividerColor
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    val contentColor = if (isSelected) activeContentColor else inactiveContentColor

                    Column(
                        modifier = Modifier
                            .width(72.dp)
                            .height(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                role = Role.Tab,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = activeContentColor),
                                onClick = { onTabSelected(tab) }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Pill Indicator for active tab
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) activePillColor else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = contentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Text(
                            text = tab.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = contentColor,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
