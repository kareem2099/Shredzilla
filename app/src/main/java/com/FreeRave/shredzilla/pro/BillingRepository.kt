package com.FreeRave.shredzilla.pro

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/** What the UI should react to after talking to Google Play. */
sealed interface BillingEvent {
    data object ProUnlocked : BillingEvent
    data class TipReceived(val tip: SupportTip) : BillingEvent
    /** Paid with a slow method (cash, bank transfer); Play finishes it later and the app picks it up then. */
    data object Pending : BillingEvent
    data class Failed(val message: String) : BillingEvent
}

/**
 * Google Play Billing for Pro and tips. Play is the source of truth for the lifetime purchase:
 * it's restored from Play on every connection, so it follows the user's Google Play account to
 * new devices and reinstalls. The last known answer is cached for offline launches.
 */
class BillingRepository(context: Context) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "Billing"
        private const val PREFS = "pro_billing"
        private const val KEY_OWNS_PRO = "owns_pro"
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var ownsProLifetime by mutableStateOf(prefs.getBoolean(KEY_OWNS_PRO, false))
        private set
    /** Localized prices by product id, e.g. "EGP 99.99"; empty until Play answers. */
    var prices by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    /** False when Play isn't reachable or the products aren't set up (e.g. a sideloaded debug build). */
    var storeAvailable by mutableStateOf(false)
        private set

    private val eventChannel = Channel<BillingEvent>(Channel.BUFFERED)
    /** Queued, so a purchase finishing while nobody listens is still shown later. */
    val events = eventChannel.receiveAsFlow()

    private var productDetails: Map<String, ProductDetails> = emptyMap()

    private val client = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    /** Connects, loads prices and restores purchases. Safe to call repeatedly. */
    fun connect() {
        if (client.isReady) {
            loadProducts()
            restorePurchases()
            return
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    loadProducts()
                    restorePurchases()
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                // enableAutoServiceReconnection retries on the next call
            }
        })
    }

    fun buy(activity: Activity, productId: String) {
        val details = productDetails[productId]
        if (!client.isReady || details == null) {
            eventChannel.trySend(BillingEvent.Failed("Google Play isn't available right now. Try again in a moment."))
            connect()
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()))
            .build()
        client.launchBillingFlow(activity, params)
    }

    /** Re-reads what this Google Play account owns; also finishes purchases that completed while away. */
    fun restorePurchases() {
        if (!client.isReady) return connect()
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            setOwnsPro(purchases.any { ProCatalog.PRO_LIFETIME in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED })
            purchases.forEach { handlePurchase(it, announce = false) }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases.orEmpty().forEach { handlePurchase(it, announce = true) }
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restorePurchases()
            else -> eventChannel.trySend(BillingEvent.Failed("The purchase didn't go through (${result.debugMessage.ifBlank { "code ${result.responseCode}" }})."))
        }
    }

    private fun loadProducts() {
        val products = ProCatalog.allProductIds.map {
            QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(BillingClient.ProductType.INAPP).build()
        }
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(products).build()) { result, details ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            productDetails = details.productDetailsList.associateBy { it.productId }
            prices = productDetails.mapNotNull { (id, d) -> d.oneTimePurchaseOfferDetails?.formattedPrice?.let { id to it } }.toMap()
            storeAvailable = productDetails.isNotEmpty()
        }
    }

    /** Lifetime Pro is acknowledged (or Play refunds it after 3 days); tips are consumed so they can be bought again. */
    private fun handlePurchase(purchase: Purchase, announce: Boolean) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PENDING -> if (announce) eventChannel.trySend(BillingEvent.Pending)
            Purchase.PurchaseState.PURCHASED -> {
                if (ProCatalog.PRO_LIFETIME in purchase.products) {
                    setOwnsPro(true)
                    if (!purchase.isAcknowledged) {
                        client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) {
                            if (it.responseCode == BillingClient.BillingResponseCode.OK) eventChannel.trySend(BillingEvent.ProUnlocked)
                        }
                    } else if (announce) {
                        eventChannel.trySend(BillingEvent.ProUnlocked)
                    }
                }
                purchase.products.mapNotNull(SupportTip::forProduct).forEach { tip ->
                    client.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) { result, _ ->
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) eventChannel.trySend(BillingEvent.TipReceived(tip))
                    }
                }
            }
            else -> Unit
        }
    }

    private fun setOwnsPro(owns: Boolean) {
        ownsProLifetime = owns
        prefs.edit().putBoolean(KEY_OWNS_PRO, owns).apply()
    }
}
