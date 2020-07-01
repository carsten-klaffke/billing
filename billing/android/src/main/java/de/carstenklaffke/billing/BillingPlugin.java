package de.carstenklaffke.billing;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

@NativePlugin()
public class BillingPlugin extends Plugin {

    private BillingClient billingClient;

    @PluginMethod()
    public void querySkuDetails(final PluginCall call) {
        billingClient = BillingClient.newBuilder(bridge.getActivity())
                .setListener(new PurchasesUpdatedListener() {

                    @Override
                    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> list) {

                    }
                })
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    List<String> skuList = new ArrayList<>();
                    skuList.add("fullversion");
                    SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                    params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
                    billingClient.querySkuDetailsAsync(params.build(),
                            new SkuDetailsResponseListener() {
                                @Override
                                public void onSkuDetailsResponse(BillingResult billingResult,
                                                                 List<SkuDetails> skuDetailsList) {
                                    if (skuDetailsList != null && skuDetailsList.size() > 0) {
                                        JSObject ret = null;
                                        try {
                                            ret = new JSObject(skuDetailsList.get(0).getOriginalJson());
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        call.resolve(ret);
                                    }
                                }
                            });
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        });

    }

    @PluginMethod()
    public void launchBillingFlow(final PluginCall call) {
        billingClient = BillingClient.newBuilder(bridge.getActivity())
                .setListener(new PurchasesUpdatedListener() {

                    @Override
                    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                                && purchases != null) {
                            if (purchases != null && purchases.size() > 0) {
                                JSObject ret = null;
                                try {
                                    ret = new JSObject(purchases.get(0).getOriginalJson());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                call.resolve(ret);
                            }
                        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                            call.reject("canceled");
                        } else {
                            call.reject("error");
                        }
                    }
                })
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(new

                                              BillingClientStateListener() {
                                                  @Override
                                                  public void onBillingSetupFinished(BillingResult billingResult) {
                                                      if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                                          List<String> skuList = new ArrayList<>();
                                                          skuList.add("fullversion");
                                                          SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                                                          params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
                                                          billingClient.querySkuDetailsAsync(params.build(),
                                                                  new SkuDetailsResponseListener() {
                                                                      @Override
                                                                      public void onSkuDetailsResponse(BillingResult billingResult,
                                                                                                       List<SkuDetails> skuDetailsList) {
                                                                          if (skuDetailsList != null && skuDetailsList.size() > 0) {

                                                                              BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                                                                      .setSkuDetails(skuDetailsList.get(0))
                                                                                      .build();
                                                                              billingClient.launchBillingFlow(bridge.getActivity(), billingFlowParams);

                                                                          }
                                                                      }
                                                                  });
                                                      }
                                                  }

                                                  @Override
                                                  public void onBillingServiceDisconnected() {
                                                      // Try to restart the connection on the next request to
                                                      // Google Play by calling the startConnection() method.
                                                  }
                                              });


    }
}