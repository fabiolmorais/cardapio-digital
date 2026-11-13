package br.com.cardapio.adapter.out.realtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Um repassador TCP entre o teste e o Postgres que sabe "congelar" as conexões já abertas: elas
 * continuam abertas dos dois lados, mas nada mais passa, nem o aviso de fechamento. É o que
 * acontece quando a máquina do Fly é suspensa e o banco fecha o outro lado nesse meio tempo, ou
 * quando a rede some. As conexões novas, abertas depois do congelamento, passam normalmente.
 */
final class FreezingProxy implements AutoCloseable {

    private final String targetHost;
    private final int targetPort;
    private final ServerSocket server;
    private final List<AtomicBoolean> frozen = new CopyOnWriteArrayList<>();
    private final List<Socket> sockets = new CopyOnWriteArrayList<>();

    FreezingProxy(String targetHost, int targetPort) throws IOException {
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        Thread.ofVirtual().start(this::accept);
    }

    int port() {
        return server.getLocalPort();
    }

    /** Congela as conexões abertas até agora. */
    void freezeOpenConnections() {
        frozen.forEach(flag -> flag.set(true));
    }

    private void accept() {
        while (!server.isClosed()) {
            try {
                Socket client = server.accept();
                Socket target = new Socket(targetHost, targetPort);
                sockets.add(client);
                sockets.add(target);
                AtomicBoolean flag = new AtomicBoolean(false);
                frozen.add(flag);
                Thread.ofVirtual().start(() -> pipe(client, target, flag));
                Thread.ofVirtual().start(() -> pipe(target, client, flag));
            } catch (IOException e) {
                return; // fechado
            }
        }
    }

    /** Copia os bytes de um lado para o outro; congelada, lê e descarta, sem fechar nada. */
    private static void pipe(Socket from, Socket to, AtomicBoolean frozen) {
        byte[] buffer = new byte[8192];
        try (InputStream in = from.getInputStream()) {
            OutputStream out = to.getOutputStream();
            int read;
            while ((read = in.read(buffer)) != -1) {
                if (!frozen.get()) {
                    out.write(buffer, 0, read);
                    out.flush();
                }
            }
            if (!frozen.get()) {
                to.shutdownOutput();
            }
        } catch (IOException e) {
            // a conexão foi fechada: acabou
        }
    }

    @Override
    public void close() throws IOException {
        server.close();
        for (Socket socket : sockets) {
            socket.close();
        }
    }
}
