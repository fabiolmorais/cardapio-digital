package br.com.cardapio.support;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

/**
 * Um par de chaves P-256 gerado na hora, no formato do web push (base64url). Serve para as chaves
 * VAPID dos testes e para o "navegador" dos testes: nenhuma chave fica no repositório.
 *
 * @param publicKey  o ponto sem compressão (0x04 + X + Y)
 * @param privateKey o número d, com 32 bytes
 */
public record TestVapidKeys(String publicKey, String privateKey, KeyPair pair) {

    public static TestVapidKeys generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair pair = generator.generateKeyPair();
            ECPublicKey pub = (ECPublicKey) pair.getPublic();
            byte[] point = new byte[65];
            point[0] = 0x04;
            copy32(pub.getW().getAffineX(), point, 1);
            copy32(pub.getW().getAffineY(), point, 33);
            byte[] d = new byte[32];
            copy32(((ECPrivateKey) pair.getPrivate()).getS(), d, 0);
            Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
            return new TestVapidKeys(b64.encodeToString(point), b64.encodeToString(d), pair);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void copy32(BigInteger value, byte[] out, int offset) {
        byte[] raw = value.toByteArray();
        int start = Math.max(0, raw.length - 32);
        System.arraycopy(raw, start, out, offset + 32 - (raw.length - start), raw.length - start);
    }
}
