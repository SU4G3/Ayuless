/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.nogms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/** Failure-only adapters for optional Google flows. Never creates credentials or tokens. */
public final class Google {
    private Google() {}
    public static final class Task<T> {
        private final T value;
        private final Exception error;
        private static final Executor MAIN = command -> new Handler(Looper.getMainLooper()).post(command);
        private Task(T value, Exception error) { this.value=value; this.error=error; }
        public static <T> Task<T> failed() { return new Task<>(null, new UnsupportedOperationException("Unavailable in Ayuless")); }
        public static <T> Task<T> result(T value) { return new Task<>(value, null); }
        public boolean isSuccessful() { return error == null; }
        public boolean isCanceled() { return false; }
        public Exception getException() { return error; }
        public T getResult() { if (error != null) throw new IllegalStateException(error); return value; }
        public <E extends Throwable> T getResult(Class<E> type) throws E {
            if (error != null) {
                if (type.isInstance(error)) throw type.cast(error);
                if (type == ApiException.class) throw type.cast(new ApiException());
                throw new IllegalStateException(error);
            }
            return value;
        }
        public Task<T> addOnSuccessListener(Consumer<? super T> c) { return addOnSuccessListener(MAIN,c); }
        public Task<T> addOnSuccessListener(Executor e, Consumer<? super T> c) { if (error == null) e.execute(() -> c.accept(value)); return this; }
        public Task<T> addOnFailureListener(Consumer<Exception> c) { return addOnFailureListener(MAIN,c); }
        public Task<T> addOnFailureListener(Executor e, Consumer<Exception> c) { if (error != null) e.execute(() -> c.accept(error)); return this; }
        public Task<T> addOnCompleteListener(Consumer<Task<T>> c) { return addOnCompleteListener(MAIN,c); }
        public Task<T> addOnCompleteListener(Executor e, Consumer<Task<T>> c) { e.execute(() -> c.accept(this)); return this; }
    }
    public static final class ApiException extends Exception {
        public ApiException() { super("Google sign-in unavailable"); }
        public int getStatusCode() { return 17; }
    }
    public static final class Status {
        public String getStatusMessage() { return "Unavailable in Ayuless"; }
        public int getStatusCode() { return 17; }
    }
    public static final class CancellationTokenSource {
        private final CancellationToken token=new CancellationToken();
        public void cancel() { token.canceled=true; }
        public CancellationToken getToken() { return token; }
    }
    public static final class CancellationToken { private boolean canceled; public boolean isCancellationRequested() { return canceled; } }
    public static final class VerificationSupportResult {}
    public static final class GoogleSignIn {
        public static Task<GoogleSignInAccount> getSignedInAccountFromIntent(Intent i) { return Task.failed(); }
        public static GoogleSignInClient getClient(Context c, GoogleSignInOptions o) { return new GoogleSignInClient(); }
    }
    public static final class GoogleSignInAccount { public String getIdToken() { return null; } public String getEmail() { return null; } }
    public static final class GoogleSignInClient {
        public Task<Void> signOut() { return Task.failed(); }
        public Intent getSignInIntent() { throw new UnsupportedOperationException("Google sign-in unavailable"); }
    }
    public static final class GoogleSignInOptions {
        public static final class Builder {
            public Builder requestIdToken(String id) { return this; }
            public Builder requestEmail() { return this; }
            public GoogleSignInOptions build() { return new GoogleSignInOptions(); }
        }
    }
    public static final class SmsRetriever {
        public static final String SMS_RETRIEVED_ACTION="org.ayuless.UNAVAILABLE_SMS_RETRIEVER";
        public static final String EXTRA_SMS_MESSAGE="message";
    }
    public static final class Wearable {
        public static Wearable getMessageClient(Context c) { return new Wearable(); }
        public Task<Integer> sendMessage(String node, String path, byte[] data) { return Task.failed(); }
    }
    public static final class PaymentsClient {
        public Task<Boolean> isReadyToPay(IsReadyToPayRequest r) { return Task.result(false); }
        public Task<PaymentData> loadPaymentData(PaymentDataRequest r) { return Task.failed(); }
    }
    public static final class IsReadyToPayRequest { public static IsReadyToPayRequest fromJson(String json) { return new IsReadyToPayRequest(); } }
    public static final class PaymentDataRequest { public static PaymentDataRequest fromJson(String json) { return new PaymentDataRequest(); } }
    public static final class PaymentData {
        public static PaymentData getFromIntent(Intent i) { return null; }
        public String toJson() { return null; }
    }
    public static final class WalletConstants { public static final int ENVIRONMENT_TEST=3, ENVIRONMENT_PRODUCTION=1, THEME_LIGHT=1; }
    public static final class Wallet {
        public static PaymentsClient getPaymentsClient(Context c, WalletOptions o) { return new PaymentsClient(); }
        public static final class WalletOptions {
            public static final class Builder {
                public Builder setEnvironment(int e) { return this; }
                public Builder setTheme(int t) { return this; }
                public WalletOptions build() { return new WalletOptions(); }
            }
        }
    }
    public static final class AutoResolveHelper {
        public static final int RESULT_ERROR=1;
        public static Status getStatusFromIntent(Intent i) { return new Status(); }
        public static void resolveTask(Task<PaymentData> t, Activity a, int code) { throw new UnsupportedOperationException("Google Pay unavailable"); }
    }
}
