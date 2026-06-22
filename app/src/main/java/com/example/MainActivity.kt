package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.Business
import com.example.data.Offer
import com.example.data.CustomerInquiry
import com.example.ui.LocalConnectViewModel
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    private val viewModel: LocalConnectViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    LocalConnectApp(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@SuppressLint("StateFlowValueCalledInAsState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalConnectApp(viewModel: LocalConnectViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions.values.any { it }
        if (hasLocationPermission) {
            Toast.makeText(context, "Location Permitted! Real-time distance of shops updated.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // ViewModel Flows
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isOwnerMode by viewModel.isOwnerMode.collectAsState()
    val ownedBusinessId by viewModel.ownedBusinessId.collectAsState()
    
    val businesses by viewModel.allBusinesses.collectAsState()
    val offers by viewModel.allOffers.collectAsState()
    val queries by viewModel.allQueries.collectAsState()
    
    val aiResult by viewModel.aiRecommendationResult.collectAsState()
    val isAiSearching by viewModel.isAiSearching.collectAsState()
    
    val filterFeatured by viewModel.filterFeatured.collectAsState()
    val filterVerified by viewModel.filterVerified.collectAsState()
    val filterOpenOnly by viewModel.filterOpenOnly.collectAsState()

    var selectedBusinessForNavigation by remember { mutableStateOf<Business?>(null) }
    var activeInteractionBusiness by remember { mutableStateOf<Business?>(null) }
    var showInquiryDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        
        // 1. Top Integrated Header with Pulsating Location and Switch Tab
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        // Location label in small, bold, tracking-wider, uppercase blue
                        Text(
                            text = "LOCATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TealSecondary,
                            modifier = Modifier.testTag("app_title_header")
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                Toast.makeText(context, "Switching neighborhood regions...", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = "Andheri East, Mum / New Delhi",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = "Expand Location Options",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Pulsator
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(GreenVerified)
                            )
                        }
                    }

                    // Mode Toggle & Profile Group
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Owner Mode Button Toggle
                        Row(
                            modifier = Modifier
                                .background(
                                    color = if (isOwnerMode) TealTertiary.copy(alpha = 0.15f) else TealSecondary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.toggleOwnerMode(!isOwnerMode) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isOwnerMode) Icons.Default.Storefront else Icons.Default.People,
                                contentDescription = "Mode Icon",
                                tint = if (isOwnerMode) TealTertiary else TealSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isOwnerMode) "Owner Mode" else "Resident Mode",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOwnerMode) TealTertiary else TealSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Custom profile icon container from the High Density design template
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(TealSecondary.copy(alpha = 0.1f))
                                .border(1.dp, TealSecondary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile Panel",
                                tint = TealSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))

                // Mode switch description
                Text(
                    text = if (isOwnerMode) {
                        "Register your shop, release amazing discount coupon deals, and resolve incoming leads inquiries in real-time."
                    } else {
                        "Find high-rated plumbers, emergency mechanics, hospitals, and gourmet bakeries near you with instant AI recommendation."
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 15.sp
                )
            }
        }

        // Main switching layout based on modes
        AnimatedContent(
            targetState = isOwnerMode,
            transitionSpec = {
                slideInHorizontally { width -> if (targetState) width else -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> if (targetState) -width else width } + fadeOut()
            },
            label = "ScreenSwitcher"
        ) { targetOwner ->
            if (targetOwner) {
                // BUSINESS OWNER DASHBOARD PANEL
                OwnerDashboardScreen(
                    viewModel = viewModel,
                    businesses = businesses,
                    offers = offers,
                    queries = queries,
                    ownedBizId = ownedBusinessId
                )
            } else {
                // USER / RESIDENT EXPLORE DIRECTORY
                ExploreDirectoryScreen(
                    viewModel = viewModel,
                    businesses = businesses,
                    offers = offers,
                    queries = queries,
                    searchQuery = searchQuery,
                    selectedCat = selectedCategory,
                    aiResult = aiResult,
                    isAiSearching = isAiSearching,
                    filterFeatured = filterFeatured,
                    filterVerified = filterVerified,
                    filterOpenOnly = filterOpenOnly,
                    selectedNavBusiness = selectedBusinessForNavigation,
                    onBusinessNavSelected = { selectedBusinessForNavigation = it },
                    onConnectBusiness = {
                        activeInteractionBusiness = it
                        showInquiryDialog = true
                    }
                )
            }
        }
    }

    // Modal Query Dialog for Resident-to-Service interaction
    if (showInquiryDialog && activeInteractionBusiness != null) {
        val biz = activeInteractionBusiness!!
        var custName by remember { mutableStateOf("") }
        var custPhone by remember { mutableStateOf("") }
        var custText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showInquiryDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Query", tint = TealSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect to ${biz.name}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Lodge visual inquiry, price quote or booking query directly, Shop owners will respond instantly on dashboard.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    OutlinedTextField(
                        value = custName,
                        onValueChange = { custName = it },
                        label = { Text("Aapka Naam / Guest Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inquiry_name_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = custPhone,
                        onValueChange = { custPhone = it },
                        label = { Text("WhatsApp / Contact Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inquiry_phone_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = custText,
                        onValueChange = { custText = it },
                        label = { Text("What service or deal are you seeking?") },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inquiry_desc_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (custName.isNotBlank() && custPhone.isNotBlank() && custText.isNotBlank()) {
                            viewModel.submitQueryToBusiness(biz.id, custName, custPhone, custText)
                            Toast.makeText(context, "Lead inquiry submitted! Shopkeeper notified.", Toast.LENGTH_LONG).show()
                            showInquiryDialog = false
                        } else {
                            Toast.makeText(context, "Please complete all inputs.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealSecondary)
                ) {
                    Text("Submit Query")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInquiryDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

@Composable
fun ExploreDirectoryScreen(
    viewModel: LocalConnectViewModel,
    businesses: List<Business>,
    offers: List<Offer>,
    queries: List<CustomerInquiry>,
    searchQuery: String,
    selectedCat: String,
    aiResult: com.example.data.AIRecommendationResult?,
    isAiSearching: Boolean,
    filterFeatured: Boolean,
    filterVerified: Boolean,
    filterOpenOnly: Boolean,
    selectedNavBusiness: Business?,
    onBusinessNavSelected: (Business?) -> Unit,
    onConnectBusiness: (Business) -> Unit
) {
    val context = LocalContext.current

    // Categorized and smart-filtered list logic
    val filteredBusinesses = remember(
        businesses, selectedCat, searchQuery, aiResult, filterFeatured, filterVerified, filterOpenOnly
    ) {
        var base = businesses
        
        // If an AI recommendation exists, prioritize Recommended business IDs
        if (aiResult != null && aiResult.recommendedIds.isNotEmpty()) {
            base = businesses.filter { aiResult.recommendedIds.contains(it.id) }
        } else {
            // Apply category first
            if (selectedCat != "All") {
                base = base.filter { it.category.equals(selectedCat, ignoreCase = true) }
            }
            // Apply text keyword search filter if AI recommendation has not been triggered/returned yet
            if (searchQuery.isNotBlank() && aiResult == null) {
                val qLower = searchQuery.lowercase()
                base = base.filter {
                    it.name.lowercase().contains(qLower) ||
                            it.address.lowercase().contains(qLower) ||
                            it.category.lowercase().contains(qLower)
                }
            }
        }

        // Apply visual toggles
        if (filterFeatured) base = base.filter { it.isFeatured }
        if (filterVerified) base = base.filter { it.isVerified }
        if (filterOpenOnly) base = base.filter { it.isOpen }

        base
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("explore_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // A. Visual Welcome Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner_1782134138346),
                        contentDescription = "Neighborhood Hub Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = "Explore Local Directory",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Supporting 50+ localized professional services and shops nearby.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // B. NEW: Emergency Services Quick Grid (High Density)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EMERGENCY SERVICES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "View All",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealSecondary,
                        modifier = Modifier.clickable {
                            viewModel.selectCategory("Emergency")
                            Toast.makeText(context, "Showing emergency listings", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Item 1: Doctor
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFFEF2F2), shape = RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFFEE2E2), shape = RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.selectCategory("Health")
                                viewModel.updateSearchQuery("doctor")
                                Toast.makeText(context, "Filtering: Health • Doctor", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEE2E2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.LocalHospital, "Doctor", tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Doctor", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                        }
                    }

                    // Item 2: Plumber
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFFFF7ED), shape = RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFFFEDD5), shape = RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.selectCategory("Emergency")
                                viewModel.updateSearchQuery("plumber")
                                Toast.makeText(context, "Filtering: Emergency • Plumber", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFEDD5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Build, "Plumber", tint = Color(0xFFEA580C), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Plumber", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9A3412))
                        }
                    }

                    // Item 3: Electrician
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFFEFCE8), shape = RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFFEF9C3), shape = RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.selectCategory("Emergency")
                                viewModel.updateSearchQuery("electrical")
                                Toast.makeText(context, "Filtering: Emergency • Electrician", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF9C3)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Bolt, "Electrician", tint = Color(0xFFCA8A04), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Electric", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF854D0E))
                        }
                    }

                    // Item 4: Transport
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFECFDF5), shape = RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFD1FAE5), shape = RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.selectCategory("Emergency")
                                viewModel.updateSearchQuery("transport")
                                Toast.makeText(context, "Filtering: Emergency • Transport", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD1FAE5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.LocalTaxi, "Transport", tint = Color(0xFF059669), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Transport", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                        }
                    }
                }
            }
        }

        // C. Smart Search & AI Concierge Card with Clean high-density styling
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("smart_search_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🔎 Smart Search & AI Assistant",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealSecondary
                        )
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Search",
                            tint = TealSecondary,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    Toast.makeText(context, "Listening... (Talk now in Hinglish/English)", Toast.LENGTH_LONG).show()
                                }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try \"best plumber nearby\" or \"schools\". The real-time integrated AI engine parses local listings.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("What are you seeking nearby?", fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("smart_search_input"),
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, "Clear")
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.performAiSmartSearch() },
                            colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("ask_ai_button")
                        ) {
                            if (isAiSearching) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, "AI", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ask AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Predefined recommendation query chips
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val quickPrompts = listOf(
                            "🔧 Urgent plumber",
                            "🏥 24/7 Pharmacy",
                            "🎓 Tuition center",
                            "🍰 Best pastry",
                            "⭐ Reliable mover"
                        )
                        quickPrompts.forEach { p ->
                            Box(
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        viewModel.updateSearchQuery(p.substring(2))
                                        viewModel.performAiSmartSearch()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(p, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                            }
                        }
                    }

                    // Display AI Output if available - Styled directly like the gorgeous High Density hero banner
                    if (isAiSearching || aiResult != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("ai_recommendation_card"),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = TealSecondary, // Royal Blue accent
                                contentColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                                // Draw high-fidelity overlay design
                                Canvas(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .align(Alignment.TopEnd)
                                ) {
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.08f),
                                        radius = 120f,
                                        center = Offset(110f, -50f)
                                    )
                                }
                                
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color.White.copy(alpha = 0.18f), shape = RoundedCornerShape(20.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "AI RECOMMENDED CONCIERGE",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "AI Active",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (isAiSearching) {
                                        Text(
                                            text = "Analyzing matching local providers in New Delhi directory...",
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.9f),
                                            lineHeight = 16.sp,
                                            modifier = Modifier.testTag("ai_loading_text")
                                        )
                                    } else if (aiResult != null) {
                                        Text(
                                            text = aiResult.explanationText,
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            lineHeight = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.testTag("ai_explanation_text")
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Divider(color = Color.White.copy(alpha = 0.15f), thickness = 1.dp)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Showing ${aiResult.recommendedIds.size} recommended partners",
                                                fontSize = 10.sp,
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontWeight = FontWeight.Bold
                                            )
                                            Button(
                                                onClick = { viewModel.updateSearchQuery("") },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color.White,
                                                    contentColor = TealSecondary
                                                ),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(28.dp).testTag("clear_filter_button")
                                            ) {
                                                Text("Reset Filter", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // C. Category horizontal scroller
        item {
            Column {
                Text(
                    text = "Directory Categories",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val categories = listOf("All", "Health", "Education", "Food/Lifestyle", "Emergency")
                    items(categories) { cat ->
                        val isSelected = selectedCat == cat
                        val catIcon = when(cat) {
                            "Health" -> Icons.Default.LocalHospital
                            "Education" -> Icons.Default.School
                            "Food/Lifestyle" -> Icons.Default.Restaurant
                            "Emergency" -> Icons.Default.Engineering
                            else -> Icons.Default.Category
                        }
                        
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectCategory(cat) },
                            label = { Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(catIcon, contentDescription = cat, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TealSecondary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            ),
                            modifier = Modifier.testTag("category_chip_$cat")
                        )
                    }
                }
            }
        }

        // D. Toggle quick action badges (Featured, Verified, Open Only)
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Featured
                Box(
                    modifier = Modifier
                        .background(
                            color = if (filterFeatured) TealTertiary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable { viewModel.toggleFilterFeatured() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Featured Filter",
                            tint = if (filterFeatured) TealTertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Featured Subscribed",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (filterFeatured) TealTertiary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Verified
                Box(
                    modifier = Modifier
                        .background(
                            color = if (filterVerified) GreenVerified.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable { viewModel.toggleFilterVerified() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Filter",
                            tint = if (filterVerified) GreenVerified else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Premium Verified",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (filterVerified) GreenVerified else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Open Status
                Box(
                    modifier = Modifier
                        .background(
                            color = if (filterOpenOnly) GreenVerified.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable { viewModel.toggleFilterOpenOnly() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (filterOpenOnly) GreenVerified else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Open Now Status",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (filterOpenOnly) GreenVerified else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // E. Custom Map Navigation Canvas Node
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📍 Interactive Neighborhood Map",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (selectedNavBusiness != null) {
                        TextButton(
                            onClick = { onBusinessNavSelected(null) },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Clear Route", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                InteractiveMapCanvas(
                    userLocation = viewModel.userLocation.value,
                    businesses = filteredBusinesses,
                    selectedBusiness = selectedNavBusiness,
                    onBusinessSelected = { onBusinessNavSelected(it) },
                    getDistanceText = { viewModel.getDistanceToBusiness(it) }
                )
            }
        }

        // F. Directory Listed Businesses Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Directory Listings (${filteredBusinesses.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Sorted by features",
                    fontSize = 10.sp,
                    color = TealSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (filteredBusinesses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = "Not found",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No matched local services found",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Try editing filters or reset AI search keyword prompt above.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // G. Loop and draw responsive business items
            items(filteredBusinesses, key = { it.id }) { biz ->
                val isSelectedRoute = selectedNavBusiness?.id == biz.id
                val businessOffers = offers.filter { it.businessId == biz.id }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelectedRoute) 2.dp else 0.dp,
                            color = if (isSelectedRoute) TealSecondary else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("business_item_${biz.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (biz.isFeatured) TealSecondary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (biz.isFeatured) 4.dp else 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = biz.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    if (biz.isVerified) {
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "Verified Premium Shop",
                                            tint = GreenVerified,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = biz.address,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            val badgeColor = when (biz.category.lowercase()) {
                                "health" -> RedEmergency
                                "education" -> BlueInfo
                                "food/lifestyle" -> TealTertiary
                                "emergency" -> RedEmergency
                                else -> TealSecondary
                            }
                            Box(
                                modifier = Modifier
                                    .background(badgeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = biz.category,
                                    color = badgeColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, "Rating", tint = TealTertiary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    "${biz.rating} (${biz.reviewsCount} reviews)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("•", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = biz.feesOrPriceRange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TealSecondary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (biz.isOpen) GreenVerified else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (biz.isOpen) "Open Now" else "Closed",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (biz.isOpen) GreenVerified else Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Navigation, "Distance", tint = TealSecondary, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Estimated: ${viewModel.getDistanceToBusiness(biz)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (biz.isFeatured) {
                                Text(
                                    "Featured Partner",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TealTertiary
                                )
                            }
                        }

                        if (businessOffers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            businessOffers.forEach { offer ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = TealTertiary.copy(alpha = 0.12f)),
                                    border = BorderStroke(1.dp, TealTertiary.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.LocalOffer, "Deal", tint = TealTertiary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1.5f)) {
                                            Text(
                                                offer.title,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TealTertiary
                                            )
                                            Text(
                                                offer.description,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(TealTertiary, shape = RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                "${offer.discountPercent}% OFF",
                                                color = Color.Black,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { onBusinessNavSelected(biz) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelectedRoute) TealSecondary else MaterialTheme.colorScheme.secondaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("nav_button_${biz.id}")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Map, "Show on Map", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (isSelectedRoute) "Routing Active" else "Route Map",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { onConnectBusiness(biz) },
                                colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(38.dp)
                                    .testTag("connect_button_${biz.id}")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Send, "Send Inquiry", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Get Free Quote", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "Initiating phone call to ${biz.phone}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Icon(Icons.Default.Call, "Call Shop", tint = TealSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OwnerDashboardScreen(
    viewModel: LocalConnectViewModel,
    businesses: List<Business>,
    offers: List<Offer>,
    queries: List<CustomerInquiry>,
    ownedBizId: Int?
) {
    val context = LocalContext.current
    val ownedBusinesses = remember(businesses) {
        businesses.filter { it.registeredByOwner || it.id <= 3 }
    }
    
    val selectedBiz = remember(ownedBizId, ownedBusinesses) {
        ownedBusinesses.firstOrNull { it.id == ownedBizId } ?: ownedBusinesses.firstOrNull()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("owner_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "💼 Business Analytics Console",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealTertiary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Brand Views", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text("1,842", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TealSecondary)
                        }
                        Column {
                            Text("Lead Quotes", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            val leadsCount = remember(queries, selectedBiz) {
                                queries.filter { it.businessId == selectedBiz?.id }.size
                            }
                            Text("$leadsCount Active", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TealTertiary)
                        }
                        Column {
                            Text("Earnings / Est", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            val commissionEarned = remember(queries, selectedBiz) {
                                val resolved = queries.filter { it.businessId == selectedBiz?.id && it.isAnswered }.size
                                resolved * 150
                            }
                            Text("₹$commissionEarned", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GreenVerified)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Monetization model details: Lead generation charges flat ₹150 commission lead, verified businesses enjoy premium visual priority ranking slots.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        lineHeight = 14.sp
                    )
                }
            }
        }

        item {
            Text(
                "Select active store to manage:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (ownedBusinesses.isEmpty()) {
                Text(
                    "You do not own any listed storefront. Use registration below to list your shop!",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ownedBusinesses) { biz ->
                        val isSel = selectedBiz?.id == biz.id
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSel) TealSecondary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSel) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.selectOwnedBusiness(biz.id) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (biz.isVerified) {
                                    Icon(Icons.Default.Verified, "Verified", tint = if (isSel) Color.Black else GreenVerified, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = biz.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📥 Incoming Leads & Inquiries",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val relevantQueries = remember(queries, selectedBiz) {
                        queries.filter { it.businessId == selectedBiz?.id }
                    }

                    if (relevantQueries.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No active customer inquiries found for this store.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            relevantQueries.forEach { q ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = q.customerName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (q.isAnswered) "Answered" else "Unanswered Lead",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (q.isAnswered) GreenVerified else TealTertiary
                                            )
                                        }
                                        Text(
                                            text = "Phone: ${q.customerPhone}",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "\"${q.queryText}\"",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        
                                        if (q.isAnswered && q.answer != null) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(TealSecondary.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp))
                                                    .padding(8.dp)
                                            ) {
                                                Column {
                                                    Text("Your response:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TealSecondary)
                                                    Text(q.answer, fontSize = 11.sp)
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            var replyText by remember { mutableStateOf("") }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                OutlinedTextField(
                                                    value = replyText,
                                                    onValueChange = { replyText = it },
                                                    placeholder = { Text("Type quote amount or response...", fontSize = 11.sp) },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(44.dp)
                                                        .testTag("reply_input_${q.id}"),
                                                    singleLine = true
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Button(
                                                    onClick = {
                                                        if (replyText.isNotBlank()) {
                                                            viewModel.answerQuery(q, replyText)
                                                            Toast.makeText(context, "Quote reply saved successfully!", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                                    modifier = Modifier.height(44.dp)
                                                ) {
                                                    Text("Send", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedBiz != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "🎟️ Post Offer Coupon (Active For Customers)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        var offerTitle by remember { mutableStateOf("") }
                        var offerDesc by remember { mutableStateOf("") }
                        var offerDiscount by remember { mutableStateOf("") }
                        var validUntilDate by remember { mutableStateOf("") }

                        OutlinedTextField(
                            value = offerTitle,
                            onValueChange = { offerTitle = it },
                            label = { Text("Offer Slogan (e.g. Flat 30% Off Home Plumbing)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("offer_title_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = offerDesc,
                            onValueChange = { offerDesc = it },
                            label = { Text("Offer Description / Conditions") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("offer_desc_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = offerDiscount,
                                onValueChange = { offerDiscount = it },
                                label = { Text("Discount %") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("offer_discount_input"),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = validUntilDate,
                                onValueChange = { validUntilDate = it },
                                label = { Text("Valid Until") },
                                placeholder = { Text("30 Jul 2026") },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("offer_valid_input"),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val pct = offerDiscount.toIntOrNull() ?: 0
                                if (offerTitle.isNotBlank() && offerDesc.isNotBlank() && pct > 0 && validUntilDate.isNotBlank()) {
                                    viewModel.postNewOffer(offerTitle, offerDesc, pct, validUntilDate)
                                    Toast.makeText(context, "Promotion published! Active in Resident Feed.", Toast.LENGTH_LONG).show()
                                    offerTitle = ""
                                    offerDesc = ""
                                    offerDiscount = ""
                                    validUntilDate = ""
                                } else {
                                    Toast.makeText(context, "Please complete all inputs with valid values.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("publish_offer_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Publish, "Post")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Publish Offer Coupon Deal")
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📝 Register Real-life Local Business Shop",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    var newBizName by remember { mutableStateOf("") }
                    var newBizCat by remember { mutableStateOf("Health") }
                    var newBizPhone by remember { mutableStateOf("") }
                    var newBizAddress by remember { mutableStateOf("") }
                    var newBizFees by remember { mutableStateOf("₹₹") }
                    var isFeaturedSubscribe by remember { mutableStateOf(false) }
                    var isVerifiedSelected by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = newBizName,
                        onValueChange = { newBizName = it },
                        label = { Text("Store/Business Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_biz_name"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Category: ", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        val cats = listOf("Health", "Education", "Food/Lifestyle", "Emergency")
                        cats.forEach { catOption ->
                            val isSel = newBizCat == catOption
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSel) TealSecondary else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { newBizCat = catOption }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .padding(horizontal = 2.dp)
                            ) {
                                Text(catOption, fontSize = 9.sp, color = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newBizPhone,
                        onValueChange = { newBizPhone = it },
                        label = { Text("Owner Hotline Contact Number") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_biz_phone"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newBizAddress,
                        onValueChange = { newBizAddress = it },
                        label = { Text("Google Map Address Line") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_biz_address"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Price range: ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        val feesOptions = listOf("₹", "₹₹", "₹₹₹", "₹₹₹₹")
                        feesOptions.forEach { opt ->
                            val isSel = newBizFees == opt
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSel) TealSecondary else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { newBizFees = opt }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(opt, fontSize = 11.sp, color = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Premium Upgrade Options (Monetization Engine):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TealTertiary)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isFeaturedSubscribe,
                            onCheckedChange = { isFeaturedSubscribe = it },
                            colors = CheckboxDefaults.colors(checkedColor = TealTertiary)
                        )
                        Column {
                            Text("Featured List Slot (₹499/mo subscription)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("App homepage & AI assistant search priority focus.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isVerifiedSelected,
                            onCheckedChange = { isVerifiedSelected = it },
                            colors = CheckboxDefaults.colors(checkedColor = GreenVerified)
                        )
                        Column {
                            Text("Premium Verified Tag (₹299 setup fee)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Green verified shield badge increases trust factor.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (newBizName.isNotBlank() && newBizPhone.isNotBlank() && newBizAddress.isNotBlank()) {
                                viewModel.registerNewBusiness(
                                    name = newBizName,
                                    category = newBizCat,
                                    phone = newBizPhone,
                                    address = newBizAddress,
                                    fees = newBizFees,
                                    isPremiumFeatured = isFeaturedSubscribe,
                                    isPremiumVerified = isVerifiedSelected
                                )
                                Toast.makeText(context, "Congratulations! Store fully registered in local database.", Toast.LENGTH_LONG).show()
                                newBizName = ""
                                newBizPhone = ""
                                newBizAddress = ""
                                isFeaturedSubscribe = false
                                isVerifiedSelected = false
                            } else {
                                Toast.makeText(context, "Please complete all fields.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_biz_submit")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddBusiness, "Register Store")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Register Storefront")
                        }
                    }
                }
            }
        }
    }
}
