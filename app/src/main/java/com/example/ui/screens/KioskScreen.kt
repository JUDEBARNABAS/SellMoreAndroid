package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.ProductEntity
import com.example.model.CartItem
import com.example.model.KioskConstants
import com.example.model.calculateInclusiveVatUgx
import com.example.model.formatUgx
import com.example.ui.components.AdminPinDialog
import com.example.ui.components.HardwareStatusBadge
import com.example.ui.components.productImageRes
import com.example.ui.theme.DispenseEmerald
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.EspressoBg
import com.example.ui.theme.EspressoBorder
import com.example.ui.theme.EspressoSurface
import com.example.ui.theme.EspressoSurfaceCard
import com.example.ui.theme.EspressoSurfaceElevated
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StrawberryPink
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VanillaAmber
import com.example.viewmodel.SellMoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KioskScreen(
    viewModel: SellMoreViewModel,
    onNavigateToAdmin: () -> Unit
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val currentTime by viewModel.currentTimeText.collectAsStateWithLifecycle()
    val activeTransport by viewModel.activeTransport.collectAsStateWithLifecycle()
    val isScreensaverVisible by viewModel.isScreensaverVisible.collectAsStateWithLifecycle()
    val isCartOpen by viewModel.isCartOpen.collectAsStateWithLifecycle()
    val isPaymentOpen by viewModel.isPaymentSheetOpen.collectAsStateWithLifecycle()
    val currentOrderRef by viewModel.currentOrderRef.collectAsStateWithLifecycle()
    val dispenseState by viewModel.dispenseState.collectAsStateWithLifecycle()
    val serialLogs by viewModel.serialLogs.collectAsStateWithLifecycle()
    val latestReceipt by viewModel.latestReceipt.collectAsStateWithLifecycle()
    val banner by viewModel.banner.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val voiceStatus by viewModel.voiceStatusText.collectAsStateWithLifecycle()

    var showPinDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }

    val cartCount = cart.sumOf { it.quantity }
    val cartTotal = cart.sumOf { it.lineTotalUgx }

    Box(modifier = Modifier.fillMaxSize().background(EspressoBg)) {
        Scaffold(
            containerColor = EspressoBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                KioskTopBar(
                    currentTime = currentTime,
                    transport = activeTransport,
                    cartCount = cartCount,
                    onHardwareClick = { showPinDialog = true },
                    onScreensaverClick = { viewModel.setScreensaverVisible(true) },
                    onVoiceClick = { showVoiceDialog = true },
                    onCartClick = { viewModel.openCart(true) },
                    onAdminClick = { showPinDialog = true }
                )
            },
            bottomBar = {
                if (cartCount > 0) {
                    Surface(
                        color = EspressoSurface,
                        border = BorderStroke(1.dp, EspressoBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "$cartCount Sealed Bottle(s) Selected",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextSecondary
                                )
                                Text(
                                    text = formatUgx(cartTotal),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = StrawberryPink
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.openCart(true) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EspressoSurfaceElevated,
                                        contentColor = TextPrimary
                                    ),
                                    modifier = Modifier.testTag("view_cart_bar_button")
                                ) {
                                    Text("View Cart")
                                }
                                Button(
                                    onClick = { viewModel.openPaymentSheet() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DispenseEmerald,
                                        contentColor = EspressoBg
                                    ),
                                    modifier = Modifier.testTag("checkout_bar_button")
                                ) {
                                    Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pay QR", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                if (banner != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (banner!!.isError) ErrorRose.copy(alpha = 0.16f) else DispenseEmerald.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, if (banner!!.isError) ErrorRose else DispenseEmerald),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { viewModel.dismissBanner() }
                    ) {
                        Text(
                            text = banner!!.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (banner!!.isError) ErrorRose else DispenseEmerald,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }

                // Location & Chiller Header Banner
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = EspressoSurfaceCard,
                    border = BorderStroke(1.dp, EspressoBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Chilled 330ml Probiotic Bottles • 4.2°C",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "${KioskConstants.MACHINE_ID} • ${KioskConstants.LOCATION}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = TelemetryCyan.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, TelemetryCyan.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "VAT 18% INCL.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TelemetryCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 270.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(products, key = { it.id }) { product ->
                        YogurtProductCard(
                            product = product,
                            onAddToCart = { viewModel.addToCart(product) }
                        )
                    }
                }
            }
        }

        // Idle Screensaver Overlay
        AnimatedVisibility(
            visible = isScreensaverVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ScreensaverOverlay(
                onWake = { viewModel.setScreensaverVisible(false) }
            )
        }
    }

    // Cart Bottom Sheet
    if (isCartOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.openCart(false) },
            sheetState = sheetState,
            containerColor = EspressoSurface,
            contentColor = TextPrimary
        ) {
            CartDrawerContent(
                cart = cart,
                onUpdateQty = { id, delta -> viewModel.updateCartQuantity(id, delta) },
                onRemove = { id -> viewModel.removeFromCart(id) },
                onProceedToPayment = { viewModel.openPaymentSheet() }
            )
        }
    }

    if (isPaymentOpen) {
        QrPaymentDialog(
            orderRef = currentOrderRef,
            cartItems = cart,
            accounts = accounts,
            onDismiss = { viewModel.closePaymentSheet() },
            onConfirmPhoneQrPayment = { viewModel.confirmPhoneQrPayment() },
            onQrPayloadScanned = { payload -> viewModel.processScannedQrPayment(payload) }
        )
    }

    DispenseProgressDialog(
        state = dispenseState,
        serialLogs = serialLogs,
        onDismiss = { viewModel.hardwareController.dismissDispenseModal() }
    )

    if (latestReceipt != null) {
        DigitalReceiptDialog(
            transaction = latestReceipt!!,
            items = viewModel.repository.decodeItems(latestReceipt!!.itemsJson),
            onDismiss = { viewModel.dismissReceiptModal() }
        )
    }

    if (showVoiceDialog) {
        VoiceOrderDialog(
            voiceStatusText = voiceStatus,
            onTranscriptReady = { viewModel.handleVoiceTranscript(it) },
            onDismiss = {
                viewModel.clearVoiceStatus()
                showVoiceDialog = false
            }
        )
    }

    if (showPinDialog) {
        AdminPinDialog(
            expectedPin = prefs.adminPin,
            onDismiss = { showPinDialog = false },
            onAuthenticated = {
                showPinDialog = false
                onNavigateToAdmin()
            }
        )
    }
}

@Composable
private fun KioskTopBar(
    currentTime: String,
    transport: com.example.model.HardwareTransport,
    cartCount: Int,
    onHardwareClick: () -> Unit,
    onScreensaverClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onCartClick: () -> Unit,
    onAdminClick: () -> Unit
) {
    Surface(
        color = EspressoSurface,
        border = BorderStroke(1.dp, EspressoBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StrawberryPink.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, StrawberryPink.copy(alpha = 0.5f)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.LocalDrink,
                            contentDescription = "SellMore Logo",
                            tint = StrawberryPink
                        )
                    }
                }
                Column {
                    Text(
                        text = "SellMore",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = currentTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = TelemetryCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HardwareStatusBadge(transport = transport, onClick = onHardwareClick)

                FilledTonalIconButton(
                    onClick = onVoiceClick,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = EspressoSurfaceElevated,
                        contentColor = StrawberryPink
                    ),
                    modifier = Modifier.testTag("voice_order_button")
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = "Voice Order")
                }

                FilledTonalIconButton(
                    onClick = onScreensaverClick,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = EspressoSurfaceElevated,
                        contentColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("screensaver_button")
                ) {
                    Icon(Icons.Filled.Bedtime, contentDescription = "Screensaver")
                }

                BadgedBox(
                    badge = {
                        if (cartCount > 0) {
                            Badge(containerColor = StrawberryPink, contentColor = TextPrimary) {
                                Text("$cartCount")
                            }
                        }
                    }
                ) {
                    FilledTonalIconButton(
                        onClick = onCartClick,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = EspressoSurfaceElevated,
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier.testTag("cart_button")
                    ) {
                        Icon(Icons.Filled.ShoppingBag, contentDescription = "Open Cart")
                    }
                }

                FilledTonalIconButton(
                    onClick = onAdminClick,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = EspressoSurfaceElevated,
                        contentColor = TelemetryCyan
                    ),
                    modifier = Modifier.testTag("admin_lock_button")
                ) {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Admin Portal")
                }
            }
        }
    }
}

@Composable
private fun YogurtProductCard(
    product: ProductEntity,
    onAddToCart: () -> Unit
) {
    val stockRatio = (product.stock.toFloat() / product.maxCapacity.coerceAtLeast(1)).coerceIn(0f, 1f)
    val stockColor = when {
        product.stock <= 0 -> ErrorRose
        product.stock <= product.lowStockThreshold -> VanillaAmber
        else -> DispenseEmerald
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = EspressoSurface,
        border = BorderStroke(1.dp, EspressoBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.slotCode}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
            ) {
                Image(
                    painter = painterResource(id = productImageRes(product.drawableKey)),
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, EspressoSurface.copy(alpha = 0.85f))
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EspressoBg.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, TelemetryCyan.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "SLOT ${product.slotCode}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TelemetryCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StrawberryPink.copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = product.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "${product.subtitle} • ${product.calories} cal • ${product.probioticsBillion}B probiotics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stock: ${product.stock}/${product.maxCapacity}",
                        style = MaterialTheme.typography.labelMedium,
                        color = stockColor
                    )
                    Text(
                        text = formatUgx(product.priceUgx),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = StrawberryPink
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { stockRatio },
                    color = stockColor,
                    trackColor = EspressoSurfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onAddToCart,
                    enabled = product.stock > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StrawberryPink,
                        contentColor = TextPrimary,
                        disabledContainerColor = EspressoSurfaceElevated,
                        disabledContentColor = TextMuted
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("add_to_cart_${product.slotCode}")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (product.stock > 0) "Add to Cart" else "Out of Stock",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CartDrawerContent(
    cart: List<CartItem>,
    onUpdateQty: (String, Int) -> Unit,
    onRemove: (String) -> Unit,
    onProceedToPayment: () -> Unit
) {
    val subtotal = cart.sumOf { it.lineTotalUgx }
    val vat = calculateInclusiveVatUgx(subtotal)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Text("Your Yogurt Order", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Text("Single-tap 330ml sealed probiotic bottles", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(14.dp))

        if (cart.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = EspressoSurfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Your cart is empty. Tap any chilled yogurt bottle to add it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(20.dp)
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(cart, key = { it.productId }) { item ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = EspressoSurfaceElevated,
                    border = BorderStroke(1.dp, EspressoBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${item.name} [${item.slotCode}]",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "${formatUgx(item.unitPriceUgx)} each",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onUpdateQty(item.productId, -1) },
                                modifier = Modifier.testTag("cart_dec_${item.slotCode}")
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease", tint = TextPrimary)
                            }
                            Text(
                                text = "${item.quantity}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = TelemetryCyan,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            IconButton(
                                onClick = { onUpdateQty(item.productId, 1) },
                                modifier = Modifier.testTag("cart_inc_${item.slotCode}")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase", tint = TextPrimary)
                            }
                            IconButton(
                                onClick = { onRemove(item.productId) },
                                modifier = Modifier.testTag("cart_rem_${item.slotCode}")
                            ) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove", tint = ErrorRose)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = EspressoBorder)
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Subtotal", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text(formatUgx(subtotal), style = MaterialTheme.typography.labelMedium, color = TextPrimary)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("VAT (18% inclusive)", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text(formatUgx(vat), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total Due", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Text(
                formatUgx(subtotal),
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = StrawberryPink
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onProceedToPayment,
            colors = ButtonDefaults.buttonColors(containerColor = DispenseEmerald, contentColor = EspressoBg),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("proceed_qr_payment_button")
        ) {
            Icon(Icons.Filled.QrCode2, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Proceed to QR Payment", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun ScreensaverOverlay(onWake: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onWake)
            .testTag("screensaver_overlay")
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_kiosk_screensaver),
            contentDescription = "SellMore Screensaver",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            EspressoBg.copy(alpha = 0.35f),
                            EspressoBg.copy(alpha = 0.65f),
                            EspressoBg.copy(alpha = 0.94f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(28.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = StrawberryPink.copy(alpha = 0.22f),
                border = BorderStroke(1.dp, StrawberryPink)
            ) {
                Text(
                    text = "${KioskConstants.MACHINE_ID} • KAMPALA, UGANDA",
                    style = MaterialTheme.typography.labelMedium,
                    color = StrawberryPink,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(id = R.string.screensaver_headline),
                style = MaterialTheme.typography.displayLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Strawberry • Vanilla Bean • Belgian Chocolate (330ml Sealed Bottles)",
                style = MaterialTheme.typography.titleMedium,
                color = TelemetryCyan
            )
            Spacer(modifier = Modifier.height(22.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DispenseEmerald,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.TouchApp, contentDescription = null, tint = EspressoBg)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.screensaver_subtitle),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EspressoBg
                    )
                }
            }
        }
    }
}
