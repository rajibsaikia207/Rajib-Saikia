package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.AppLogo
import com.example.ui.theme.*

@Composable
fun SelectUserScreen(
    onSelectPassenger: () -> Unit,
    onSelectDriver: () -> Unit,
    onSelectAdmin: () -> Unit,
    onOpenHelp: () -> Unit
) {
    Scaffold(
        containerColor = BackgroundLight
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // E-Ride 3 logo at top
                AppLogo(size = 56.dp)
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "Welcome to E-Ride 3",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = CharcoalDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose how you want to continue",
                    fontSize = 14.sp,
                    color = SlateGray
                )
                
                Spacer(modifier = Modifier.height(28.dp))

                // OPTION 1: Passenger Card (Green filled card)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSelectPassenger),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandGreen),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Passenger",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "I'm a Passenger",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Book quick & affordable local E-Rickshaws",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                lineHeight = 17.sp
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Continue",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // OPTION 2: Driver Card (White card with green border)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSelectDriver),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.5.dp, BrandGreen),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            modifier = Modifier.size(50.dp),
                            shadowElevation = 1.dp
                        ) {
                            Box(
                                modifier = Modifier.padding(5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_eride_rickshaw),
                                    contentDescription = "Driver",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "I'm a Driver",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = CharcoalDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Register your E-Rickshaw & start earning",
                                fontSize = 13.sp,
                                color = SlateGray,
                                lineHeight = 17.sp
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Continue",
                            tint = BrandGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // OFFICIAL SERVICE AREAS
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "OFFICIAL SERVICE AREAS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.2.sp,
                        color = SlateGray
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    val serviceAreas = listOf(
                        "Tezpur",
                        "Biswanath Chariali",
                        "Nagsankar"
                    )

                    serviceAreas.forEach { areaName ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = BrandGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = areaName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = CharcoalDark
                                    )
                                }
                                
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = BrandGreenLight
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(BrandGreen)
                                        )
                                        Text(
                                            text = "Active",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandGreenDark
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Section: Admin Access & Help
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(
                    onClick = onSelectAdmin,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin",
                        tint = SlateGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Admin Web / Control Panel",
                        color = SlateGray,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }

                TextButton(
                    onClick = onOpenHelp,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Help",
                        tint = SlateGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Help & Support",
                        color = SlateGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
