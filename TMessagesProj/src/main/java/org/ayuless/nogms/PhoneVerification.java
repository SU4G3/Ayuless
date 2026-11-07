/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.nogms;

import android.content.Context;
import java.util.Collections;
import java.util.List;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;

/** Firebase PNV is absent; login continues through the existing passkey/manual flow. */
public final class PhoneVerification {
    private PhoneVerification() {}
    public static final class SimInfo {
        public int slot=-1;
        public String carrierName, countryName, mccMnc, phoneNumber;
    }
    public static SimInfo getSimInfo(Context c, Google.VerificationSupportResult r) { return new SimInfo(); }
    public static void getVerificationSupportInfo(Utilities.Callback<List<Google.VerificationSupportResult>> c) { c.run(Collections.emptyList()); }
    public static Google.Task<TLRPC.auth_Authorization> loginViaFirebasePhoneNumberVerification(Context c, int account, Google.CancellationToken token, Runnable verifying) { return Google.Task.failed(); }
}
