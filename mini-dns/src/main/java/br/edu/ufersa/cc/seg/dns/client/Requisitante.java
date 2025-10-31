package br.edu.ufersa.cc.seg.dns.client;

import java.io.IOException;
import java.net.Socket;
import java.util.Base64;
import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.ufersa.cc.seg.common.crypto.CryptoService;
import br.edu.ufersa.cc.seg.common.network.SecureMessaging;
import br.edu.ufersa.cc.seg.common.network.SecureTcpMessaging;
import lombok.val;

/**
 * Cliente simples que envia QUERY (requisitante)
 */
public class Requisitante {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9000;

    // Chaves de demonstração — devem ser as mesmas do servidor (BeanConfig)
    private static final byte[] ENC_KEY = Base64.getDecoder().decode("DJXkb7GyuXP5Hfep9OLukQ==");
    private static final byte[] HMAC_KEY = Base64.getDecoder().decode("QYp+xG2d7Ir8Xo2ZyD7m8FJKwrFrxd9ayN9i4mBQlTg=");

    private static CryptoService cryptoService = new CryptoService(ENC_KEY, HMAC_KEY);
    private static ObjectMapper mapper = new ObjectMapper();

    public static void main(final String[] args) throws Exception {
        val scanner = new Scanner(System.in);

        // Loop de interação
        var repeat = true;
        while (repeat) {
            repeat = interact(scanner);
        }

        scanner.close();
    }

    private static boolean interact(final Scanner scanner) throws IOException {
        System.out.println("CONSULTAR DOMÍNIO (Digite 'x' para sair)");
        System.out.print("Nome:\t");
        val name = scanner.nextLine();

        // Escape: sair quando usuário digitar "x"
        if (name.equalsIgnoreCase("x")) {
            return false;
        }

        try (val socket = new Socket(SERVER_HOST, SERVER_PORT);
                final SecureMessaging comm = new SecureTcpMessaging(socket, cryptoService)) {

            val request = mapper.createObjectNode();
            request.put("type", "QUERY");
            request.put("name", name);

            comm.sendSecure(mapper.writeValueAsBytes(request));

            val resp = comm.receiveSecure();
            System.out.println("Resposta: " + new String(resp));
        }

        System.out.println();
        return true;
    }

}
