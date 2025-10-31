package br.edu.ufersa.cc.seg.dns.client;

import java.net.Socket;
import java.util.Base64;
import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.ufersa.cc.seg.common.crypto.CryptoService;
import br.edu.ufersa.cc.seg.common.network.SecureMessaging;
import br.edu.ufersa.cc.seg.common.network.SecureTcpMessaging;
import lombok.val;

/**
 * Cliente simples que envia UPDATE (registrador)
 */
public class Registrador {

    // Localização do servidor DNS
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9000;

    // Chaves fixas para demo
    private static final byte[] ENC_KEY = Base64.getDecoder().decode("DJXkb7GyuXP5Hfep9OLukQ==");
    private static final byte[] HMAC_KEY = Base64.getDecoder().decode("QYp+xG2d7Ir8Xo2ZyD7m8FJKwrFrxd9ayN9i4mBQlTg=");

    private static CryptoService cryptoService = new CryptoService(ENC_KEY, HMAC_KEY);
    private static ObjectMapper mapper = new ObjectMapper();

    public static void main(final String[] args) throws Exception {
        val scanner = new Scanner(System.in);

        // Receber dados do usuário
        System.out.println("REGISTRAR NOVO DOMÍNIO");
        System.out.print("Nome:\t");
        val name = scanner.nextLine();
        System.out.print("IP:\t");
        val ip = scanner.nextLine();

        // Abrir conexão
        try (val socket = new Socket(SERVER_HOST, SERVER_PORT);
                final SecureMessaging comm = new SecureTcpMessaging(socket, cryptoService)) {

            // Construir mensagem UPDATE
            val request = mapper.createObjectNode();
            request.put("type", "UPDATE");
            request.put("name", name);
            request.put("ip", ip);

            // Enviar
            comm.sendSecure(mapper.writeValueAsBytes(request));

            // Receber e imprimir resposta
            val resp = comm.receiveSecure();
            System.out.println("Resposta: " + new String(resp));
        }

        // Fechar entrada
        scanner.close();
    }

}
