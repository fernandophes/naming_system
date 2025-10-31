package br.edu.ufersa.cc.seg.common.network;

import java.io.Closeable;
import java.io.IOException;

/**
 * Interface comum para comunicação segura entre processos
 */
public interface SecureMessaging extends Closeable {

    /**
     * Envia uma mensagem de forma segura (cifrada e com HMAC)
     */
    void sendSecure(byte[] message) throws IOException;

    /**
     * Recebe uma mensagem de forma segura (decifra e valida HMAC)
     */
    byte[] receiveSecure() throws IOException;

}
