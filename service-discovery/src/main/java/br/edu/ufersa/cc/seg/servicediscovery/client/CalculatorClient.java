package br.edu.ufersa.cc.seg.servicediscovery.client;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Scanner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.ufersa.cc.seg.common.crypto.CryptoService;
import br.edu.ufersa.cc.seg.common.network.SecureTcpMessaging;
import lombok.extern.slf4j.Slf4j;

/**
 * Cliente que consulta o DirectoryServer para descobrir serviços e chama um
 * serviço escolhido (round-robin ou random).
 */
@Slf4j
public class CalculatorClient {

    private static final String DIRECTORY_HOST = "localhost";
    private static final int DIRECTORY_PORT = 9100;

    private static final byte[] ENC_KEY = java.util.Base64.getDecoder()
            .decode("DJXkb7GyuXP5Hfep9OLukQ==");
    private static final byte[] HMAC_KEY = java.util.Base64.getDecoder()
            .decode("QYp+xG2d7Ir8Xo2ZyD7m8FJKwrFrxd9ayN9i4mBQlTg=");

    private final CryptoService crypto = new CryptoService(ENC_KEY, HMAC_KEY);
    private final ObjectMapper mapper = new ObjectMapper();

    public static void main(final String[] args) {
        final var scanner = new Scanner(System.in);

        var isRunning = true;
        while (isRunning) {
            isRunning = interact(scanner);
        }

        scanner.close();
    }

    private void run(final String op, final double a, final double b) {
        try (final var messenger = new SecureTcpMessaging(DIRECTORY_HOST, DIRECTORY_PORT, crypto)) {
            // Cria requisição
            final var request = mapper.createObjectNode();
            request.put("type", "DISCOVER");
            request.put("service", "calculator");

            // Envia requisição
            messenger.sendSecure(mapper.writeValueAsBytes(request));

            // Recebe resposta
            final var responseInBytes = messenger.receiveSecure();
            final var response = mapper.readTree(responseInBytes);
            final var address = response.get("address");

            // Interrompe se não houver endereços associados
            if (address.isNull()) {
                System.out.println("Nenhum serviço disponível");
                return;
            }

            // 3) Call service
            callService(address.asText(), op, a, b);
        } catch (final UnknownHostException e) {
            log.error("Host '{}' desconhecido", DIRECTORY_HOST, e);
        } catch (final JsonProcessingException e) {
            log.error("Falha ao serializar/desserializar", e);
        } catch (final IOException e) {
            log.error("Erro de I/O na comunicação com o serviço", e);
        }
    }

    private void callService(final String address, final String operator, final double a, final double b) {
        final var parts = address.split(":");
        final var host = parts[0];
        final var port = Integer.parseInt(parts[1]);

        try (final var messenger = new SecureTcpMessaging(host, port, crypto)) {
            final var request = mapper.createObjectNode();
            request.put("type", "CALL");
            request.put("op", operator);
            request.put("a", a);
            request.put("b", b);

            messenger.sendSecure(mapper.writeValueAsBytes(request));
            final var respBytes = messenger.receiveSecure();
            final JsonNode resp = mapper.readTree(respBytes);

            if (resp.has("result")) {
                System.out.println("Resultado: " + resp.get("result").asDouble());
            } else {
                System.out.println("Erro do serviço: " + resp.get("error").asText());
            }
        } catch (final UnknownHostException e) {
            log.error("Host '{}' desconhecido", host, e);
        } catch (final JsonProcessingException e) {
            log.error("Falha ao serializar/desserializar", e);
        } catch (final IOException e) {
            log.error("Erro de I/O na comunicação com o serviço", e);
        }
    }

    private static boolean interact(final Scanner scanner) {
        // Obter expressao numérica
        System.out.println("\nNOVO CÁLCULO");
        System.out.print("Expressão simples: ");
        final var expression = scanner.nextLine().trim();

        if ("x".equalsIgnoreCase(expression)) {
            return false;
        }

        // Separar partes do cálculo
        final var parts = expression.splitWithDelimiters("[\\+\\-\\*\\/]", 99);
        final var a = Double.parseDouble(parts[0]);
        final var operator = parts[1];
        final var b = Double.parseDouble(parts[2]);

        new CalculatorClient().run(operator, a, b);
        return true;
    }

}
