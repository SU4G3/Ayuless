package org.ayuless.login;

import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.OutputSerializedData;
import org.telegram.tgnet.TLRPC;

/**
 * Ayuless: login client spoof, modeled after AyuGram's "Compatibility mode".
 *
 * While an account is not authorized, the network layer presents itself as the official
 * Telegram web build 10.10.1 (45839) on API layer 177. After a successful sign-in the real
 * client version is restored without dropping the auth key.
 *
 * Requests that are sent during authorization must match the layer-177 schema, so this class
 * also provides layer-177 variants of {@code codeSettings} and {@code auth.resendCode}.
 */
public final class LoginSpoof {

    public static final int LAYER = 177;
    public static final int VERSION = 45839;
    public static final String APP_VERSION = "10.10.1 (45839)";

    /** Sent on every connection, spoofed or not (same as AyuGram). */
    public static final String PACKAGE_ID = "org.telegram.messenger.web";
    public static final String INSTALLER = "";

    private static final boolean[] active = new boolean[UserConfig.MAX_ACCOUNT_COUNT];

    private LoginSpoof() {
    }

    public static boolean isActive(int account) {
        synchronized (active) {
            return account >= 0 && account < active.length && active[account];
        }
    }

    /** Only tracks state; the native client version is switched by ConnectionsManager. */
    public static void setActive(int account, boolean value) {
        synchronized (active) {
            if (account >= 0 && account < active.length) {
                active[account] = value;
            }
        }
    }

    public static TLRPC.TL_codeSettings newCodeSettings(int account) {
        return isActive(account) ? new CodeSettings177() : new TLRPC.TL_codeSettings();
    }

    public static TLRPC.TL_auth_resendCode newResendCode(int account) {
        return isActive(account) ? new ResendCode177() : new TLRPC.TL_auth_resendCode();
    }

    /**
     * codeSettings#ad253d78 in layer 177: same constructor, but flag 9 (unknown_number)
     * does not exist yet.
     */
    public static final class CodeSettings177 extends TLRPC.TL_codeSettings {
        @Override
        public void serializeToStream(OutputSerializedData stream) {
            unknown_number = false;
            flags &= ~FLAG_9;
            super.serializeToStream(stream);
        }
    }

    /**
     * auth.resendCode#3ef1a9bf phone_number:string phone_code_hash:string = auth.SentCode
     * (layer 177, no flags and no reason field).
     */
    public static final class ResendCode177 extends TLRPC.TL_auth_resendCode {
        public static final int constructor = 0x3ef1a9bf;

        @Override
        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(phone_number);
            stream.writeString(phone_code_hash);
        }
    }
}
