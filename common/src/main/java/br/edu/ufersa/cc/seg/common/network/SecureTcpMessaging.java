package br.edu.ufersa.cc.seg.common.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import br.edu.ufersa.cc.seg.common.crypto.CryptoService;
import br.edu.ufersa.cc.seg.common.crypto.SecurityMessage;
import lombok.RequiredArgsConstructor;
import lombok.val;

/**
 * Implementação TCP da interface SecureMessaging
 */
@RequiredArgsConstructor
public class SecureTcpMessaging implements SecureMessaging {

    private final Socket socket;
    private final CryptoService cryptoService;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    public SecureTcpMessaging(final Socket socket, final CryptoService cryptoService) throws IOException {
        this.socket = socket;
        this.cryptoService = cryptoService;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    public SecureTcpMessaging(final String host, final int port, final CryptoService cryptoService) throws IOException {
        this(new Socket(host, port), cryptoService);
    }

    public SecureTcpMessaging(final ServerSocket serverSocket, final CryptoService cryptoService) throws IOException {
        this(serverSocket.accept(), cryptoService);
    }

    @Override
    public void sendSecure(final byte[] message) throws IOException {
        final var secureMsg = cryptoService.encrypt(message);
        out.writeObject(secureMsg);
        out.flush();
    }

    @Override
    public byte[] receiveSecure() throws IOException {
        try {
            val secureMsg = (SecurityMessage) in.readObject();
            return cryptoService.decrypt(secureMsg);
        } catch (final ClassNotFoundException e) {
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
