package com.diui.launcher.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * 管理 ADB 认证所需的 RSA 密钥对
 */
public class AdbKeyManager {

    private static final String TAG = "AdbKeyManager";
    private static final String PREFS_NAME = "adb_keys";
    private static final String KEY_PRIVATE = "private_key";
    private static final String KEY_PUBLIC = "public_key";

    private PrivateKey privateKey;
    private RSAPublicKey publicKey;

    public AdbKeyManager(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String privStr = prefs.getString(KEY_PRIVATE, null);
        String pubStr = prefs.getString(KEY_PUBLIC, null);

        if (privStr != null && pubStr != null) {
            try {
                byte[] privBytes = Base64.decode(privStr, Base64.DEFAULT);
                PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privBytes);
                privateKey = KeyFactory.getInstance("RSA").generatePrivate(privSpec);

                byte[] pubBytes = Base64.decode(pubStr, Base64.DEFAULT);
                X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubBytes);
                publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(pubSpec);
                return;
            } catch (Exception e) {
                Log.w(TAG, "Failed to load keys, regenerating", e);
            }
        }

        generateAndSave(prefs);
    }

    private void generateAndSave(SharedPreferences prefs) {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair pair = gen.generateKeyPair();
            privateKey = pair.getPrivate();
            publicKey = (RSAPublicKey) pair.getPublic();

            prefs.edit()
                    .putString(KEY_PRIVATE, Base64.encodeToString(privateKey.getEncoded(), Base64.DEFAULT))
                    .putString(KEY_PUBLIC, Base64.encodeToString(publicKey.getEncoded(), Base64.DEFAULT))
                    .apply();
            Log.i(TAG, "Generated new RSA key pair");
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate keys", e);
        }
    }

    public static void clearKeys(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().remove(KEY_PRIVATE).remove(KEY_PUBLIC).apply();
        Log.i(TAG, "RSA key pair cleared — next auth will trigger ADB dialog");
    }

    public byte[] signToken(byte[] token) throws Exception {
        Signature sig = Signature.getInstance("SHA1withRSA");
        sig.initSign(privateKey);
        sig.update(token);
        return sig.sign();
    }

    public String getAdbPublicKeyString() {
        byte[] encoded = encodeAdbPublicKey(publicKey);
        return Base64.encodeToString(encoded, Base64.NO_WRAP) + " bydlauncher@byd\n";
    }

    /**
     * 将 Java RSAPublicKey 编码为 ADB 的 mincrypt RSAPublicKey 格式：
     * len(4) + n0inv(4) + n[256] + rr[256] + exponent(4) = 524 bytes
     */
    private static byte[] encodeAdbPublicKey(RSAPublicKey key) {
        BigInteger n = key.getModulus();
        BigInteger e = key.getPublicExponent();

        // n → 小端序 256 字节
        byte[] nBytes = n.toByteArray();
        int nSrcOff = (nBytes.length > 256 && nBytes[0] == 0) ? 1 : 0;
        int nLen = nBytes.length - nSrcOff;
        byte[] nLE = new byte[256];
        for (int i = 0; i < nLen && i < 256; i++) {
            nLE[i] = nBytes[nBytes.length - 1 - i];
        }

        // n0inv = -(n^-1) mod 2^32
        BigInteger n0 = n.and(BigInteger.valueOf(0xFFFFFFFFL));
        BigInteger n0inv = BigInteger.valueOf(0x100000000L).subtract(
                n0.modInverse(BigInteger.valueOf(0x100000000L)));
        long n0invVal = n0inv.longValue() & 0xFFFFFFFFL;

        // rr = 2^4096 mod n（Montgomery R² 参数，R = 2^2048）
        BigInteger rr = BigInteger.valueOf(2).modPow(BigInteger.valueOf(4096), n);
        byte[] rrBytes = rr.toByteArray();
        int rrSrcOff = (rrBytes.length > 256 && rrBytes[0] == 0) ? 1 : 0;
        int rrLen = rrBytes.length - rrSrcOff;
        byte[] rrLE = new byte[256];
        for (int i = 0; i < rrLen && i < 256; i++) {
            rrLE[i] = rrBytes[rrBytes.length - 1 - i];
        }

        byte[] result = new byte[4 + 4 + 256 + 256 + 4]; // 524 bytes
        putLE32(result, 0, 64);              // len = 64 个 uint32
        putLE32(result, 4, (int) n0invVal); // n0inv
        System.arraycopy(nLE, 0, result, 8, 256);    // modulus n
        System.arraycopy(rrLE, 0, result, 264, 256); // R² mod n
        putLE32(result, 520, e.intValue()); // exponent (65537)

        return result;
    }

    private static void putLE32(byte[] buf, int offset, int value) {
        buf[offset] = (byte) (value & 0xFF);
        buf[offset + 1] = (byte) ((value >> 8) & 0xFF);
        buf[offset + 2] = (byte) ((value >> 16) & 0xFF);
        buf[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }
}
