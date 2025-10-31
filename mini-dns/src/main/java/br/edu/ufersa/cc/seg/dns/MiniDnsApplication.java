package br.edu.ufersa.cc.seg.dns;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import br.edu.ufersa.cc.seg.dns.server.DnsServer;
import lombok.AllArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@SpringBootApplication
public class MiniDnsApplication implements CommandLineRunner {

    private final DnsServer dnsServer;

    public static void main(final String[] args) {
        SpringApplication.run(MiniDnsApplication.class, args);
    }

    @Override
    public void run(final String... args) throws Exception {
        // Inicia o servidor DNS em porta 9000
        val serverThread = new Thread(dnsServer::start, "dns-server-thread");
        serverThread.setDaemon(true);
        serverThread.start();

        log.info("Mini DNS iniciado na porta 9000");
    }

}
