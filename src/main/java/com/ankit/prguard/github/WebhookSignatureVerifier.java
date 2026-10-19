package com.ankit.prguard.github;

import com.ankit.prguard.shared.PrGuardProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class WebhookSignatureVerifier {

    private static final String PREFIX = "sha256=";
    private final String secret;
    private final boolean signatureRequired;

    public WebhookSignatureVerifier(PrGuardProperties properties) {
        this.secret = properties.github().webhookSecret();
        this.signatureRequired = properties.github().requireSignature();
    }

    public boolean isValid(byte[] payload, String signatureHeader) {
        if (!signatureRequired) {
            return true;
        }
        if (secret == null || secret.isBlank() || signatureHeader == null || !signatureHeader.startsWith(PREFIX)) {
            return false;
        }

        byte[] expected = hmacSha256(payload, secret);
        byte[] actual;
        try {
            actual = HexFormat.of().parseHex(signatureHeader.substring(PREFIX.length()));
        } catch (IllegalArgumentException exception) {
            return false;
        }
        return MessageDigest.isEqual(expected, actual);
    }

    private byte[] hmacSha256(byte[] payload, String secretValue) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretValue.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(payload);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("HmacSHA256 is unavailable", exception);
        } catch (java.security.InvalidKeyException exception) {
            throw new IllegalStateException("Invalid webhook secret", exception);
        }
    }
}
