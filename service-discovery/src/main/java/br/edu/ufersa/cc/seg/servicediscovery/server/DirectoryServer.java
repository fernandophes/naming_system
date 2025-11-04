package br.edu.ufersa.cc.seg.servicediscovery.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.ufersa.cc.seg.common.crypto.CryptoService;
import br.edu.ufersa.cc.seg.common.network.SecureMessaging;
import br.edu.ufersa.cc.seg.common.network.SecureTcpMessaging;
import br.edu.ufersa.cc.seg.servicediscovery.load_balancer.LoadBalancer;
import br.edu.ufersa.cc.seg.servicediscovery.load_balancer.RandomLoadBalancer;
import br.edu.ufersa.cc.seg.servicediscovery.load_balancer.RoundRobinLoadBalancer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Servidor de diretório simples para descoberta de serviços.
 * Aceita registros do tipo REGISTER {type, service, address}
 * e consultas DISCOVER {type, service} retornando lista de addresses.
 */
@Slf4j
public class DirectoryServer {

    @Data
    private static class RegistryItem {
        private final List<String> addresses = Collections.synchronizedList(new ArrayList<>());
        private final LoadBalancer<String> loadBalancer;

        public static RegistryItem empty() {
            return new RegistryItem(new RoundRobinLoadBalancer());
        }

        public String getNextAddress() {
            return loadBalancer.choose(addresses);
        }
    }

    /*
     * Campos JSON para troca de mensagens
     */
    private static final String TYPE = "type";
    private static final String SERVICE = "service";
    private static final String ADDRESS = "address";

    private static final int DEFAULT_PORT = 9100;
    private static final LoadBalancer<String> DEFAULT_LOAD_BALANCER = new RandomLoadBalancer();

    // Mesmas chaves usadas pelos exemplos do projeto (demo)
    private static final byte[] ENC_KEY = java.util.Base64.getDecoder()
            .decode("DJXkb7GyuXP5Hfep9OLukQ==");
    private static final byte[] HMAC_KEY = java.util.Base64.getDecoder()
            .decode("QYp+xG2d7Ir8Xo2ZyD7m8FJKwrFrxd9ayN9i4mBQlTg=");

    private final CryptoService crypto = new CryptoService(ENC_KEY, HMAC_KEY);
    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, RegistryItem> registry = Collections.synchronizedMap(new HashMap<>());

    public static void main(final String[] args) throws Exception {
        final int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new DirectoryServer().start(port);
    }

    public void start(final int port) throws IOException {
        log.info("Iniciando DirectoryServer em porta {}", port);
        try (final var server = new ServerSocket(port)) {
            while (true) {
                final var client = new SecureTcpMessaging(server, crypto);
                new Thread(() -> handleClient(client)).start();
            }
        }
    }

    private void handleClient(final SecureMessaging messenger) {
        try {
            // Esperar e receber requisição
            final var requestInBytes = messenger.receiveSecure();
            final var requestInJson = mapper.readTree(requestInBytes);
            

            // Redirecionar conforme o tipo
            final var type = requestInJson.get(TYPE).asText();
            switch (type) {
                case "REGISTER":
                    handleRegister(requestInJson, messenger);
                    break;
                case "DISCOVER":
                    handleDiscover(requestInJson, messenger);
                    break;
                default:
                    log.warn("Tipo de mensagem desconhecido: {}", type);
            }

        } catch (final Exception e) {
            // Pode ser CryptoException ao decodificar (chave errada) — descarta
            log.warn("Mensagem descartada / erro no cliente: {}", e.getMessage());
        }
    }

    private void handleRegister(final JsonNode request, final SecureMessaging messenger) throws IOException {
        // Obtém nome e endereço do serviço a ser registrado
        final var serviceName = request.get(SERVICE).asText();
        final var address = request.get(ADDRESS).asText();

        // Adiciona à tabela (ou a atualiza)
        registry.computeIfAbsent(serviceName, ignoredSvcName -> new RegistryItem(DEFAULT_LOAD_BALANCER));
        registry.get(serviceName).getAddresses().add(address);

        // Cria resposta
        final var resp = mapper.createObjectNode();
        resp.put(TYPE, "REGISTERED");
        resp.put(SERVICE, serviceName);
        resp.put(ADDRESS, address);

        // Envia resposta
        messenger.sendSecure(mapper.writeValueAsBytes(resp));
    }

    private void handleDiscover(final JsonNode request, final SecureMessaging messenger) throws IOException {
        // Obtém nome e endereço do serviço a ser buscado
        final var serviceName = request.get(SERVICE).asText();
        log.info("Buscando endereços do servidor '{}'...", serviceName);

        // Cria resposta
        final var response = mapper.createObjectNode();
        response.put(TYPE, "DISCOVERY_RESPONSE");
        response.put(SERVICE, serviceName);

        // Adiciona endereço escolhido
        final var registryItem = registry.getOrDefault(serviceName, RegistryItem.empty());
        final var chosen = registryItem.getNextAddress();
        response.put(ADDRESS, chosen);

        log.info("Endereço fornecido: {}", chosen);

        // Envia resposta
        messenger.sendSecure(mapper.writeValueAsBytes(response));
    }

}
