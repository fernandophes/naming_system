package br.edu.ufersa.cc.seg.dns;

import br.edu.ufersa.cc.seg.dns.server.DnsServer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MiniDnsApplication {

    private static final int PORT = 9000;

    public static void main(final String[] args) {
        new DnsServer(PORT).start();
    }

}
