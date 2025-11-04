package br.edu.ufersa.cc.seg.dns.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.ufersa.cc.seg.common.crypto.CryptoException;
import br.edu.ufersa.cc.seg.common.crypto.CryptoService;
import br.edu.ufersa.cc.seg.common.network.SecureMessaging;
import br.edu.ufersa.cc.seg.common.network.SecureTcpMessaging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DnsServer {

    private static final byte[] ENC_KEY = java.util.Base64.getDecoder()
            .decode("DJXkb7GyuXP5Hfep9OLukQ==");
    private static final byte[] HMAC_KEY = java.util.Base64.getDecoder()
            .decode("QYp+xG2d7Ir8Xo2ZyD7m8FJKwrFrxd9ayN9i4mBQlTg=");

    private final CryptoService cryptoService = new CryptoService(ENC_KEY, HMAC_KEY);
    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, String> servers = new HashMap<>();
    private final List<SecureMessaging> listeners = new ArrayList<>();
    private final int port;

    /**
     * Inicia o servidor DNS, ouvindo por conexões e processando requisições
     * dos clientes.
     */
    public void start() {
        populateServers();

        try (final var serverSocket = new ServerSocket(port)) {
            log.info("DNS Server iniciado na porta {}", port);

            // Fica ouvindo novos clientes
            while (true) {
                // Aceita nova conexão
                final var messenger = new SecureTcpMessaging(serverSocket, cryptoService);

                // Delega uma nova thread para atender
                new Thread(() -> handleClient(messenger)).start();
            }
        } catch (final IOException e) {
            log.error("Erro ao iniciar servidor DNS", e);
        }
    }

    private void populateServers() {
        servers.put("servidor1", "192.168.0.10");
        servers.put("servidor2", "192.168.0.20");
        servers.put("servidor3", "192.168.0.30");
        servers.put("servidor4", "192.168.0.40");
        servers.put("servidor5", "192.168.0.50");
        servers.put("servidor6", "192.168.0.60");
        servers.put("servidor7", "192.168.0.70");
        servers.put("servidor8", "192.168.0.80");
        servers.put("servidor9", "192.168.0.90");
        servers.put("servidor10", "192.168.0.100");
    }

    private void handleClient(final SecureMessaging messenger) {
        // Inicializa valores padrão
        var registeredForNotify = false;

        try {
            while (true) {
                // Recebe mensagem criptografada em bytes
                final var requestInBytes = messenger.receiveSecure();

                // Descarta se payload for nulo
                if (requestInBytes == null)
                    break;

                // Transforma em JSON e obter tipo
                final var requestInJson = mapper.readTree(requestInBytes);
                final var type = requestInJson.get("type").asText();

                switch (type) {
                    case "QUERY": {
                        handleQuery(messenger, requestInJson);
                        break;
                    }
                    case "UPDATE": {
                        handleUpdate(messenger, requestInJson);
                        break;
                    }
                    case "REGISTER_NOTIFY": {
                        // Cliente quer receber notificações; mantemos a conexão aberta
                        registeredForNotify = true;
                        handleRegisterNotify(messenger);
                        break;
                    }
                    default: {
                        final var err = mapper.createObjectNode();
                        err.put("type", "ERROR");
                        err.put("message", "Unknown type: " + type);
                        messenger.sendSecure(mapper.writeValueAsBytes(err));
                        break;
                    }
                }
            }
        } catch (final IOException e) {
            log.error("Cliente desconectado ou erro de E/S: {}", e.getMessage());
        } catch (final CryptoException e) {
            log.error("Erro de criptografia: {}", e.getMessage());
        } catch (final Exception e) {
            log.error("Erro processando cliente", e);
        } finally {
            if (registeredForNotify && messenger != null) {
                listeners.remove(messenger);
            }
            if (messenger != null) {
                try {
                    messenger.close();
                } catch (final IOException e) {
                    // ignore
                }
            }
        }
    }

    private void handleQuery(final SecureMessaging messenger, final JsonNode request) throws IOException {
        // Obter o nome
        final var name = request.get("name").asText();

        // Encontrar o IP correspondente
        final var ip = servers.get(name);

        // Criar mensagem de resposta
        final var resp = mapper.createObjectNode();
        resp.put("type", "RESPONSE");
        resp.put("name", name);
        resp.put("ip", ip);

        messenger.sendSecure(mapper.writeValueAsBytes(resp));
    }

    private void handleUpdate(final SecureMessaging messenger, final JsonNode request) throws IOException {
        // Inclui novo registro
        final var name = request.get("name").asText();
        final var ip = request.get("ip").asText();
        servers.put(name, ip);

        // Imprime log
        log.info("Novo registro: {} -> {}", name, ip);
        printMap();

        // Envia ACK para o registrador
        final var response = mapper.createObjectNode();
        response.put("type", "ACK");
        response.put("name", name);
        response.put("ip", ip);
        messenger.sendSecure(mapper.writeValueAsBytes(response));

        // Envia NOTIFY para os listeners
        final var notify = mapper.createObjectNode();
        notify.put("type", "NOTIFY");
        notify.put("name", name);
        notify.put("ip", ip);
        final var notifyBytes = mapper.writeValueAsBytes(notify);

        for (final var listener : listeners) {
            try {
                listener.sendSecure(notifyBytes);
            } catch (final IOException e) {
                // Problema com listener — remove
                listeners.remove(listener);
                try {
                    listener.close();
                } catch (final IOException ex) {
                    // Ignora
                }
            }
        }
    }

    private void handleRegisterNotify(final SecureMessaging messenger) throws IOException {
        // Adiciona conexão à lista de listeners
        listeners.add(messenger);

        // Cria mensagem confirmando a inscrição
        final var response = mapper.createObjectNode();
        response.put("type", "REGISTERED");

        // Envia mensagem
        messenger.sendSecure(mapper.writeValueAsBytes(response));
    }

    private void printMap() {
        final var text = new StringBuilder("ESTADO ATUAL DA TABELA DNS:\n");
        servers.forEach((name, ip) -> text.append(name).append(" -> ").append(ip).append("\n"));

        log.info(text.toString());
    }

}
