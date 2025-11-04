package br.edu.ufersa.cc.seg.p2p.node;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.ufersa.cc.seg.common.crypto.CryptoService;
import br.edu.ufersa.cc.seg.common.network.SecureMessaging;
import br.edu.ufersa.cc.seg.common.network.SecureTcpMessaging;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servidor simples de nó para simular anel P2P.
 */
@Slf4j
@Getter
@RequiredArgsConstructor
public class NodeServer {

    private static final byte[] ENC_KEY = java.util.Base64.getDecoder().decode("DJXkb7GyuXP5Hfep9OLukQ==");
    private static final byte[] HMAC_KEY = java.util.Base64.getDecoder()
            .decode("QYp+xG2d7Ir8Xo2ZyD7m8FJKwrFrxd9ayN9i4mBQlTg=");

    private final int id;
    private final String host;
    private final int port;
    private NodeServer previous;
    private NodeServer next;

    private final CryptoService cryptoService = new CryptoService(ENC_KEY, HMAC_KEY);
    private final ObjectMapper mapper = new ObjectMapper();
    private final Set<String> localFiles = Collections.synchronizedSet(new HashSet<>());

    public static NodeServer createRing(final String host, final Collection<Integer> ports) {
        final var index = new AtomicInteger(0);

        final var nodeServers = ports.stream()
                .map(port -> new NodeServer(index.getAndIncrement(), host, port))
                .toList();

        for (var i = 0; i < nodeServers.size(); i++) {
            final var currentNode = nodeServers.get(i);
            final var previousNode = nodeServers.get((i - 1 + nodeServers.size()) % nodeServers.size());
            final var nextNode = nodeServers.get((i + 1) % nodeServers.size());

            currentNode.previous = previousNode;
            currentNode.next = nextNode;

            for (int j = 1; j <= 10; j++) {
                final var fileNumber = i * 10 + j;
                currentNode.localFiles.add("arquivo" + fileNumber);
            }

            new Thread(() -> {
                try {
                    currentNode.start();
                } catch (final IOException e) {
                    log.error("Erro ao iniciar o nó {}: {}", currentNode.getId(), e.getMessage());
                }
            }).start();
        }

        return nodeServers.getFirst();
    }

    public void start() throws IOException {
        log.info("[Nó {}] Iniciando em {}:{}... {} << {} >> {}", id, host, port, previous.getId(), id, next.getId());

        try (final var server = new ServerSocket(port)) {
            while (true) {
                final var messenger = new SecureTcpMessaging(server, cryptoService);
                new Thread(() -> handleClient(messenger)).start();
            }
        }
    }

    private void handleClient(final SecureMessaging messenger) {
        try {
            // Aguarda e recebe a requisição
            final var requestInBytes = messenger.receiveSecure();
            final var json = new String(requestInBytes);
            final var request = mapper.readTree(json);
            log.info("[Nó {}] Recebido: {}", id, json);

            // Obtém o tipo de mensagem
            final var type = request.path("type").asText(null);
            if (type == null) {
                log.error("Mensagem sem tipo");
                return;
            }

            // Obtém os dados da origem
            final int originId = request.path("originId").asInt(id);

            switch (type.toUpperCase()) {
                case "SEARCH":
                    // Se for uma busca de arquivo
                    handleSearchMessage(request, originId);
                    break;

                case "RESPONSE":
                    // Se for uma resposta de busca
                    handleResponseMessage(request, originId);
                    break;

                default:
                    log.info("[Nó {}] tipo desconhecido: {}", id, type);
            }

        } catch (final Exception e) {
            log.error("Erro no nó", e);
        }
    }

    private void handleSearchMessage(JsonNode request, final int originId) {
        final var fileName = request.path("fileName").asText(null);

        if (fileName == null) {
            log.error("[Nó {}] Nome do arquivo nulo (inválido).", id, fileName);
        } else if (localFiles.contains(fileName)) {
            final var response = mapper.createObjectNode();
            response.put("type", "RESPONSE");
            response.put("originId", originId);
            response.put("fileName", fileName);
            response.put("holderId", id);

            sendToNext(response.toString());
        } else {
            if (originId == id) {
                // Se já tiver percorrido o anel inteiro e voltar ao nó de origem, interrompe e
                // informa o erro
                log.info("[Nó {}] arquivo '{}' não encontrado no anel.", id, fileName);
            } else {
                // Busca no próximo nó
                sendToNext(request.toString());
            }
        }
    }

    private void handleResponseMessage(final JsonNode request, final int originId) {
        if (originId == id) {
            // Se esse foi o nó de origem, apresenta a resposta
            log.info("[Nó {}] RESPONSE recebido: {}", id, request.toString());
        } else {
            // Senão, vai propagando até chegar ao nó responsável
            sendToNext(request.toString());
        }
    }

    private void sendToNext(final String msg) {
        final var destinationHost = next.getHost();
        final int destinationPort = next.getPort();

        try (final var messenger = new SecureTcpMessaging(destinationHost, destinationPort, cryptoService)) {
            messenger.sendSecure(msg.getBytes());
        } catch (final Exception e) {
            log.error("[Nó {}] falha ao enviar para {}:{} -> {}", id, destinationHost, destinationPort, e.getMessage());
        }
    }

}
