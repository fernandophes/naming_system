package br.edu.ufersa.cc.seg.p2p;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.ufersa.cc.seg.common.crypto.CryptoService;
import br.edu.ufersa.cc.seg.common.network.SecureTcpMessaging;
import br.edu.ufersa.cc.seg.p2p.node.NodeServer;

public class Demo {

    private static final byte[] ENC_KEY = java.util.Base64.getDecoder().decode("DJXkb7GyuXP5Hfep9OLukQ==");
    private static final byte[] HMAC_KEY = java.util.Base64.getDecoder()
            .decode("QYp+xG2d7Ir8Xo2ZyD7m8FJKwrFrxd9ayN9i4mBQlTg=");

    private static final CryptoService cryptoService = new CryptoService(ENC_KEY, HMAC_KEY);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        final var nodeServer = NodeServer.createRing("localhost", List.of(8090, 8091, 8092, 8093, 8094, 8095));

        try (var messenger = new SecureTcpMessaging(nodeServer.getHost(), nodeServer.getPort(), cryptoService)) {
            // Cria requisição
            final var request = mapper.createObjectNode();
            request.put("type", "SEARCH");
            request.put("fileName", "arquivo1");

            // Envia requisição
            messenger.sendSecure(mapper.writeValueAsBytes(request));

            // Recebe a resposta
            final var responseInBytes = messenger.receiveSecure();
            final var response = mapper.readTree(responseInBytes);
            System.out.println("Resposta recebida: " + response.toPrettyString());
        }

    }

}
