package br.edu.ufersa.cc.seg.dns.client;

import java.io.EOFException;
import java.util.Base64;
import java.util.Scanner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.ufersa.cc.seg.common.crypto.CryptoService;
import br.edu.ufersa.cc.seg.common.network.SecureMessaging;
import br.edu.ufersa.cc.seg.common.network.SecureTcpMessaging;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Cliente simples que envia QUERY (requisitante)
 */
@Slf4j
public class RequestingClient {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9000;

    // Chaves de demonstração
    private static final byte[] ENC_KEY = Base64.getDecoder().decode("DJXkb7GyuXP5Hfep9OLukQ==");
    private static final byte[] HMAC_KEY = Base64.getDecoder().decode("QYp+xG2d7Ir8Xo2ZyD7m8FJKwrFrxd9ayN9i4mBQlTg=");

    private static CryptoService cryptoService = new CryptoService(ENC_KEY, HMAC_KEY);
    private static ObjectMapper mapper = new ObjectMapper();

    public static void main(final String[] args) throws Exception {
        final var scanner = new Scanner(System.in);

        final var notificationListener = new Thread(() -> listenNotifications(scanner));

        final var interaction = new Thread(() -> {
            interact(scanner);
            notificationListener.interrupt();
        });

        notificationListener.start();
        Thread.sleep(1000);
        interaction.start();
        interaction.join();

        scanner.close();
    }

    @SneakyThrows
    private static void interact(final Scanner scanner) {
        try {
            String input;
            do {
                System.out.println("\nCONSULTAR DOMÍNIO");
                System.out.println("Digite 'x' para sair");
                System.out.print("Nome:\t");

                input = scanner.nextLine().trim();
                if (!"x".equalsIgnoreCase(input)) {
                    sendQuery(input);
                }

            } while (!"x".equalsIgnoreCase(input) && !Thread.currentThread().isInterrupted());
        } catch (final IllegalStateException e) {
            System.out.println();
            log.info("Encerrando...");
        } catch (final Exception e) {
            System.out.println();
            log.error("Erro ao processar consulta", e);
        }
    }

    @SneakyThrows
    private static void sendQuery(final String name) {
        try (final var messenger = new SecureTcpMessaging(SERVER_HOST, SERVER_PORT, cryptoService)) {

            // Envia requisição
            final var request = mapper.createObjectNode();
            request.put("type", "QUERY");
            request.put("name", name);
            messenger.sendSecure(mapper.writeValueAsBytes(request));

            // Processa resposta
            final var responseInBytes = messenger.receiveSecure();
            final var responseInJson = mapper.readTree(responseInBytes);
            printResponse(responseInJson);
        }
    }

    private static void printResponse(final JsonNode resp) {
        final var type = resp.get("type").asText();
        final var name = resp.get("name").asText();

        if ("RESPONSE".equals(type)) {
            if (resp.has("ip")) {
                final var ip = resp.get("ip").asText();
                System.out.printf("%s → %s%n", name, ip);
            } else {
                System.out.printf("%s → NÃO ENCONTRADO%n", name);
            }
        } else {
            System.out.printf("Erro: %s%n", resp.get("message"));
        }
    }

    @SneakyThrows
    private static void listenNotifications(final Scanner scanner) {
        while (!Thread.currentThread().isInterrupted()) {
            try (final var messenger = new SecureTcpMessaging(SERVER_HOST, SERVER_PORT, cryptoService)) {
                registerForNotifications(messenger);
                receiveNotifications(messenger);
            } catch (final EOFException e) {
                log.error("Credenciais desconhecidas");
                Thread.currentThread().interrupt();
                scanner.close();
            } catch (final Exception e) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                log.error("Erro no listener de notificações", e);
                Thread.sleep(1000); // Espera antes de reconectar
            }
        }
    }

    @SneakyThrows
    private static void registerForNotifications(final SecureMessaging messenger) {
        final var request = mapper.createObjectNode();
        request.put("type", "REGISTER_NOTIFY");
        messenger.sendSecure(mapper.writeValueAsBytes(request));

        final var responseInBytes = messenger.receiveSecure();
        final var responseInJson = mapper.readTree(responseInBytes);
        if (!"REGISTERED".equals(responseInJson.get("type").asText())) {
            throw new NotificationRegistrationException("Registro para notificação falhou: " + responseInJson);
        }

        log.info("Registrado para receber notificações");
    }

    @SneakyThrows
    private static void receiveNotifications(final SecureMessaging messenger) {
        while (!Thread.currentThread().isInterrupted()) {
            final var notificationInBytes = messenger.receiveSecure();
            final var notificationInJson = mapper.readTree(notificationInBytes);

            if ("NOTIFY".equals(notificationInJson.get("type").asText())) {
                final var name = notificationInJson.get("name").asText();
                final var ip = notificationInJson.get("ip").asText();
                System.out.printf("%n[ATUALIZAÇÃO] %s → %s%n", name, ip);
            }
        }
    }

}
