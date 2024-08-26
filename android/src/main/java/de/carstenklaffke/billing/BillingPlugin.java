package de.carstenklaffke.billing;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CapacitorPlugin()
public class BillingPlugin extends Plugin {

    private BillingClient createNewBillingClient(PurchasesUpdatedListener listener) {
        return BillingClient.newBuilder(bridge.getActivity())
                .setListener(listener)
                .enablePendingPurchases()
                .build();
    }

    private void startBillingClientConnection(BillingClient billingClient, BillingClientStateListener listener) {
        billingClient.startConnection(listener);
    }

    private PurchasesUpdatedListener createPurchasesUpdatedListener(final PluginCall call) {
        return (billingResult, purchases) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (Purchase purchase : purchases) {
                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                        try {
                            JSObject ret = new JSObject(purchase.getOriginalJson());
                            call.resolve(ret);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                call.reject("Purchase canceled");
            } else {
                call.reject("Error during purchase: " + billingResult.getDebugMessage());
            }
        };
    }

    @PluginMethod()
    public void querySkuDetails(final PluginCall call) {
        BillingClient billingClient = createNewBillingClient((billingResult, purchases) -> { /* Empty listener */ });

        startBillingClientConnection(billingClient, new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
                    productList.add(QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(call.getString("product", "fullversion"))
                            .setProductType(call.getString("type", "INAPP").equals("SUBS") ? BillingClient.ProductType.SUBS : BillingClient.ProductType.INAPP)
                            .build());

                    QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                            .setProductList(productList)
                            .build();

                    billingClient.queryProductDetailsAsync(params, (billingResult1, productDetailsList) -> {
                        if (billingResult1.getResponseCode() == BillingClient.BillingResponseCode.OK && productDetailsList != null && !productDetailsList.isEmpty()) {
                            ProductDetails productDetails = productDetailsList.get(0);
                            JSObject ret = new JSObject();
                            ret.put("productId", productDetails.getProductId());
                            ret.put("title", productDetails.getName());
                            ret.put("description", productDetails.getDescription());

                            if (productDetails.getOneTimePurchaseOfferDetails() != null) {
                                ProductDetails.OneTimePurchaseOfferDetails offerDetails = productDetails.getOneTimePurchaseOfferDetails();
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
                                ret.put("billing_period", pricingPhase.getBillingPeriod());
                                ret.put("recurrence_mode", pricingPhase.getRecurrenceMode());
                            }

                            call.resolve(ret);
                        } else {
                            call.reject("Error retrieving product details: " + billingResult1.getDebugMessage());
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
                    List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
                    productList.add(QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(call.getString("product", "fullversion"))
                            .setProductType(call.getString("type", "INAPP").equals("SUBS") ? BillingClient.ProductType.SUBS : BillingClient.ProductType.INAPP)
                            .build());

                    QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                            .setProductList(productList)
                            .build();

                    billingClient.queryProductDetailsAsync(params, (billingResult1, productDetailsList) -> {
                        if (billingResult1.getResponseCode() == BillingClient.BillingResponseCode.OK && productDetailsList != null && !productDetailsList.isEmpty()) {
                            ProductDetails productDetails = productDetailsList.get(0);

                            if (productDetails.getSubscriptionOfferDetails() != null && !productDetails.getSubscriptionOfferDetails().isEmpty()) {
                                ProductDetails.SubscriptionOfferDetails subscriptionOfferDetails = productDetails.getSubscriptionOfferDetails().get(0);
                                String offerToken = subscriptionOfferDetails.getOfferToken();

                                BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                        .setProductDetailsParamsList(Arrays.asList(
                                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                                        .setProductDetails(productDetails)
                                                        .setOfferToken(offerToken)
                                                        .build()))
                                        .build();

                                BillingResult billingResult2 = billingClient.launchBillingFlow(bridge.getActivity(), billingFlowParams);
                                if (billingResult2.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                                    call.reject("Error launching billing flow: " + billingResult2.getDebugMessage());
                                }
                            } else {
                                BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                        .setProductDetailsParamsList(Arrays.asList(
                                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                                        .setProductDetails(productDetails)
                                                        .build()))
                                        .build();

                                BillingResult billingResult2 = billingClient.launchBillingFlow(bridge.getActivity(), billingFlowParams);
                                if (billingResult2.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                                    call.reject("Error launching billing flow: " + billingResult2.getDebugMessage());
                                }
                            }
                        } else {
                            call.reject("Error retrieving product details: " + billingResult1.getDebugMessage());
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

