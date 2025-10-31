package br.edu.ufersa.cc.seg.dns.client;

import br.edu.ufersa.cc.seg.common.crypto.CryptoService;
import br.edu.ufersa.cc.seg.common.network.SecureMessaging;
import br.edu.ufersa.cc.seg.common.network.SecureTcpMessaging;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.Socket;
import java.util.Base64;

/**
 * Cliente simples que envia UPDATE (registrador)
 * args: <host> <port> <encKeyBase64> <hmacKeyBase64> <name> <ip>
 */
public class Registrador {

    public static void main(String[] args) throws Exception {
        if (args.length < 6) {
            System.out.println("Usage: Registrador <host> <port> <encKeyBase64> <hmacKeyBase64> <name> <ip>");
            System.exit(1);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        byte[] encKey = Base64.getDecoder().decode(args[2]);
        byte[] hmacKey = Base64.getDecoder().decode(args[3]);
        String name = args[4];
        String ip = args[5];

        CryptoService crypto = new CryptoService(encKey, hmacKey);
        ObjectMapper mapper = new ObjectMapper();

        try (Socket s = new Socket(host, port);
             SecureMessaging comm = new SecureTcpMessaging(s, crypto)) {

            ObjectNode upd = mapper.createObjectNode();
            upd.put("type", "UPDATE");
            upd.put("name", name);
            upd.put("ip", ip);

            comm.sendSecure(mapper.writeValueAsBytes(upd));

            byte[] resp = comm.receiveSecure();
            System.out.println("Resposta: " + new String(resp));
        }
    }

}
