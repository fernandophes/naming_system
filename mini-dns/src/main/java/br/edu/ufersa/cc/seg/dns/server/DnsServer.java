package br.edu.ufersa.cc.seg.dns.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import br.edu.ufersa.cc.seg.common.crypto.CryptoService;
import br.edu.ufersa.cc.seg.common.network.SecureMessaging;
import br.edu.ufersa.cc.seg.common.network.SecureTcpMessaging;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DnsServer {

    private final Map<String, String> dnsRecords;
    private final CryptoService cryptoService;
    private final int port;

    // Lista de clientes que se registraram para receber NOTIFY
    private final List<SecureMessaging> notifyListeners = new CopyOnWriteArrayList<>();

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Construtor do servidor DNS
     * 
     * Define atributos e já popula registros iniciais
     * 
     * @param cryptoService
     * @param port
     */
    public DnsServer(final CryptoService cryptoService, final int port) {
        this.cryptoService = cryptoService;
        this.port = port;
        this.dnsRecords = new ConcurrentHashMap<>();

        // Registros iniciais
        dnsRecords.put("servidor1", "192.168.0.10");
        dnsRecords.put("servidor2", "192.168.0.20");
        dnsRecords.put("servidor3", "192.168.0.30");
        dnsRecords.put("servidor4", "192.168.0.40");
        dnsRecords.put("servidor5", "192.168.0.50");
        dnsRecords.put("servidor6", "192.168.0.60");
        dnsRecords.put("servidor7", "192.168.0.70");
        dnsRecords.put("servidor8", "192.168.0.80");
        dnsRecords.put("servidor9", "192.168.0.90");
        dnsRecords.put("servidor10", "192.168.0.100");
    }

    /**
     * Inicia o servidor DNS, ouvindo por conexões e processando requisições
     * dos clientes.
     */
    public void start() {
        try (val serverSocket = new ServerSocket(port)) {
            log.info("DNS Server iniciado na porta {}", port);

            // Fica ouvindo novos clientes
            while (true) {
                // Aceita nova conexão
                val clientSocket = new SecureTcpMessaging(serverSocket, cryptoService);

                // Delega uma nova thread para atender
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (final IOException e) {
            log.error("Erro ao iniciar servidor DNS", e);
        }
    }

    private void handleClient(final SecureMessaging secureComm) {
        // Inicializa valores padrão
        var registeredForNotify = false;

        try {
            while (true) {
                // Recebe mensagem criptografada em bytes
                val payload = secureComm.receiveSecure();

                // Descarta se payload for nulo
                if (payload == null)
                    break;

                // Transforma em JSON e obter tipo
                val body = new String(payload, StandardCharsets.UTF_8);
                val node = mapper.readTree(body);
                val type = node.get("type").asText();

                switch (type) {
                    case "QUERY": {
                        handleQuery(secureComm, node);
                        break;
                    }
                    case "UPDATE": {
                        handleUpdate(secureComm, node);
                        break;
                    }
                    case "REGISTER_NOTIFY": {
                        // Cliente quer receber notificações; mantemos a conexão aberta
                        registeredForNotify = true;
                        handleRegisterNotify(secureComm);
                        break;
                    }
                    default: {
                        final ObjectNode err = mapper.createObjectNode();
                        err.put("type", "ERROR");
                        err.put("message", "Unknown type: " + type);
                        secureComm.sendSecure(mapper.writeValueAsBytes(err));
                        break;
                    }
                }
            }
        } catch (final IOException e) {
            log.debug("Cliente desconectado ou erro de E/S: {}", e.getMessage());
        } catch (final Exception e) {
            log.error("Erro processando cliente", e);
        } finally {
            if (registeredForNotify && secureComm != null) {
                notifyListeners.remove(secureComm);
            }
            if (secureComm != null) {
                try {
                    secureComm.close();
                } catch (final IOException e) {
                    // ignore
                }
            }
        }
    }

    private void handleQuery(final SecureMessaging secureComm, final JsonNode node) throws IOException {
        // Obter o nome
        val name = node.get("name").asText();

        // Encontrar o IP correspondente
        val ip = dnsRecords.get(name);

        // Criar mensagem de resposta
        val resp = mapper.createObjectNode();
        resp.put("type", "RESPONSE");
        resp.put("name", name);
        resp.put("ip", ip);

        secureComm.sendSecure(mapper.writeValueAsBytes(resp));
    }

    private void handleUpdate(final SecureMessaging secureComm, final JsonNode node) throws IOException {
        // Inclui novo registro
        val name = node.get("name").asText();
        val ip = node.get("ip").asText();
        dnsRecords.put(name, ip);

        // Imprime log
        log.info("Novo registro: {} -> {}", name, ip);
        printMap();

        // Envia ACK para o registrador
        val response = mapper.createObjectNode();
        response.put("type", "ACK");
        response.put("name", name);
        response.put("ip", ip);
        secureComm.sendSecure(mapper.writeValueAsBytes(response));

        // Envia NOTIFY para os listeners
        val notify = mapper.createObjectNode();
        notify.put("type", "NOTIFY");
        notify.put("name", name);
        notify.put("ip", ip);
        val notifyBytes = mapper.writeValueAsBytes(notify);

        for (val listener : notifyListeners) {
            try {
                listener.sendSecure(notifyBytes);
            } catch (final IOException e) {
                // Problema com listener — remove
                notifyListeners.remove(listener);
                try {
                    listener.close();
                } catch (final IOException ex) {
                    // Ignora
                }
            }
        }
    }

    private void handleRegisterNotify(final SecureMessaging secureComm) throws IOException {
        // Adiciona conexão à lista de listeners
        notifyListeners.add(secureComm);

        // Cria mensagem confirmando a inscrição
        val response = mapper.createObjectNode();
        response.put("type", "REGISTERED");

        // Envia mensagem
        secureComm.sendSecure(mapper.writeValueAsBytes(response));
    }

    private void printMap() {
        val text = new StringBuilder("ESTADO ATUAL DA TABELA DNS:\n");
        dnsRecords.forEach((name, ip) -> text.append(name).append(" -> ").append(ip).append("\n"));

        log.info(text.toString());
    }

}
