package br.edu.ufersa.cc.seg.dns.server;

import br.edu.ufersa.cc.seg.common.crypto.CryptoService;
import br.edu.ufersa.cc.seg.common.network.SecureMessaging;
import br.edu.ufersa.cc.seg.common.network.SecureTcpMessaging;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class DnsServer {

    private final Map<String, String> dnsRecords;
    private final CryptoService cryptoService;
    private final int port;

    // Lista de clientes que se registraram para receber NOTIFY
    private final List<SecureMessaging> notifyListeners = new CopyOnWriteArrayList<>();

    private final ObjectMapper mapper = new ObjectMapper();

    public DnsServer(CryptoService cryptoService, int port) {
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

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("DNS Server iniciado na porta {}", port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            log.error("Erro ao iniciar servidor DNS", e);
        }
    }

    private void handleClient(Socket clientSocket) {
        SecureMessaging secureComm = null;
        boolean registeredForNotify = false;
        try {
            secureComm = new SecureTcpMessaging(clientSocket, cryptoService);

            while (true) {
                byte[] payload = secureComm.receiveSecure();
                if (payload == null) break;
                String body = new String(payload, StandardCharsets.UTF_8);
                JsonNode node = mapper.readTree(body);
                String type = node.get("type").asText();

                switch (type) {
                    case "QUERY": {
                        String name = node.get("name").asText();
                        String ip = dnsRecords.get(name);
                        ObjectNode resp = mapper.createObjectNode();
                        resp.put("type", "RESPONSE");
                        resp.put("name", name);
                        if (ip != null) {
                            resp.put("ip", ip);
                        } else {
                            resp.put("status", "NX");
                        }
                        secureComm.sendSecure("client", mapper.writeValueAsBytes(resp));
                        break;
                    }
                    case "UPDATE": {
                        String name = node.get("name").asText();
                        String ip = node.get("ip").asText();
                        dnsRecords.put(name, ip);

                        // ACK para o registrador
                        ObjectNode ack = mapper.createObjectNode();
                        ack.put("type", "ACK");
                        ack.put("name", name);
                        ack.put("ip", ip);
                        secureComm.sendSecure("client", mapper.writeValueAsBytes(ack));

                        // Envia NOTIFY para listeners
                        ObjectNode notify = mapper.createObjectNode();
                        notify.put("type", "NOTIFY");
                        notify.put("name", name);
                        notify.put("ip", ip);

                        byte[] notifyBytes = mapper.writeValueAsBytes(notify);
                        for (SecureMessaging listener : notifyListeners) {
                            try {
                                listener.sendSecure("notify", notifyBytes);
                            } catch (IOException e) {
                                // problema com listener — remove
                                notifyListeners.remove(listener);
                                try {
                                    listener.close();
                                } catch (IOException ex) {
                                    // ignore
                                }
                            }
                        }

                        break;
                    }
                    case "REGISTER_NOTIFY": {
                        // Cliente quer receber notificações; mantemos a conexão aberta
                        notifyListeners.add(secureComm);
                        registeredForNotify = true;
                        ObjectNode ok = mapper.createObjectNode();
                        ok.put("type", "REGISTERED");
                        secureComm.sendSecure("client", mapper.writeValueAsBytes(ok));
                        // continue loop to keep connection
                        break;
                    }
                    default: {
                        ObjectNode err = mapper.createObjectNode();
                        err.put("type", "ERROR");
                        err.put("message", "Unknown type: " + type);
                        secureComm.sendSecure("client", mapper.writeValueAsBytes(err));
                        break;
                    }
                }
            }
        } catch (IOException e) {
            log.debug("Cliente desconectado ou erro de E/S: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Erro processando cliente", e);
        } finally {
            if (registeredForNotify && secureComm != null) {
                notifyListeners.remove(secureComm);
            }
            if (secureComm != null) {
                try {
                    secureComm.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

}
