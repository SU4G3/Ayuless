/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.nogms;

import android.app.Activity;
import android.content.Context;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

/** Inert types for optional Play purchase UI. No store, products or receipts exist. */
public final class Billing {
    private Billing() {}
    private static BillingResult unavailable() { return BillingResult.newBuilder().setResponseCode(3).build(); }
    public interface PurchasesUpdatedListener { void onPurchasesUpdated(BillingResult result, List<Purchase> purchases); }
    public interface PurchasesResponseListener { void onQueryPurchasesResponse(BillingResult result, List<Purchase> purchases); }
    public interface BillingClientStateListener {
        void onBillingServiceDisconnected();
        void onBillingSetupFinished(BillingResult result);
    }
    public static final class BillingClient {
        public static final class ProductType { public static final String INAPP="inapp", SUBS="subs"; }
        public static final class BillingResponseCode {
            public static final int SERVICE_TIMEOUT=-3, FEATURE_NOT_SUPPORTED=-2, SERVICE_DISCONNECTED=-1,
                OK=0, USER_CANCELED=1, SERVICE_UNAVAILABLE=2, BILLING_UNAVAILABLE=3, ITEM_UNAVAILABLE=4,
                DEVELOPER_ERROR=5, ERROR=6, ITEM_ALREADY_OWNED=7, ITEM_NOT_OWNED=8, NETWORK_ERROR=12;
        }
        public static Builder newBuilder(Context context) { return new Builder(); }
        public static final class Builder {
            public Builder enablePendingPurchases(PendingPurchasesParams p) { return this; }
            public Builder setListener(PurchasesUpdatedListener l) { return this; }
            public BillingClient build() { return new BillingClient(); }
        }
        public boolean isReady() { return false; }
        public void startConnection(BillingClientStateListener l) { l.onBillingSetupFinished(unavailable()); }
        public void queryProductDetailsAsync(QueryProductDetailsParams p, BiConsumer<BillingResult, QueryProductDetailsResult> l) { l.accept(unavailable(), new QueryProductDetailsResult()); }
        public void queryPurchasesAsync(QueryPurchasesParams p, PurchasesResponseListener l) { l.onQueryPurchasesResponse(unavailable(), Collections.emptyList()); }
        public void consumeAsync(ConsumeParams p, BiConsumer<BillingResult, String> l) { l.accept(unavailable(), ""); }
        public BillingResult launchBillingFlow(Activity a, BillingFlowParams p) { return unavailable(); }
    }
    public static final class BillingResult {
        private int code=3;
        public int getResponseCode() { return code; }
        public String getDebugMessage() { return "Play Billing is unavailable in Ayuless"; }
        public static Builder newBuilder() { return new Builder(); }
        public static final class Builder {
            private final BillingResult result=new BillingResult();
            public Builder setResponseCode(int c) { result.code=c; return this; }
            public BillingResult build() { return result; }
        }
    }
    public static final class QueryProductDetailsResult {
        public List<ProductDetails> getProductDetailsList() { return Collections.emptyList(); }
    }
    public static final class ProductDetails {
        public String getProductId() { return ""; }
        public String getName() { return ""; }
        public String getTitle() { return ""; }
        public String getDescription() { return ""; }
        public String getProductType() { return ""; }
        public List<SubscriptionOfferDetails> getSubscriptionOfferDetails() { return Collections.emptyList(); }
        public OneTimePurchaseOfferDetails getOneTimePurchaseOfferDetails() { return null; }
        public static class OneTimePurchaseOfferDetails {
            public String getFormattedPrice() { return ""; }
            public long getPriceAmountMicros() { return 0; }
            public String getPriceCurrencyCode() { return ""; }
        }
        public static final class PricingPhase extends OneTimePurchaseOfferDetails {
            public String getBillingPeriod() { return ""; }
            public int getBillingCycleCount() { return 0; }
        }
        public static final class PricingPhases {
            public List<PricingPhase> getPricingPhaseList() { return Collections.emptyList(); }
        }
        public static final class SubscriptionOfferDetails {
            public PricingPhases getPricingPhases() { return new PricingPhases(); }
            public String getOfferToken() { return ""; }
        }
    }
    public static final class Purchase {
        public static final class PurchaseState { public static final int PURCHASED=1; }
        public List<String> getProducts() { return Collections.emptyList(); }
        public boolean isAcknowledged() { return false; }
        public String getOrderId() { return null; }
        public String getPurchaseToken() { return ""; }
        public int getPurchaseState() { return 0; }
        public String getOriginalJson() { return ""; }
        public AccountIdentifiers getAccountIdentifiers() { return null; }
    }
    public static final class AccountIdentifiers {
        public String getObfuscatedAccountId() { return null; }
        public String getObfuscatedProfileId() { return null; }
    }
    public static final class QueryProductDetailsParams {
        public static Builder newBuilder() { return new Builder(); }
        public static final class Builder {
            public Builder setProductList(List<Product> p) { return this; }
            public QueryProductDetailsParams build() { return new QueryProductDetailsParams(); }
        }
        public static final class Product {
            public static Builder newBuilder() { return new Builder(); }
            public static final class Builder {
                public Builder setProductId(String id) { return this; }
                public Builder setProductType(String type) { return this; }
                public Product build() { return new Product(); }
            }
        }
    }
    public static final class BillingFlowParams {
        public static final class ProrationMode { public static final int IMMEDIATE_AND_CHARGE_FULL_PRICE=5; }
        public static Builder newBuilder() { return new Builder(); }
        public static final class Builder {
            public Builder setObfuscatedAccountId(String id) { return this; }
            public Builder setObfuscatedProfileId(String id) { return this; }
            public Builder setProductDetailsParamsList(List<ProductDetailsParams> p) { return this; }
            public Builder setSubscriptionUpdateParams(SubscriptionUpdateParams p) { return this; }
            public BillingFlowParams build() { return new BillingFlowParams(); }
        }
        public static final class ProductDetailsParams {
            private ProductDetails details;
            public ProductDetails zza() { return details; }
            public static Builder newBuilder() { return new Builder(); }
            public static final class Builder {
                private final ProductDetailsParams p=new ProductDetailsParams();
                public Builder setProductDetails(ProductDetails d) { p.details=d; return this; }
                public Builder setOfferToken(String token) { return this; }
                public ProductDetailsParams build() { return p; }
            }
        }
        public static final class SubscriptionUpdateParams {
            public static final class ReplacementMode { public static final int CHARGE_FULL_PRICE=5; }
            public static Builder newBuilder() { return new Builder(); }
            public static final class Builder {
                public Builder setOldPurchaseToken(String t) { return this; }
                public Builder setSubscriptionReplacementMode(int mode) { return this; }
                public Builder setReplaceProrationMode(int mode) { return this; }
                public SubscriptionUpdateParams build() { return new SubscriptionUpdateParams(); }
            }
        }
    }
    public static final class PendingPurchasesParams {
        public static Builder newBuilder() { return new Builder(); }
        public static final class Builder {
            public Builder enableOneTimeProducts() { return this; }
            public PendingPurchasesParams build() { return new PendingPurchasesParams(); }
        }
    }
    public static final class ConsumeParams {
        public static Builder newBuilder() { return new Builder(); }
        public static final class Builder {
            public Builder setPurchaseToken(String t) { return this; }
            public ConsumeParams build() { return new ConsumeParams(); }
        }
    }
    public static final class QueryPurchasesParams {
        public static Builder newBuilder() { return new Builder(); }
        public static final class Builder {
            public Builder setProductType(String t) { return this; }
            public QueryPurchasesParams build() { return new QueryPurchasesParams(); }
        }
    }
}
