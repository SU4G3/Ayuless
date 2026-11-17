// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Ayuless contributors
//
// Checks the Web Push decryptor bundled in the UnifiedPush connector against the
// RFC 8291 section 5 example. Run (JDK 17+), with the connector classes.jar
// (from connector-3.3.5.aar), tink-1.23.0.jar, protobuf-java and
// error_prone_annotations on the classpath:
//   javac -cp "$CP" -d out Rfc8291Check.java && java -cp "out:$CP" Rfc8291Check
import com.google.crypto.tink.apps.fixed_webpush.WebPushHybridDecrypt;
import java.util.Base64;

/** Decrypts the RFC 8291 section 5 example with the decryptor bundled in the UnifiedPush connector. */
public class Rfc8291Check {
    static byte[] b(String s) { return Base64.getUrlDecoder().decode(s); }
    public static void main(String[] a) throws Exception {
        byte[] priv = b("q1dXpw3UpT5VOmu_cf_v6ih07Aems3njxI-JWgLcM94");
        byte[] pub = b("BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4");
        byte[] auth = b("BTBZMqHH6r4Tts7J_aSIgg");
        byte[] body = b("DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPTpK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPXLWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN");
        WebPushHybridDecrypt d = new WebPushHybridDecrypt.Builder()
                .withAuthSecret(auth).withRecipientPublicKey(pub).withRecipientPrivateKey(priv).build();
        String out = new String(d.decrypt(body, null), "UTF-8");
        String want = "When I grow up, I want to be a watermelon";
        System.out.println(out);
        if (!out.equals(want)) throw new AssertionError("mismatch");
        System.out.println("RFC 8291 vector OK");
    }
}
