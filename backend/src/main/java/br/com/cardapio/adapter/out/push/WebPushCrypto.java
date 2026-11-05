package br.com.cardapio.adapter.out.push;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * A criptografia do web push, só com o Java (sem biblioteca): as chaves P-256, a cifra da
 * mensagem (RFC 8291, "aes128gcm") e a assinatura ES256 do VAPID (RFC 8292).
 */
final class WebPushCrypto {

    private static final SecureRandom RANDOM = new SecureRandom();
    /** O tamanho do registro: a mensagem cabe num só (as nossas têm menos de 200 bytes). */
    private static final int RECORD_SIZE = 4096;

    private WebPushCrypto() {
    }

    static ECParameterSpec p256() throws GeneralSecurityException {
        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
        parameters.init(new ECGenParameterSpec("secp256r1"));
        return parameters.getParameterSpec(ECParameterSpec.class);
    }

    /** Um ponto P-256 sem compressão (0x04 + X + Y, 65 bytes) vira chave pública. */
    static ECPublicKey publicKey(byte[] point) throws GeneralSecurityException {
        if (point.length != 65 || point[0] != 0x04) {
            throw new GeneralSecurityException("chave pública P-256 inválida");
        }
        ECPoint w = new ECPoint(new BigInteger(1, Arrays.copyOfRange(point, 1, 33)),
                new BigInteger(1, Arrays.copyOfRange(point, 33, 65)));
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(w, p256()));
    }

    static PrivateKey privateKey(byte[] d) throws GeneralSecurityException {
        return KeyFactory.getInstance("EC").generatePrivate(new ECPrivateKeySpec(new BigInteger(1, d), p256()));
    }

    /** A chave pública como ponto sem compressão (o formato do web push). */
    static byte[] point(ECPublicKey key) {
        byte[] out = new byte[65];
        out[0] = 0x04;
        copy32(key.getW().getAffineX(), out, 1);
        copy32(key.getW().getAffineY(), out, 33);
        return out;
    }

    private static void copy32(BigInteger value, byte[] out, int offset) {
        byte[] raw = value.toByteArray();
        int start = Math.max(0, raw.length - 32);
        int length = raw.length - start;
        System.arraycopy(raw, start, out, offset + 32 - length, length);
    }

    static KeyPair newKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"), RANDOM);
        return generator.generateKeyPair();
    }

    static byte[] hmac(byte[] key, byte[]... parts) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        for (byte[] part : parts) {
            mac.update(part);
        }
        return mac.doFinal();
    }

    /**
     * Cifra a mensagem para um navegador (RFC 8291). O corpo é o cabeçalho "aes128gcm" (sal,
     * tamanho do registro, nossa chave pública de uso único) seguido do texto cifrado.
     *
     * @param uaPublic   a chave pública do navegador (p256dh)
     * @param authSecret o segredo do navegador (auth)
     */
    static byte[] encrypt(byte[] plaintext, byte[] uaPublic, byte[] authSecret) throws GeneralSecurityException {
        KeyPair ephemeral = newKeyPair();
        byte[] asPublic = point((ECPublicKey) ephemeral.getPublic());
        KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
        agreement.init(ephemeral.getPrivate());
        agreement.doPhase(publicKey(uaPublic), true);
        byte[] ecdhSecret = agreement.generateSecret();

        // HKDF com o segredo do navegador, depois com o sal (as duas etapas da seção 3.4 do RFC 8291).
        byte[] prkKey = hmac(authSecret, ecdhSecret);
        byte[] keyInfo = concat("WebPush: info".getBytes(StandardCharsets.US_ASCII), new byte[] {0}, uaPublic,
                asPublic);
        byte[] ikm = hmac(prkKey, keyInfo, new byte[] {1});
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] prk = hmac(salt, ikm);
        byte[] cek = Arrays.copyOf(hmac(prk, "Content-Encoding: aes128gcm".getBytes(StandardCharsets.US_ASCII),
                new byte[] {0, 1}), 16);
        byte[] nonce = Arrays.copyOf(hmac(prk, "Content-Encoding: nonce".getBytes(StandardCharsets.US_ASCII),
                new byte[] {0, 1}), 12);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        // 0x02: o delimitador do último (e único) registro, sem preenchimento.
        byte[] ciphertext = cipher.doFinal(concat(plaintext, new byte[] {2}));

        return concat(salt, ByteBuffer.allocate(4).putInt(RECORD_SIZE).array(), new byte[] {(byte) asPublic.length},
                asPublic, ciphertext);
    }

    /**
     * A assinatura ES256 no formato do JWT: R e S com 32 bytes cada, um depois do outro. O
     * "inP1363Format" já devolve assim (o SHA256withECDSA comum devolve em DER, que o JWT não aceita).
     */
    static byte[] es256(PrivateKey key, byte[] data) throws GeneralSecurityException {
        Signature signature = Signature.getInstance("SHA256withECDSAinP1363Format");
        signature.initSign(key);
        signature.update(data);
        return signature.sign();
    }

    static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.writeBytes(part);
        }
        return out.toByteArray();
    }
}
