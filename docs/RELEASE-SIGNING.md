# Ayuless release signing

Starting with GitHub release v1.0.0, public release APKs use a dedicated private
Ayuless key instead of Telegram's committed public test key.

- Certificate: CN=Ayuless, O=Ayuless; RSA 4096, SHA256withRSA.
- Certificate SHA-256: `16be2baa6910eac6c4b75aa6a2c2a29b38c1f82204e2ce7d8e55ba00971a628c`.
- Public certificate: [release-signing-certificate.pem](release-signing-certificate.pem).
- Old test certificate SHA-256: `a08d7dc323ddf71ef3201944397e0d3cce7d40847263e11f328b68bbe19229ab`.

The private keystore and password are stored outside the checkout in the owner's
restricted local signing directory. They must never be committed, attached to a
release or included in an APK/source archive. Back up both securely outside the
build machine; without them, compatible future updates cannot be signed.

The repository's existing Gradle test-signing configuration is for test artifacts.
A future public release must build unsigned and be signed with the private
release key, then pass `apksigner verify --verbose --print-certs` against the
fingerprint above. Do not upload a test-signed CI artifact as a public release.
F-Droid's own signing arrangement remains separate.

On the current owner's machine, with an unsigned APK already built:

```sh
SIGNING_DIR="$HOME/.local/share/ayuless-signing"
zipalign -P 16 -f 4 unsigned.apk Ayuless-release.apk
apksigner sign --ks "$SIGNING_DIR/ayuless-release.p12" \
  --ks-key-alias ayuless \
  --ks-pass "file:$SIGNING_DIR/store-password.txt" Ayuless-release.apk
apksigner verify --verbose --print-certs Ayuless-release.apk
```

The PKCS12 key and store use the same password. With apksigner, omit `--key-pass`
so it reuses the store password; passing the same one-line password file twice
attempts a second read after EOF.

## Migrating from test/alpha APKs

The new certificate differs from the public test certificate. The first install
requires uninstalling the old app. Export the local Ayuless database and saved
attachments before uninstalling. Android deletes app data on uninstall; secret
chats cannot be recovered by logging in again. Cloud Telegram chats remain on
the server. Later GitHub releases must keep the new private signing identity.

Release numbering follows the project's existing scheme: GitHub v1.0.0 uses
Telegram base Android versionName 13.2.1 and afat versionCode 72919. The v1.0.0
APK uses the exact previously tested application sources; re-signing and ZIP
alignment do not change the contents of any original APK entry.
