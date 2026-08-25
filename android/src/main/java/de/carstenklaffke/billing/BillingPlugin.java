package de.carstenklaffke.billing;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.android.billingclient.api.UnfetchedProduct;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@CapacitorPlugin()
public class BillingPlugin extends Plugin {

    /** Clearer errors when Play returns empty list or BillingResult without debugMessage. */
    private void rejectQueryProductDetailsFailure(
            final PluginCall call,
            final BillingResult billingResult,
            final QueryProductDetailsResult queryResult) {
        int code = billingResult.getResponseCode();
        String dbg = billingResult.getDebugMessage();
        if (dbg == null) {
            dbg = "";
        }
        String productId = call.getString("product", "fullversion");
        String productType = call.getString("type", "INAPP");
        List<ProductDetails> productDetailsList = fetchedProductDetails(queryResult);
        if (code == BillingClient.BillingResponseCode.OK
                && (productDetailsList == null || productDetailsList.isEmpty())) {
            String unfetched = unfetchedSuffix(queryResult);
            call.reject(
                    "Error retrieving product details: Play returned no product for productId=\""
                            + productId
                            + "\" type=\""
                            + productType
                            + "\""
                            + unfetched
                            + ". Add this managed product or subscription in Play Console for the same applicationId as this app, publish it (e.g. internal testing), and install a build signed with a key Play knows for that listing.");
        } else {
            String suffix = dbg.isEmpty() ? ("billingResponseCode=" + code) : ("billingResponseCode=" + code + ": " + dbg);
            call.reject("Error retrieving product details: " + suffix);
        }
    }

    private static List<ProductDetails> fetchedProductDetails(QueryProductDetailsResult queryResult) {
        if (queryResult == null) {
            return null;
        }
        return queryResult.getProductDetailsList();
    }

    private static String unfetchedSuffix(QueryProductDetailsResult queryResult) {
        if (queryResult == null || queryResult.getUnfetchedProductList() == null
                || queryResult.getUnfetchedProductList().isEmpty()) {
            return "";
        }
        UnfetchedProduct unfetched = queryResult.getUnfetchedProductList().get(0);
        return " (unfetchedStatus=" + unfetched.getStatusCode() + ")";
    }

    private BillingClient createNewBillingClient(PurchasesUpdatedListener listener) {
        PendingPurchasesParams pendingPurchasesParams = PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build();
        return BillingClient.newBuilder(bridge.getActivity())
                .setListener(listener)
                .enablePendingPurchases(pendingPurchasesParams)
                .enableAutoServiceReconnection()
                .build();
    }

    private void startBillingClientConnection(BillingClient billingClient, BillingClientStateListener listener) {
        billingClient.startConnection(listener);
    }

    private PurchasesUpdatedListener createPurchasesUpdatedListener(final PluginCall call) {
        return (billingResult, purchases) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                boolean pendingOnly = false;
                for (Purchase purchase : purchases) {
                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                        try {
                            JSObject ret = new JSObject(purchase.getOriginalJson());
                            call.resolve(ret);
                        } catch (JSONException e) {
                            call.reject("Error parsing purchase: " + e.getMessage());
                        }
                        return;
                    }
                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                        pendingOnly = true;
                    }
                }
                if (!pendingOnly) {
                    call.reject("Purchase update contained no purchased item");
                }
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                call.reject("Purchase canceled");
            } else {
                String dbg = billingResult.getDebugMessage();
                if (dbg == null || dbg.isEmpty()) {
                    call.reject("Error during purchase: billingResponseCode=" + billingResult.getResponseCode());
                } else {
                    call.reject("Error during purchase: " + dbg);
                }
            }
        };
    }

    private QueryProductDetailsParams productDetailsParamsFromCall(PluginCall call) {
        QueryProductDetailsParams.Product product = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(call.getString("product", "fullversion"))
                .setProductType(call.getString("type", "INAPP").equals("SUBS")
                        ? BillingClient.ProductType.SUBS
                        : BillingClient.ProductType.INAPP)
                .build();
        return QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(product))
                .build();
    }

    private ProductDetails.OneTimePurchaseOfferDetails firstOneTimeOffer(ProductDetails productDetails) {
        List<ProductDetails.OneTimePurchaseOfferDetails> offers = productDetails.getOneTimePurchaseOfferDetailsList();
        if (offers != null && !offers.isEmpty()) {
            return offers.get(0);
        }
        return productDetails.getOneTimePurchaseOfferDetails();
    }

    private BillingFlowParams.ProductDetailsParams productDetailsParamsForPurchase(ProductDetails productDetails) {
        BillingFlowParams.ProductDetailsParams.Builder builder = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails);

        if (productDetails.getSubscriptionOfferDetails() != null
                && !productDetails.getSubscriptionOfferDetails().isEmpty()) {
            builder.setOfferToken(productDetails.getSubscriptionOfferDetails().get(0).getOfferToken());
        } else {
            ProductDetails.OneTimePurchaseOfferDetails offer = firstOneTimeOffer(productDetails);
            if (offer != null) {
                String offerToken = offer.getOfferToken();
                if (offerToken != null && !offerToken.isEmpty()) {
                    builder.setOfferToken(offerToken);
                }
            }
        }
        return builder.build();
    }

    @PluginMethod()
    public void querySkuDetails(final PluginCall call) {
        BillingClient billingClient = createNewBillingClient((billingResult, purchases) -> { /* Empty listener */ });

        startBillingClientConnection(billingClient, new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    billingClient.queryProductDetailsAsync(productDetailsParamsFromCall(call), (billingResult1, queryResult) -> {
                        List<ProductDetails> productDetailsList = fetchedProductDetails(queryResult);
                        if (billingResult1.getResponseCode() == BillingClient.BillingResponseCode.OK
                                && productDetailsList != null && !productDetailsList.isEmpty()) {
                            ProductDetails productDetails = productDetailsList.get(0);
                            JSObject ret = new JSObject();
                            ret.put("productId", productDetails.getProductId());
                            ret.put("title", productDetails.getName());
                            ret.put("description", productDetails.getDescription());

                            ProductDetails.OneTimePurchaseOfferDetails offerDetails = firstOneTimeOffer(productDetails);
                            if (offerDetails != null) {
                                ret.put("price", offerDetails.getFormattedPrice());
                                ret.put("price_amount_micros", offerDetails.getPriceAmountMicros());
                                ret.put("price_currency_code", offerDetails.getPriceCurrencyCode());
                            }

                            if (productDetails.getSubscriptionOfferDetails() != null && !productDetails.getSubscriptionOfferDetails().isEmpty()) {
                                ProductDetails.SubscriptionOfferDetails subscriptionOfferDetails = productDetails.getSubscriptionOfferDetails().get(0);
                                ProductDetails.PricingPhases pricingPhases = subscriptionOfferDetails.getPricingPhases();
                                ProductDetails.PricingPhase pricingPhase = pricingPhases.getPricingPhaseList().get(0);

                                ret.put("price", pricingPhase.getFormattedPrice());
                                ret.put("price_amount_micros", pricingPhase.getPriceAmountMicros());
                                ret.put("currency_code", pricingPhase.getPriceCurrencyCode());
                                ret.put("price_currency_code", pricingPhase.getPriceCurrencyCode());
                                ret.put("billing_period", pricingPhase.getBillingPeriod());
                                ret.put("recurrence_mode", pricingPhase.getRecurrenceMode());
                            }

                            call.resolve(ret);
                        } else {
                            rejectQueryProductDetailsFailure(call, billingResult1, queryResult);
                        }
                    });
                } else {
                    call.reject("Billing service not connected");
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                call.reject("Billing service disconnected");
            }
        });
    }

    @PluginMethod()
    public void launchBillingFlow(final PluginCall call) {
        BillingClient billingClient = createNewBillingClient(createPurchasesUpdatedListener(call));

        startBillingClientConnection(billingClient, new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    billingClient.queryProductDetailsAsync(productDetailsParamsFromCall(call), (billingResult1, queryResult) -> {
                        List<ProductDetails> productDetailsList = fetchedProductDetails(queryResult);
                        if (billingResult1.getResponseCode() == BillingClient.BillingResponseCode.OK
                                && productDetailsList != null && !productDetailsList.isEmpty()) {
                            ProductDetails productDetails = productDetailsList.get(0);
                            BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                    .setProductDetailsParamsList(Arrays.asList(
                                            productDetailsParamsForPurchase(productDetails)))
                                    .build();

                            BillingResult billingResult2 = billingClient.launchBillingFlow(bridge.getActivity(), billingFlowParams);
                            if (billingResult2.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                                call.reject("Error launching billing flow: " + billingResult2.getDebugMessage());
                            }
                        } else {
                            rejectQueryProductDetailsFailure(call, billingResult1, queryResult);
                        }
                    });
                } else {
                    call.reject("Billing service not connected");
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                call.reject("Billing service disconnected");
            }
        });
    }

    @PluginMethod()
    public void sendAck(final PluginCall call) {
        BillingClient billingClient = createNewBillingClient((billingResult, purchases) -> { /* Empty listener */ });

        startBillingClientConnection(billingClient, new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(call.getString("purchaseToken"))
                            .build();
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult1 -> {
                        if (billingResult1.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            call.resolve();
                        } else {
                            call.reject("Error acknowledging purchase: " + billingResult1.getDebugMessage());
                        }
                    });
                } else {
                    call.reject("Billing service not connected");
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                call.reject("Billing service disconnected");
            }
        });
    }
}
