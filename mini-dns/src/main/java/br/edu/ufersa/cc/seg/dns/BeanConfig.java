package br.edu.ufersa.cc.seg.dns;

import java.util.Base64;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.edu.ufersa.cc.seg.common.crypto.CryptoService;
import br.edu.ufersa.cc.seg.dns.server.DnsServer;
import lombok.val;

@Configuration
public class BeanConfig {

    @Bean
    CryptoService cryptoService() {
        val encKey = Base64.getDecoder().decode("DJXkb7GyuXP5Hfep9OLukQ==");
        val hmacKey = Base64.getDecoder().decode("QYp+xG2d7Ir8Xo2ZyD7m8FJKwrFrxd9ayN9i4mBQlTg=");

        return new CryptoService(encKey, hmacKey);
    }

    @Bean
    DnsServer dnsServer() {
        return new DnsServer(cryptoService(), 9000);
    }

}
