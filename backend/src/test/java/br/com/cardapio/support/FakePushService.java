package br.com.cardapio.support;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Um serviço de push de mentira, na própria máquina (127.0.0.1, porta livre): guarda cada
 * notificação recebida e responde 201, ou 410 nos caminhos que começam com /gone.
 */
public final class FakePushService implements AutoCloseable {

    /** O que chegou: o caminho (quem é o navegador), os cabeçalhos e o corpo cifrado. */
    public record Received(String path, Map<String, String> headers, byte[] body) {
    }

    private final HttpServer server;
    public final BlockingQueue<Received> received = new LinkedBlockingQueue<>();

    public FakePushService() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = exchange.getRequestBody().readAllBytes();
            Map<String, String> headers = new java.util.HashMap<>();
            exchange.getRequestHeaders().forEach((name, values) -> headers.put(name.toLowerCase(), values.getFirst()));
            String path = exchange.getRequestURI().getPath();
            received.add(new Received(path, headers, body));
            exchange.sendResponseHeaders(path.startsWith("/gone") ? 410 : 201, -1);
            exchange.close();
        });
        server.start();
    }

    /** O endereço de um "navegador" neste serviço. */
    public String endpoint(String name) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/" + name;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
