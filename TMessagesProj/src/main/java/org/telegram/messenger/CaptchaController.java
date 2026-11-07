package org.telegram.messenger;

// Ayuless: reCAPTCHA no longer needs an Activity.

// Ayuless: no proprietary reCAPTCHA SDK.

import org.telegram.tgnet.ConnectionsManager;

public class CaptchaController {

    private static class Request {
        private final int currentAccount;
        private final int requestToken;
        private final int generation;
        private boolean completed;

        private Request(int currentAccount, int requestToken, int generation) {
            this.currentAccount = currentAccount;
            this.requestToken = requestToken;
            this.generation = generation;
        }

        private void done(String token) {
            if (completed) {
                return;
            }
            completed = true;
            ConnectionsManager.native_receivedCaptchaResult(currentAccount, requestToken, generation, token);
        }
    }

    public static void request(int currentAccount, int requestToken, int generation, String action, String key_id) {
        // Ayuless: proprietary integration is unavailable.
        new Request(currentAccount, requestToken, generation).done("RECAPTCHA_FAILED_UNAVAILABLE");
    }

    // Ayuless: proprietary action construction removed.
}
