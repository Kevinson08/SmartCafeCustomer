package uk.ac.dmu.smartcafe

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val viewModel: CafeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartCafeTheme {
                SmartCafeApp(viewModel)
            }
        }
    }
}

@Composable
fun SmartCafeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        content = content
    )
}

@Composable
fun SmartCafeApp(viewModel: CafeViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val prefs = viewModel.privacyPreferences.collectAsStateWithLifecycle(initialValue = PrivacyPreferences(null, null, false, false, false, false)).value

    if (!isLoggedIn) {
        AuthScreen(viewModel)
    } else if (prefs.termsVersion != PrivacyStore.TERMS_VERSION) {
        TermsGateScreen(viewModel)
    } else {
        MainScreen(viewModel, prefs)
    }
}

@Composable
fun AuthScreen(viewModel: CafeViewModel) {
    var isRegister by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val error by viewModel.authError.collectAsStateWithLifecycle()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(if (isRegister) "Register" else "Sign In", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation())
            if (error != null) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                if (isRegister) viewModel.register(email, password)
                else viewModel.login(email, password)
            }) {
                Text(if (isRegister) "Register" else "Sign In")
            }
            TextButton(onClick = { isRegister = !isRegister }) {
                Text(if (isRegister) "Already have an account? Sign In" else "Don't have an account? Register")
            }
        }
    }
}

@Composable
fun TermsGateScreen(viewModel: CafeViewModel) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Privacy & Terms", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text("By using this application, you must agree to our terms of service and privacy policy. We process your order information to fulfill our contract with you (UK GDPR Art. 6(1)(b)).")
            Spacer(modifier = Modifier.height(8.dp))
            Text("Optional features such as analytics, location, local AI sentiment analysis, and photos require your explicit consent (UK GDPR Art. 6(1)(a)). You can enable or disable these at any time in the Privacy tab.")
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { viewModel.acceptTerms() }) {
                Text("Accept Terms (${PrivacyStore.TERMS_VERSION})")
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: CafeViewModel, prefs: PrivacyPreferences) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Menu", "Cart", "Orders", "Review", "Privacy")
    val icons = listOf(Icons.Default.Menu, Icons.Default.ShoppingCart, Icons.Default.List, Icons.Default.Star, Icons.Default.Lock)

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icons[index], contentDescription = title) },
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> MenuTab(viewModel, prefs)
                1 -> CartTab(viewModel)
                2 -> OrdersTab(viewModel)
                3 -> ReviewTab(viewModel, prefs)
                4 -> PrivacyTab(viewModel, prefs)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuTab(viewModel: CafeViewModel, prefs: PrivacyPreferences) {
    val items = remember { viewModel.getMenuItems() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var branch by remember { mutableStateOf("Main Branch") }
    var showLocationRationale by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            branch = "Nearest Branch (Auto)"
        }
    }

    if (showLocationRationale) {
        AlertDialog(
            onDismissRequest = { showLocationRationale = false },
            title = { Text("Location Permission") },
            text = { Text("We need approximate location to find the nearest branch.") },
            confirmButton = {
                TextButton(onClick = {
                    showLocationRationale = false
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = { showLocationRationale = false }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = branch,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Branch") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Main Branch") }, onClick = { branch = "Main Branch"; expanded = false })
                    DropdownMenuItem(text = { Text("North Branch") }, onClick = { branch = "North Branch"; expanded = false })
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { showLocationRationale = true }) {
                Text("Find Nearest")
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            listOf("All", "Drinks", "Cakes").forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        val filteredItems = items.filter {
            (selectedCategory == "All" || it.category == selectedCategory) &&
            it.name.contains(searchQuery, ignoreCase = true)
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredItems) { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium)
                            Text(item.description, style = MaterialTheme.typography.bodyMedium)
                            Text("£${"%.2f".format(item.price)}", style = MaterialTheme.typography.labelLarge)
                        }
                        Button(onClick = { viewModel.addToCart(item.id, item.name, item.price) }, enabled = item.available) {
                            Text(if (item.available) "Add to Cart" else "Unavailable")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartTab(viewModel: CafeViewModel) {
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val total by viewModel.cartTotal.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Your Cart", style = MaterialTheme.typography.headlineMedium)
        if (cart.isEmpty()) {
            Text("Cart is empty", modifier = Modifier.padding(top = 16.dp))
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(cart) { item ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.name, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.updateQuantity(item.itemId, item.quantity - 1) }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease")
                        }
                        Text("${item.quantity}")
                        IconButton(onClick = { viewModel.updateQuantity(item.itemId, item.quantity + 1) }) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase")
                        }
                        IconButton(onClick = { viewModel.removeFromCart(item.itemId) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove")
                        }
                        Text("£${"%.2f".format(item.price * item.quantity)}")
                    }
                }
            }
            Divider()
            Text("Total: £${"%.2f".format(total)}", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 8.dp))
            Text("Notice: This is a simulated payment. No real transaction will occur.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { viewModel.placeOrder("Main Branch") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Place Simulated Order")
            }
        }
    }
}

@Composable
fun OrdersTab(viewModel: CafeViewModel) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val currentOrder by viewModel.currentOrder.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshOrders()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        currentOrder?.let {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                Text("Latest Order Update: ${it.status}", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        Text("Order History", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
        Text("Note: Orders are retained for 30 days for accounting purposes.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(orders) { order ->
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Order from ${order.branchName}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Badge { Text(order.status) }
                        }
                        Text(order.itemsSummary, style = MaterialTheme.typography.bodyMedium)
                        Text("Total: £${"%.2f".format(order.totalPrice)}", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewTab(viewModel: CafeViewModel, prefs: PrivacyPreferences) {
    val reviews by viewModel.reviews.collectAsStateWithLifecycle()
    var rating by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var sentimentLabel by remember { mutableStateOf("N/A") }
    var sentimentScore by remember { mutableStateOf(0.0) }
    var photoBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }


    LaunchedEffect(Unit) {
        viewModel.refreshReviews()
    }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        photoBitmap = bitmap
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Leave a Review", style = MaterialTheme.typography.headlineMedium)
        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            for (i in 1..5) {
                IconButton(onClick = { rating = i }) {
                    Icon(if (i <= rating) Icons.Default.Star else Icons.Default.Star, contentDescription = "Star $i", tint = if (i <= rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        OutlinedTextField(
            value = comment,
            onValueChange = { if (it.length <= 250) comment = it },
            label = { Text("Comment (max 250 chars)") },
            modifier = Modifier.fillMaxWidth()
        )
        Text("${comment.length}/250", style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.End))

        if (prefs.localAi) {
            Button(onClick = {
                val score = if (comment.contains("good", ignoreCase = true) || comment.contains("great", ignoreCase = true)) 0.9 else 0.1
                sentimentScore = score
                sentimentLabel = if (score > 0.5) "Positive" else "Negative"
            }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Classify Sentiment Locally")
            }
            if (sentimentLabel != "N/A") {
                Text("Local AI Sentiment: $sentimentLabel (Score: $sentimentScore)", style = MaterialTheme.typography.bodyMedium)
                Text("Note: Model is a basic Naive Bayes mockup and struggles with negation (e.g. 'not good').", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }

        if (prefs.photos) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = { photoLauncher.launch(null) }) {
                    Text("Attach Photo Preview")
                }
                if (photoBitmap != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { photoBitmap = null }) {
                        Text("Remove Photo")
                    }
                }
            }
            photoBitmap?.let {
                Image(bitmap = it.asImageBitmap(), contentDescription = "Review Photo", modifier = Modifier.size(100.dp).padding(top = 8.dp))
            }
        }

        Button(onClick = {
            viewModel.saveReview(rating, comment, sentimentLabel)
            comment = ""
            rating = 5
            sentimentLabel = "N/A"
            photoBitmap = null
        }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text("Submit Review")
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))
        Text("Saved Local Reviews", style = MaterialTheme.typography.titleMedium)
        reviews.forEach { rev ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Rating: ${rev.rating}/5")
                    Text(rev.comment)
                    if (rev.sentimentLabel != "N/A") {
                        Text("Sentiment: ${rev.sentimentLabel}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacyTab(viewModel: CafeViewModel, prefs: PrivacyPreferences) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Privacy Settings", style = MaterialTheme.typography.headlineMedium)
        Text("Control your data locally.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Analytics", modifier = Modifier.weight(1f))
            Switch(checked = prefs.analytics, onCheckedChange = { viewModel.updatePrivacyPreference("analytics", it) })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("On-Device Sentiment", modifier = Modifier.weight(1f))
            Switch(checked = prefs.localAi, onCheckedChange = { viewModel.updatePrivacyPreference("localAi", it) })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Approximate Branch Location", modifier = Modifier.weight(1f))
            Switch(checked = prefs.location, onCheckedChange = { viewModel.updatePrivacyPreference("location", it) })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Feedback Photo Preview", modifier = Modifier.weight(1f))
            Switch(checked = prefs.photos, onCheckedChange = { viewModel.updatePrivacyPreference("photos", it) })
        }

        if (prefs.analytics) {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Analytics Summary", style = MaterialTheme.typography.titleMedium)
                    Text("Total Orders: 50")
                    Text("Collected Orders: 40")
                    Text("Completion Rate: 80%")
                }
            }
        }
    }
}