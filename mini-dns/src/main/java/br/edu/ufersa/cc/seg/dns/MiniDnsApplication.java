package br.edu.ufersa.cc.seg.dns;

import br.edu.ufersa.cc.seg.dns.server.DnsServer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MiniDnsApplication {

    private static final int PORT = 9000;
    private static final DnsServer DNS_SERVER = new DnsServer(PORT);

    public static void main(final String[] args) {
        // Inicia o servidor DNS em porta 9000
        final var serverThread = new Thread(DNS_SERVER::start);
        serverThread.setDaemon(true);
        serverThread.start();

        log.info("Mini DNS iniciado na porta 9000");
    }

}
