package br.edu.ufersa.cc.seg.common.network;

import br.edu.ufersa.cc.seg.common.crypto.CryptoService;
import br.edu.ufersa.cc.seg.common.crypto.SecurityMessage;
import lombok.RequiredArgsConstructor;
import java.io.*;
import java.net.Socket;

/**
 * Implementação TCP da interface SecureMessaging
 */
@RequiredArgsConstructor
public class SecureTcpMessaging implements SecureMessaging {

    private final Socket socket;
    private final CryptoService cryptoService;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    public SecureTcpMessaging(Socket socket, CryptoService cryptoService) throws IOException {
        this.socket = socket;
        this.cryptoService = cryptoService;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void sendSecure(String destination, byte[] message) throws IOException {
        SecurityMessage secureMsg = cryptoService.encrypt(message);
        out.writeObject(secureMsg);
        out.flush();
    }

    @Override
    public byte[] receiveSecure() throws IOException {
        try {
            SecurityMessage secureMsg = (SecurityMessage) in.readObject();
            return cryptoService.decrypt(secureMsg);
        } catch (ClassNotFoundException e) {
            throw new IOException("Erro ao deserializar mensagem", e);
        }
    }

    @Override
    public void close() throws IOException {
        in.close();
        out.close();
        socket.close();
    }

}
