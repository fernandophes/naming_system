package br.edu.ufersa.cc.seg.dns;

import br.edu.ufersa.cc.seg.common.crypto.CryptoService;
import br.edu.ufersa.cc.seg.dns.server.DnsServer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.security.SecureRandom;

@SpringBootApplication
public class MiniDnsApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(MiniDnsApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Gera chaves temporárias para demo (não use isso em produção)
        SecureRandom random = new SecureRandom();
        byte[] encKey = new byte[16];
        byte[] hmacKey = new byte[32];
        random.nextBytes(encKey);
        random.nextBytes(hmacKey);

        CryptoService cryptoService = new CryptoService(encKey, hmacKey);

        // Inicia o servidor DNS em porta 9000
        DnsServer dnsServer = new DnsServer(cryptoService, 9000);
        Thread serverThread = new Thread(dnsServer::start, "dns-server-thread");
        serverThread.setDaemon(true);
        serverThread.start();

        System.out.println("Mini DNS iniciado na porta 9000");
    }
}
