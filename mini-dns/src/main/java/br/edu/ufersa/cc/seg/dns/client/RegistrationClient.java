package br.edu.ufersa.cc.seg.dns.client;

import java.io.IOException;
import java.util.Base64;
import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.ufersa.cc.seg.common.crypto.CryptoService;
import br.edu.ufersa.cc.seg.common.network.SecureTcpMessaging;

/**
 * Cliente simples que envia UPDATE (registrador)
 */
public class RegistrationClient {

    // Localização do servidor DNS
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9000;

    // Chaves fixas para demo
    private static final byte[] ENC_KEY = Base64.getDecoder().decode("DJXkb7GyuXP5Hfep9OLukQ==");
    private static final byte[] HMAC_KEY = Base64.getDecoder().decode("QYp+xG2d7Ir8Xo2ZyD7m8FJKwrFrxd9ayN9i4mBQlTg=");

    private static CryptoService cryptoService = new CryptoService(ENC_KEY, HMAC_KEY);
    private static ObjectMapper mapper = new ObjectMapper();

    public static void main(final String[] args) throws Exception {
        final var scanner = new Scanner(System.in);

        // Loop de interação
        var repeat = true;
        while (repeat) {
            repeat = interact(scanner);
        }

        // Fechar entrada
        scanner.close();
    }

    private static boolean interact(final Scanner scanner) throws IOException {
        // Receber dados do usuário
        System.out.println("REGISTRAR NOVO DOMÍNIO (Digite 'x' para sair)");
        System.out.print("Nome:\t");
        final var name = scanner.nextLine();

        // Escape: sair quando usuário digitar "x"
        if (name.equalsIgnoreCase("x")) {
            return false;
        }

        System.out.print("IP:\t");
        final var ip = scanner.nextLine();

        // Abrir conexão
        try (final var messenger = new SecureTcpMessaging(SERVER_HOST, SERVER_PORT, cryptoService)) {
            // Construir mensagem UPDATE
            final var request = mapper.createObjectNode();
            request.put("type", "UPDATE");
            request.put("name", name);
            request.put("ip", ip);

            // Enviar
            messenger.sendSecure(mapper.writeValueAsBytes(request));

            // Receber e imprimir resposta
            final var responseInBytes = messenger.receiveSecure();
            System.out.println("Resposta: " + new String(responseInBytes));
        }

        System.out.println();
        return true;
    }

}
