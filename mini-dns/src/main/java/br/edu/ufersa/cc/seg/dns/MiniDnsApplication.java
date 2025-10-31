package br.edu.ufersa.cc.seg.dns;

import java.util.Base64;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import br.edu.ufersa.cc.seg.common.crypto.CryptoService;
import br.edu.ufersa.cc.seg.dns.server.DnsServer;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class MiniDnsApplication implements CommandLineRunner {

    // Chaves fixas para demo
    private static final byte[] ENC_KEY = Base64.getDecoder().decode("DJXkb7GyuXP5Hfep9OLukQ==");
    private static final byte[] HMAC_KEY = Base64.getDecoder().decode("QYp+xG2d7Ir8Xo2ZyD7m8FJKwrFrxd9ayN9i4mBQlTg=");

    public static void main(String[] args) {
        SpringApplication.run(MiniDnsApplication.class, args);
    }

    @Override
    public void run(final String... args) throws Exception {
        // Constrói serviço de criptografia
        val cryptoService = new CryptoService(ENC_KEY, HMAC_KEY);

        // Inicia o servidor DNS em porta 9000
        val dnsServer = new DnsServer(cryptoService, 9000);
        val serverThread = new Thread(dnsServer::start, "dns-server-thread");
        serverThread.setDaemon(true);
        serverThread.start();

        log.info("Mini DNS iniciado na porta 9000");
    }

}
