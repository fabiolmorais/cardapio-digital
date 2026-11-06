package br.com.cardapio.support;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A API do Resend de mentira, na própria máquina (127.0.0.1, porta livre): guarda cada e-mail
 * recebido (o cabeçalho de autorização e o JSON) e responde 200, ou o status que o teste mandar.
 */
public final class FakeResend implements AutoCloseable {

    public record Received(String path, String authorization, String json) {
    }

    private final HttpServer server;
    public final BlockingQueue<Received> received = new LinkedBlockingQueue<>();
    public volatile int status = 200;

    public FakeResend() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            received.add(new Received(exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("Authorization"), json));
            byte[] answer = "{\"id\":\"teste\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, answer.length);
            exchange.getResponseBody().write(answer);
            exchange.close();
        });
        server.start();
    }

    public URI url() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
