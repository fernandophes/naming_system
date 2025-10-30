package br.edu.ufersa.cc.seg.common.crypto;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.security.MessageDigest;
import java.security.SecureRandom;
import lombok.extern.slf4j.Slf4j;

/**
 * Serviço de criptografia que provê funções para confidencialidade (cifra
 * simétrica) e integridade/autenticidade (HMAC) das mensagens.
 */
@Slf4j
public class CryptoService {

    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int IV_SIZE = 16;

    private final SecretKey encryptionKey;
    private final SecretKey hmacKey;
    private final SecureRandom secureRandom;

    public CryptoService(byte[] encryptionKey, byte[] hmacKey) {
        this.encryptionKey = new SecretKeySpec(encryptionKey, "AES");
        this.hmacKey = new SecretKeySpec(hmacKey, HMAC_ALGORITHM);
        this.secureRandom = new SecureRandom();
    }

    /**
     * Cifra uma mensagem e gera o HMAC para garantir confidencialidade e
     * integridade/autenticidade
     */
    public SecurityMessage encrypt(byte[] message) {
        try {
            // Gera IV aleatório
            byte[] iv = new byte[IV_SIZE];
            secureRandom.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // Cifra a mensagem
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, ivSpec);
            byte[] encrypted = cipher.doFinal(message);

            // Gera HMAC (encrypted + iv + timestamp para evitar replay)
            long timestamp = System.currentTimeMillis();
            byte[] hmac = generateHmac(encrypted, iv, timestamp);

            // Retorna mensagem segura
            return SecurityMessage.builder()
                    .encryptedContent(encrypted)
                    .hmac(hmac)
                    .iv(iv)
                    .timestamp(timestamp)
                    .build();

        } catch (Exception e) {
            log.error("Erro ao cifrar mensagem", e);
            throw new RuntimeException("Erro de criptografia", e);
        }
    }

    /**
     * Decifra uma mensagem e valida seu HMAC para garantir
     * integridade/autenticidade
     */
    public byte[] decrypt(SecurityMessage secureMsg) {
        try {
            // Valida HMAC primeiro
            byte[] expectedHmac = generateHmac(
                    secureMsg.getEncryptedContent(),
                    secureMsg.getIv(),
                    secureMsg.getTimestamp());

            if (!MessageDigest.isEqual(expectedHmac, secureMsg.getHmac())) {
                throw new RuntimeException("HMAC inválido - mensagem pode ter sido adulterada");
            }

            // Se HMAC ok, decifra
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey,
                    new IvParameterSpec(secureMsg.getIv()));

            return cipher.doFinal(secureMsg.getEncryptedContent());

        } catch (Exception e) {
            log.error("Erro ao decifrar mensagem", e);
            throw new RuntimeException("Erro de criptografia", e);
        }
    }

    /**
     * Gera HMAC para os componentes da mensagem
     */
    private byte[] generateHmac(byte[] encrypted, byte[] iv, long timestamp) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(hmacKey);

            // HMAC(encrypted + iv + timestamp)
            mac.update(encrypted);
            mac.update(iv);
            mac.update(longToBytes(timestamp));

            return mac.doFinal();

        } catch (Exception e) {
            log.error("Erro ao gerar HMAC", e);
            throw new RuntimeException("Erro ao gerar HMAC", e);
        }
    }

    /**
     * Converte long para array de bytes
     */
    private static byte[] longToBytes(long x) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (x & 0xFF);
            x >>= 8;
        }
        return result;
    }
}