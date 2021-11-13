package com.oseamiya.inappbilling;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.android.billingclient.api.*;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.OnDestroyListener;
import com.google.appinventor.components.runtime.util.YailList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class InAppBilling extends AndroidNonvisibleComponent{
  private final Context context;
  private final Activity activity;
  private BillingClient billingClient;
  private final String TEST_PURCHASED_ID = "android.test.purchased";
  private boolean isTest = false;
  private boolean isSubscription = false;
  private boolean isAutoAcknowledge = false;
  public InAppBilling(ComponentContainer container) {
    super(container.$form());
    context = container.$context();
    activity = (Activity) container.$context();
    form.registerForOnDestroy(new OnDestroyListener() {
        @Override
        public void onDestroy() {
            billingClient.endConnection();
        }
    });
    Initialize();
  }
  @DesignerProperty(defaultValue = "False", editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN)
  @SimpleProperty(description = "If enabled, Test product id is used in each case")
  public void Test(boolean test){
      isTest = test;
  }
  @SimpleProperty(description = "Return if Test is enabled or not")
  public boolean Test(){
      return isTest;
  }
  @DesignerProperty(defaultValue = "False", editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN)
  @SimpleProperty(description = "If enabled then it will automatically handle every purchase as non-consumable")
  public void AutoAcknowledge(boolean autoAcknowledge){
      isAutoAcknowledge = autoAcknowledge;
  }
  @SimpleProperty(description = "Return if AutoAcknowledge is enabled or not")
  public boolean AutoAcknowledge(){
      return isAutoAcknowledge;
  }
  @SimpleEvent
  public void BillingServiceDisconnect(){
    EventDispatcher.dispatchEvent(this , "BillingServiceDisconnect");
  }
  @SimpleEvent
  public void BillingClientReady(){
    EventDispatcher.dispatchEvent(this , "BillingClientReady");
  }
  @SimpleEvent
  public void FailedToLaunchPurchaseFlow(int responseCode){
    EventDispatcher.dispatchEvent(this , "FailedToLaunchPurchaseFlow", responseCode);
  }
  @SimpleEvent
  public void GotPurchase(Object purchase){
    EventDispatcher.dispatchEvent(this , "GotPurchase", purchase);
  }
  @SimpleEvent
  public void PurchaseCancelled(){
    EventDispatcher.dispatchEvent(this , "PurchaseCancelled");
  }
  @SimpleEvent
  public void PurchaseFailed(int responseCode){
    EventDispatcher.dispatchEvent(this , "PurchaseFailed" , responseCode);
  }
  @SimpleEvent
  public void PurchaseStatePending(){
    EventDispatcher.dispatchEvent(this , "PurchaseStatePending");
  }
  @SimpleEvent
  public void PurchaseStateUnspecified(){
    EventDispatcher.dispatchEvent(this , "PurchaseStateUnspecified");
  }
  @SimpleEvent
  public void PurchaseSuccess(){
    EventDispatcher.dispatchEvent(this , "PurchaseSuccess");
  }
  @SimpleEvent
  public void GotPurchaseDetails(String orderId, boolean isAcknowledged, boolean isAutoRenewing, long purchasedTime, String json){
    EventDispatcher.dispatchEvent(this , "GotPurchaseDetails", orderId , isAcknowledged, isAutoRenewing, purchasedTime, json);
  }
  @SimpleEvent
  public void GotDetails(String title, String description, String price, String json){
    EventDispatcher.dispatchEvent(this, "GotDetails", title, description, price, json);
  }
  @SimpleEvent
  public void FailedToGetPurchasesHistory(String error){
    EventDispatcher.dispatchEvent(this, "FailedToGetPurchasesHistory", error);
  }
  @SimpleEvent
  public void GotPurchasesHistory(YailList productIds, YailList orderIds, YailList jsons){
    EventDispatcher.dispatchEvent(this ,"GotPurchasesHistory", productIds, orderIds, jsons);
  }
  @SimpleEvent
  public void OnError(String error){
    EventDispatcher.dispatchEvent(this , "OnError");
  }
  private void Initialize(){
    billingClient = BillingClient.newBuilder(context)
            .setListener(new PurchasesUpdatedListener() {
              @Override
              public void onPurchasesUpdated(@NonNull @NotNull BillingResult billingResult, List<Purchase> list) {
                // Google Play calls this to deliver the result of the purchase operation to this listener
                if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null){
                  for(Purchase purchase : list){
                    // since we have only passes a productId at LaunchPurchaseFlow
                    activity.runOnUiThread(new Runnable() {
                      @Override
                      public void run() {
                        GotPurchase(purchase);
                        if(isAutoAcknowledge && !isSubscription){
                            HandleNonConsumable(purchase);
                        }
                      }
                    });
                  }
                }else if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED){
                  activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                      PurchaseCancelled();
                    }
                  });
                }else{
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
  public void StartConnection(){
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
  @SimpleFunction
  public void LaunchPurchaseFlow(String productId, String skuType){
    String idOfProduct = isTest ? TEST_PURCHASED_ID : productId;
    isSubscription = skuType.equals(BillingClient.SkuType.SUBS);
    if(billingClient.isReady()){
      List<String> skuList = new ArrayList<>();
      skuList.add(idOfProduct);
      SkuDetailsParams skuDetailsParams = SkuDetailsParams.newBuilder()
              .setSkusList(skuList)
              .setType(skuType)
              .build();
      billingClient.querySkuDetailsAsync(skuDetailsParams, new SkuDetailsResponseListener() {
        @Override
        public void onSkuDetailsResponse(@NonNull @NotNull BillingResult billingResult, List<SkuDetails> list) {
          if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null){
              BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                      .setSkuDetails(list.get(0))
                      .build();
              billingClient.launchBillingFlow(activity , billingFlowParams);
          }else{
            FailedToLaunchPurchaseFlow(billingResult.getResponseCode());
          }
        }
      });

    }else{
      OnError("Billing Client is not ready");
    }
  }
  @SimpleFunction(description = "The product user consumed again can be purchased")
  public void HandleConsumable(Object purchase){
    if(purchase instanceof Purchase){
      Purchase purchase1 = (Purchase) purchase;
      if(purchase1.getPurchaseState() == Purchase.PurchaseState.PENDING){
        PurchaseStatePending();
      }else if(purchase1.getPurchaseState() == Purchase.PurchaseState.UNSPECIFIED_STATE){
        PurchaseStateUnspecified();
      }else{
        if(!purchase1.isAcknowledged()){ // TO know if payment is acknowledged, if not it means the payment is not successful/acknowledged yet.
          ConsumeParams consumeParams = ConsumeParams.newBuilder()
                  .setPurchaseToken(purchase1.getPurchaseToken())
                  .build();
          ConsumeResponseListener listener = new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(@NonNull @NotNull BillingResult billingResult, @NonNull @NotNull String s) {
              if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
                activity.runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                    PurchaseSuccess();
                  }
                });
              }else{
                activity.runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                    PurchaseFailed(billingResult.getResponseCode());
                  }
                });
              }
            }
          };
          billingClient.consumeAsync(consumeParams , listener);
        }
      }
    }else{
      OnError("purchase is not an instance of Purchase");
    }
  }
  @SimpleFunction(description = "The product can be purchased only once")
  public void HandleNonConsumable(Object purchase){
    if(purchase instanceof Purchase){
      Purchase purchase1 = (Purchase) purchase;
      if(purchase1.getPurchaseState() == Purchase.PurchaseState.PENDING){
        PurchaseStatePending();
      }else if(purchase1.getPurchaseState() == Purchase.PurchaseState.UNSPECIFIED_STATE){
        PurchaseStateUnspecified();
      }else if(purchase1.getPurchaseState() == Purchase.PurchaseState.PURCHASED){
        if(!purchase1.isAcknowledged()){
          AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                  .setPurchaseToken(purchase1.getPurchaseToken())
                  .build();
          AcknowledgePurchaseResponseListener listener = new AcknowledgePurchaseResponseListener() {
            @Override
            public void onAcknowledgePurchaseResponse(@NonNull @NotNull BillingResult billingResult) {
              if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
                activity.runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                    PurchaseSuccess();
                  }
                });
              }else{
                activity.runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                    PurchaseFailed(billingResult.getResponseCode());
                  }
                });
              }
            }
          };
          billingClient.acknowledgePurchase(acknowledgePurchaseParams , listener);
        }
      }
    }else{
      OnError("purchase is not an instance of Purchase");
    }
  }
  @SimpleFunction
  public void GetPurchaseDetails(Object purchase){
    if(purchase instanceof Purchase){
      Purchase purchase1 = (Purchase) purchase;
      String orderId = purchase1.getOrderId();
      boolean isAcknowledge = purchase1.isAcknowledged();
      boolean isAutoRenewing = purchase1.isAutoRenewing();
      String originalJson = purchase1.getOriginalJson();
      long purchaseTime = purchase1.getPurchaseTime();
      GotPurchaseDetails(orderId==null?"":orderId, isAcknowledge, isAutoRenewing, purchaseTime, originalJson==null?"":originalJson);
    }else{
      OnError("purchase is not an instance of Purchase");
    }
  }
  @SimpleFunction
  public void GetDetails(String productId, String skuType){
    String idOfProduct = isTest ? TEST_PURCHASED_ID : productId;
    if(billingClient.isReady()){
      List<String> skuList = new ArrayList<>();
      skuList.add(idOfProduct);
      SkuDetailsParams skuDetailsParams = SkuDetailsParams.newBuilder()
              .setSkusList(skuList)
              .setType(skuType)
              .build();
      billingClient.querySkuDetailsAsync(skuDetailsParams, new SkuDetailsResponseListener() {
        @Override
        public void onSkuDetailsResponse(@NonNull @NotNull BillingResult billingResult, @Nullable @org.jetbrains.annotations.Nullable List<SkuDetails> list) {
          if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list!= null){
            SkuDetails skuDetails = list.get(0);
            activity.runOnUiThread(new Runnable() {
              @Override
              public void run() {
                GotDetails(skuDetails.getTitle()==null?"":skuDetails.getTitle() , skuDetails.getDescription()==null?"":skuDetails.getDescription(), skuDetails.getPrice()==null?"" : skuDetails.getPrice(), skuDetails.getOriginalJson()==null?"": skuDetails.getOriginalJson());
              }
            });
          }
        }
      });
    }else{
      OnError("Billing Client is not ready");
    }
  }
  @SimpleFunction
  public boolean IsPurchaseAcknowledged(Object purchase){
    if(purchase instanceof Purchase){
      Purchase purchase1 = (Purchase) purchase;
      return purchase1.isAcknowledged();
    }
    return false;
  }

  @SimpleFunction
  public void GetPurchasesHistory(String skuType){
      Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(skuType);
      List<Purchase> purchases = purchasesResult.getPurchasesList();
      List<String> listOfJsons = new ArrayList<>();
      List<String> listOfSkus = new ArrayList<>();
      List<String> orderIds = new ArrayList<>();
      if(purchasesResult.getBillingResult().getResponseCode() == BillingClient.BillingResponseCode.OK){
          if(purchases != null){
              for(Purchase purchase : purchases){
                  listOfJsons.add(purchase.getOriginalJson());
                  listOfSkus.add(purchase.getSkus().get(0));
                  orderIds.add(purchase.getOrderId());
              }
            GotPurchasesHistory(YailList.makeList(listOfSkus), YailList.makeList(orderIds), YailList.makeList(listOfJsons));
          }else{
            FailedToGetPurchasesHistory("Failed as purchases is null ");
          }
      }else{
        FailedToGetPurchasesHistory("Failed, ResponseCode = " + purchasesResult.getBillingResult().getResponseCode());
      }
  }
  @SimpleProperty
  public boolean IsSubscriptionSupported(){
    BillingResult billingResult = billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS);
    return billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK; // If feature is not supported then BillingResponseCode.FEATURE_NOT_SUPPORTED.
  }
  @SimpleProperty
  public boolean IsReady(){
    return billingClient.isReady(); // To check if billing client is ready or not
  }
  @SimpleFunction
  public void EndConnection(){
    billingClient.endConnection(); // No need of it , connection will automatically end, see constructor -- >>
  }
  @SimpleProperty
  public String InApp(){
    return BillingClient.SkuType.INAPP;
  }
  @SimpleProperty
  public String Subs(){
    return BillingClient.SkuType.SUBS;
  }
}
