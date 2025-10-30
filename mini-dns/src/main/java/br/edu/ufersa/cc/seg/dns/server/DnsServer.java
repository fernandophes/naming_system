package br.edu.ufersa.cc.seg.dns.server;

import br.edu.ufersa.cc.seg.common.crypto.CryptoService;
import br.edu.ufersa.cc.seg.common.network.SecureMessaging;
import br.edu.ufersa.cc.seg.common.network.SecureTcpMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DnsServer {

    private final Map<String, String> dnsRecords;
    private final CryptoService cryptoService;
    private final int port;

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
        try (SecureMessaging secureComm = new SecureTcpMessaging(clientSocket, cryptoService)) {
            // TODO: Implementar protocolo de comunicação
            // 1. Receber requisição
            // 2. Validar tipo (consulta/atualização)
            // 3. Processar
            // 4. Enviar resposta
        } catch (IOException e) {
            log.error("Erro ao processar cliente", e);
        }
    }

}
