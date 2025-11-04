package br.edu.ufersa.cc.seg.servicediscovery.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.ufersa.cc.seg.common.crypto.CryptoService;
import br.edu.ufersa.cc.seg.common.network.SecureMessaging;
import br.edu.ufersa.cc.seg.common.network.SecureTcpMessaging;
import br.edu.ufersa.cc.seg.servicediscovery.exceptions.CalcException;
import lombok.extern.slf4j.Slf4j;

/**
 * Servidor que oferece operações aritméticas simples (sum, sub, mul, div)
 * e registra-se no DirectoryServer ao iniciar.
 */
@Slf4j
public class CalculatorServer {

    private static final String DIRECTORY_HOST = "localhost";
    private static final int DIRECTORY_PORT = 9100;

    private static final byte[] ENC_KEY = java.util.Base64.getDecoder().decode("DJXkb7GyuXP5Hfep9OLukQ==");
    private static final byte[] HMAC_KEY = java.util.Base64.getDecoder()
            .decode("QYp+xG2d7Ir8Xo2ZyD7m8FJKwrFrxd9ayN9i4mBQlTg=");

    private final CryptoService crypto = new CryptoService(ENC_KEY, HMAC_KEY);
    private final ObjectMapper mapper = new ObjectMapper();

    private final int port;

    public CalculatorServer(final int port) {
        this.port = port;
    }

    public static void main(final String[] args) throws Exception {
        final var scanner = new Scanner(System.in);

        System.out.println("\nNOVA CALCULADORA");

        System.out.print("Porta:\t");
        final var port = scanner.nextInt();
        scanner.close();

        new CalculatorServer(port).start();
    }

    public void start() throws IOException {
        log.info("Inciando calculadora...");

        // Registra no diretório
        try (final var messenger = new SecureTcpMessaging(DIRECTORY_HOST, DIRECTORY_PORT, crypto)) {
            // Cria a requisição
            final var request = mapper.createObjectNode();
            request.put("type", "REGISTER");
            request.put("service", "calculator");
            request.put("address", "localhost:" + port);

            // Envia a requisição
            messenger.sendSecure(mapper.writeValueAsBytes(request));

            // Recebe a resposta
            final var responseInBytes = messenger.receiveSecure();
            final var responseInJson = mapper.readTree(responseInBytes);

            // Imprime resultado do registro
            log.info("Registro do serviço: {}", responseInJson.toString());
        } catch (final Exception e) {
            log.warn("Falha ao registrar no diretório: {}", e.getMessage());
        }

        log.info("Iniciando servidor na porta {}", port);
        try (final var server = new ServerSocket(port)) {
            while (true) {
                final var client = new SecureTcpMessaging(server, crypto);
                new Thread(() -> handleClient(client)).start();
            }
        }
    }

    private void handleClient(final SecureMessaging messenger) {
        // Cria a resposta
        final var responseInJson = mapper.createObjectNode();
        responseInJson.put("type", "RESPONSE");

        try {
            // Recebe a requisição
            final var requestInBytes = messenger.receiveSecure();
            final var requestInJson = mapper.readTree(requestInBytes);
            final var type = requestInJson.get("type").asText();

            if ("CALL".equals(type)) {
                // Obtém os dados do cálculo
                final var operator = requestInJson.get("op").asText();
                final var a = requestInJson.get("a").asDouble();
                final var b = requestInJson.get("b").asDouble();

                // Adiciona o resultado
                final double result = calculate(operator, a, b);
                responseInJson.put("result", result);

                // Envia a resposta
                messenger.sendSecure(mapper.writeValueAsBytes(responseInJson));
            }
        } catch (final CalcException e) {
            responseInJson.put("error", e.getMessage());
            log.warn("Erro no cálculo: {}", e.getMessage());
        } catch (final Exception e) {
            log.warn("Erro no serviço ao processar requisição: {}", e.getMessage());
        }
    }

    private double calculate(final String operator, final double a, final double b) throws CalcException {
        return switch (operator) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> a / b;
            default -> throw new CalcException("Operação desconhecida: " + operator);
        };
    }

}
