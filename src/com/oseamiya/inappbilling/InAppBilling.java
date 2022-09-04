package com.oseamiya.inappbilling;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.android.billingclient.api.*;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.runtime.util.YailList;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class InAppBilling extends AndroidNonvisibleComponent {
    private final Context context;
    private final Activity activity;
    private BillingClient billingClient;
    private final String TEST_PURCHASED_ID = "android.test.purchased";
    private boolean isTest = false;
    private boolean isSubscription = false;
    private boolean isAutoAcknowledge = false;

    private final Handler handler;

    public InAppBilling(ComponentContainer container) {
        super(container.$form());
        context = container.$context();
        activity = (Activity) container.$context();
        handler = new Handler();
        form.registerForOnDestroy(new OnDestroyListener() {
            @Override
            public void onDestroy() {
                billingClient.endConnection();
            }
        });
        form.registerForOnResume(new OnResumeListener() {
            @Override
            public void onResume() {
                billingClient.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder()
                                .setProductType(isSubscription ? BillingClient.ProductType.SUBS : BillingClient.ProductType.INAPP)
                                .build(),
                        new PurchasesResponseListener() {
                            public void onQueryPurchasesResponse(BillingResult billingResult, List purchases) {
                                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                                    for (Object purchase : purchases) {
                                        // since we have only passes a productId at LaunchPurchaseFlow
                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                GotPurchase(purchase);
                                                if (isAutoAcknowledge && !isSubscription) {
                                                    HandleNonConsumable(purchase);
                                                }
                                            }
                                        });
                                    }
                                } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            PurchaseCancelled();
                                        }
                                    });
                                } else {
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            PurchaseFailed(billingResult.getResponseCode());
                                        }
                                    });
                                }

                            }
                        }
                );

            }
        });
        Initialize();
    }

    @DesignerProperty(defaultValue = "False", editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN)
    @SimpleProperty(description = "If enabled, Test product id is used in each case")
    public void Test(boolean test) {
        isTest = test;
    }

    @SimpleProperty(description = "Return if Test is enabled or not")
    public boolean Test() {
        return isTest;
    }

    @DesignerProperty(defaultValue = "False", editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN)
    @SimpleProperty(description = "If enabled then it will automatically handle every purchase as non-consumable")
    public void AutoAcknowledge(boolean autoAcknowledge) {
        isAutoAcknowledge = autoAcknowledge;
    }

    @SimpleProperty(description = "Return if AutoAcknowledge is enabled or not")
    public boolean AutoAcknowledge() {
        return isAutoAcknowledge;
    }

    @SimpleEvent
    public void BillingServiceDisconnect() {
        EventDispatcher.dispatchEvent(this, "BillingServiceDisconnect");
    }

    @SimpleEvent
    public void BillingClientReady() {
        EventDispatcher.dispatchEvent(this, "BillingClientReady");
    }

    @SimpleEvent
    public void FailedToLaunchPurchaseFlow(int responseCode) {
        EventDispatcher.dispatchEvent(this, "FailedToLaunchPurchaseFlow", responseCode);
    }

    @SimpleEvent
    public void GotPurchase(Object purchase) {
        EventDispatcher.dispatchEvent(this, "GotPurchase", purchase);
    }

    @SimpleEvent
    public void PurchaseCancelled() {
        EventDispatcher.dispatchEvent(this, "PurchaseCancelled");
    }

    @SimpleEvent
    public void PurchaseFailed(int responseCode) {
        EventDispatcher.dispatchEvent(this, "PurchaseFailed", responseCode);
    }

    @SimpleEvent
    public void PurchaseStatePending() {
        EventDispatcher.dispatchEvent(this, "PurchaseStatePending");
    }

    @SimpleEvent
    public void PurchaseStateUnspecified() {
        EventDispatcher.dispatchEvent(this, "PurchaseStateUnspecified");
    }

    @SimpleEvent
    public void PurchaseSuccess() {
        EventDispatcher.dispatchEvent(this, "PurchaseSuccess");
    }

    @SimpleEvent
    public void GotPurchaseDetails(String orderId, boolean isAcknowledged, boolean isAutoRenewing, long purchasedTime, String json) {
        EventDispatcher.dispatchEvent(this, "GotPurchaseDetails", orderId, isAcknowledged, isAutoRenewing, purchasedTime, json);
    }


    @SimpleEvent
    public void OnError(String error) {
        EventDispatcher.dispatchEvent(this, "OnError");
    }

    private void Initialize() {
        billingClient = BillingClient.newBuilder(context)
                .setListener(new PurchasesUpdatedListener() {
                    @Override
                    public void onPurchasesUpdated(@NonNull @NotNull BillingResult billingResult, List<Purchase> list) {
                        // Google Play calls this to deliver the result of the purchase operation to this listener
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                            for (Purchase purchase : list) {
                                // since we have only passes a productId at LaunchPurchaseFlow
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        GotPurchase(purchase);
                                        if (isAutoAcknowledge && !isSubscription) {
                                            HandleNonConsumable(purchase);
                                        }
                                    }
                                });
                            }
                        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    PurchaseCancelled();
                                }
                            });
                        } else {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    PurchaseFailed(billingResult.getResponseCode());
                                }
                            });
                        }
                    }
                })
                .enablePendingPurchases()
                .build();
    }

    @SimpleFunction
    public void StartConnection() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        BillingServiceDisconnect();
                    }
                });
            }

            @Override
            public void onBillingSetupFinished(@NonNull @NotNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            BillingClientReady();
                        }
                    });
                }
            }
        });
    }

    @SimpleEvent
    public void ProductDetailsResponseFailed(int responseCode) {
        EventDispatcher.dispatchEvent(this, "ProductDetailsResponseFailed", responseCode);
    }

    @SimpleEvent
    public void OnProductDetailsResponse(String name, String title, String productId, String productType, String description, Object productDetails) {
        EventDispatcher.dispatchEvent(this, "OnProductDetailsResponse", name, title, productId, productType, description, productDetails);
    }

    @SimpleFunction
    public void QueryProductDetails(String productId, String skuType) {
        isSubscription = skuType.equals(BillingClient.ProductType.SUBS);
        String idOfProduct = isTest ? TEST_PURCHASED_ID : productId;
        QueryProductDetailsParams queryProductDetailsParams =
                QueryProductDetailsParams.newBuilder()
                        .setProductList(
                                ImmutableList.of(
                                        QueryProductDetailsParams.Product.newBuilder()
                                                .setProductId(idOfProduct)
                                                .setProductType(skuType)
                                                .build()))
                        .build();

        billingClient.queryProductDetailsAsync(
                queryProductDetailsParams,
                new ProductDetailsResponseListener() {
                    @Override
                    public void onProductDetailsResponse(@NotNull BillingResult billingResult,
                                                         @NotNull List<ProductDetails> productDetailsList) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                    ProductDetails productDetails = productDetailsList.get(0);
                                    OnProductDetailsResponse(productDetails.getName(), productDetails.getTitle(), productDetails.getProductId(), productDetails.getProductType(), productDetails.getDescription(), productDetails);
                                } else {
                                    ProductDetailsResponseFailed(billingResult.getResponseCode());
                                }
                            }
                        }, 2000);

                    }

                }
        );
    }
    @SimpleEvent
    public void OnQueryPurchasesResponse(YailList listOfPurchases, int size){
        EventDispatcher.dispatchEvent(this, "OnQueryPurchasesResponse", listOfPurchases, size);
    }
    @SimpleEvent
    public void OnQueryPurchasesFailed(int responseCode){
        EventDispatcher.dispatchEvent(this, "OnQueryPurchasesFailed", responseCode);
    }
    @SimpleFunction
    public void QueryPurchases(String skuType){
        YailList lists = new YailList();
        billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(skuType).build(), new PurchasesResponseListener() {
            @Override
            public void onQueryPurchasesResponse(@NonNull @NotNull BillingResult billingResult, @NonNull @NotNull List<Purchase> list) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
                            for(Purchase purchase : list){
                                lists.add(purchase);
                            }
                            OnQueryPurchasesResponse(lists, list.size());
                        }else{
                            OnQueryPurchasesFailed(billingResult.getResponseCode());
                        }

                    }
                });
            }
        });
    }

    @SimpleFunction
    public String GetOfferToken(Object productDetails) {
        assert ((ProductDetails) productDetails).getSubscriptionOfferDetails() != null;
        return ((ProductDetails) productDetails).getSubscriptionOfferDetails().get(0).getOfferToken();
    }

    @SimpleFunction
    public void LaunchPurchaseFlow(Object productDetails, String offerToken, boolean isOfferPersonalized, String skuType) {
        isSubscription = skuType.equals(BillingClient.ProductType.SUBS);
        if (billingClient.isReady()) {
            ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                    ImmutableList.of(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails((ProductDetails) productDetails)
                                    .setOfferToken(isSubscription ? offerToken : "")
                                    .build()
                    );
            BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .setIsOfferPersonalized(isOfferPersonalized)
                    .build();

            BillingResult billingResult = billingClient.launchBillingFlow(activity, billingFlowParams);
        } else {
            OnError("Billing Client is not ready");
        }
    }

    @SimpleFunction(description = "The product user consumed again can be purchased")
    public void HandleConsumable(Object purchase) {
        if (purchase instanceof Purchase) {
            Purchase purchase1 = (Purchase) purchase;
            if (purchase1.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                PurchaseStatePending();
            } else if (purchase1.getPurchaseState() == Purchase.PurchaseState.UNSPECIFIED_STATE) {
                PurchaseStateUnspecified();
            } else {
                if (!purchase1.isAcknowledged()) { // TO know if payment is acknowledged, if not it means the payment is not successful/acknowledged yet.
                    ConsumeParams consumeParams = ConsumeParams.newBuilder()
                            .setPurchaseToken(purchase1.getPurchaseToken())
                            .build();
                    ConsumeResponseListener listener = new ConsumeResponseListener() {
                        @Override
                        public void onConsumeResponse(@NonNull @NotNull BillingResult billingResult, @NonNull @NotNull String s) {
                            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        PurchaseSuccess();
                                    }
                                });
                            } else {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        PurchaseFailed(billingResult.getResponseCode());
                                    }
                                });
                            }
                        }
                    };
                    billingClient.consumeAsync(consumeParams, listener);
                }
            }
        } else {
            OnError("purchase is not an instance of Purchase");
        }
    }

    @SimpleFunction(description = "The product can be purchased only once")
    public void HandleNonConsumable(Object purchase) {
        if (purchase instanceof Purchase) {
            Purchase purchase1 = (Purchase) purchase;
            if (purchase1.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                PurchaseStatePending();
            } else if (purchase1.getPurchaseState() == Purchase.PurchaseState.UNSPECIFIED_STATE) {
                PurchaseStateUnspecified();
            } else if (purchase1.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                if (!purchase1.isAcknowledged()) {
                    AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase1.getPurchaseToken())
                            .build();
                    AcknowledgePurchaseResponseListener listener = new AcknowledgePurchaseResponseListener() {
                        @Override
                        public void onAcknowledgePurchaseResponse(@NonNull @NotNull BillingResult billingResult) {
                            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        PurchaseSuccess();
                                    }
                                });
                            } else {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        PurchaseFailed(billingResult.getResponseCode());
                                    }
                                });
                            }
                        }
                    };
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams, listener);
                }
            }
        } else {
            OnError("purchase is not an instance of Purchase");
        }
    }

    @SimpleFunction
    public void GetPurchaseDetails(Object purchase) {
        if (purchase instanceof Purchase) {
            Purchase purchase1 = (Purchase) purchase;
            String orderId = purchase1.getOrderId();
            boolean isAcknowledge = purchase1.isAcknowledged();
            boolean isAutoRenewing = purchase1.isAutoRenewing();
            String originalJson = purchase1.getOriginalJson();
            long purchaseTime = purchase1.getPurchaseTime();
            GotPurchaseDetails(orderId, isAcknowledge, isAutoRenewing, purchaseTime, originalJson);

        } else {
            OnError("purchase is not an instance of Purchase");
        }
    }


    @SimpleFunction
    public boolean IsPurchaseAcknowledged(Object purchase) {
        if (purchase instanceof Purchase) {
            Purchase purchase1 = (Purchase) purchase;
            return purchase1.isAcknowledged();
        }
        return false;
    }

    @SimpleEvent
    public void FailedToGetPurchasesHistory(String error) {
        EventDispatcher.dispatchEvent(this, "FailedToGetPurchasesHistory", error);
    }

    @SimpleEvent
    public void GotPurchasesHistory(List purchasesHistoryList) {
        EventDispatcher.dispatchEvent(this, "GotPurchasesHistory", purchasesHistoryList);
    }

    @SimpleFunction
    public void GetPurchasesHistory(String skuType) {
        billingClient.queryPurchaseHistoryAsync(
                QueryPurchaseHistoryParams.newBuilder()
                        .setProductType(skuType)
                        .build(),
                new PurchaseHistoryResponseListener() {
                    public void onPurchaseHistoryResponse(
                            BillingResult billingResult, List purchasesHistoryList) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                    if (purchasesHistoryList != null) {
                                        GotPurchasesHistory(purchasesHistoryList);
                                    } else {
                                        FailedToGetPurchasesHistory("Purchase History List is null or not found");
                                    }
                                } else {
                                    FailedToGetPurchasesHistory("Failed with billing response code " + billingResult.getResponseCode());
                                }
                            }
                        });
                    }
                }
        );

    }

    @SimpleProperty
    public boolean IsSubscriptionSupported() {
        BillingResult billingResult = billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS);
        return billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK; // If feature is not supported then BillingResponseCode.FEATURE_NOT_SUPPORTED.
    }

    @SimpleProperty
    public boolean IsReady() {
        return billingClient.isReady(); // To check if billing client is ready or not
    }

    @SimpleFunction
    public void EndConnection() {
        billingClient.endConnection(); // No need of it , connection will automatically end on destroy of activity
    }

    @SimpleProperty
    public String InApp() {
        return BillingClient.ProductType.INAPP;
    }

    @SimpleProperty
    public String Subs() {
        return BillingClient.ProductType.SUBS;
    }
}