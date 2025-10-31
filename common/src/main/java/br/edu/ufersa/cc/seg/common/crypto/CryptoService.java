package br.edu.ufersa.cc.seg.common.crypto;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.security.SecureRandom;

import lombok.SneakyThrows;
import lombok.val;
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

    public CryptoService(final byte[] encryptionKey, final byte[] hmacKey) {
        this.encryptionKey = new SecretKeySpec(encryptionKey, "AES");
        this.hmacKey = new SecretKeySpec(hmacKey, HMAC_ALGORITHM);
        this.secureRandom = new SecureRandom();
    }

    /**
     * Cifra uma mensagem e gera o HMAC para garantir confidencialidade e
     * integridade/autenticidade
     */
    public SecurityMessage encrypt(final byte[] message) {
        log.debug("Criptografando mensagem...\n{}", new String(message));

        try {
            // Gerar IV aleatório
            val iv = new byte[IV_SIZE];
            secureRandom.nextBytes(iv);
            val ivSpec = new IvParameterSpec(iv);

            // Cifrar a mensagem
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, ivSpec);
            val encrypted = cipher.doFinal(message);

            // Gera HMAC (encrypted + iv + timestamp para evitar replay)
            val timestamp = System.currentTimeMillis();
            val hmac = generateHmac(encrypted, iv, timestamp);

            // Retorna mensagem segura
            val securityMessage = SecurityMessage.builder()
                    .encryptedContent(encrypted)
                    .hmac(hmac)
                    .iv(iv)
                    .timestamp(timestamp)
                    .build();

            val writer = new ObjectMapper().writerWithDefaultPrettyPrinter();
            log.debug("Mensagem criptografada:\n{}", writer.writeValueAsString(securityMessage));

            return securityMessage;
        } catch (final Exception e) {
            log.error("Erro ao cifrar mensagem", e);
            throw new CryptoException("Erro de criptografia", e);
        }
    }

    /**
     * Decifra uma mensagem e valida seu HMAC para garantir
     * integridade/autenticidade
     */
    @SneakyThrows
    public byte[] decrypt(final SecurityMessage secureMsg) {
        val writer = new ObjectMapper().writerWithDefaultPrettyPrinter();
        log.debug("Descriptografando mensagem...\n{}", writer.writeValueAsString(secureMsg));

        try {
            // Valida HMAC primeiro
            val expectedHmac = generateHmac(
                    secureMsg.getEncryptedContent(),
                    secureMsg.getIv(),
                    secureMsg.getTimestamp());

            if (!MessageDigest.isEqual(expectedHmac, secureMsg.getHmac())) {
                throw new CryptoException("HMAC inválido - mensagem pode ter sido adulterada");
            }

            // Se HMAC ok, decifra
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey,
                    new IvParameterSpec(secureMsg.getIv()));

            val original = cipher.doFinal(secureMsg.getEncryptedContent());
            log.debug("Mensagem descriptografada:\n{}", new String(original));

            return original;
        } catch (final CryptoException e) {
            throw e;
        } catch (final Exception e) {
            log.error("Erro ao decifrar mensagem", e);
            throw new CryptoException("Erro de criptografia", e);
        }
    }

    /**
     * Gera HMAC para os componentes da mensagem
     */
    private byte[] generateHmac(final byte[] encrypted, final byte[] iv, final long timestamp) {
        try {
            val mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(hmacKey);

            // HMAC(encrypted + iv + timestamp)
            mac.update(encrypted);
            mac.update(iv);
            mac.update(longToBytes(timestamp));

            return mac.doFinal();

        } catch (final Exception e) {
            log.error("Erro ao gerar HMAC", e);
            throw new CryptoException("Erro ao gerar HMAC", e);
        }
    }

    /**
     * Converte long para array de bytes
     */
    private static byte[] longToBytes(long x) {
        val result = new byte[8];
        for (var i = 7; i >= 0; i--) {
            result[i] = (byte) (x & 0xFF);
            x >>= 8;
        }
        return result;
    }

}
