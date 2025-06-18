package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.example.Request.createRequest;

public class Server {
    private final int port;
    private final List<String> validPaths = List.of(
            "/index.html", "/spring.svg", "/spring.png", "/resources.html",
            "/styles.css", "/app.js", "/links.html", "/forms.html",
            "/classic.html", "/events.html", "/events.js"
    );
    private final ExecutorService threadPool;

    public Server(int port) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(64); // Создаем пул потоков
    }

    public void start() {
        try (final var serverSocket = new ServerSocket(port)) {
            System.out.println("Server is running on port " + port);
            while (true) {
                try {
                    final var socket = serverSocket.accept();
                    threadPool.submit(() -> handleConnection(socket)); // Обработка подключения в потоке
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @lombok.SneakyThrows
    private void handleConnection(Socket socket)  throws URISyntaxException {
        try (
                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream());
        ) {
            // Читаем запрос
            Request request = createRequest(new BufferedInputStream(socket.getInputStream()));
            if (request == null) {
                out.write("HTTP/1.1 400 Bad Request\r\n\r\n".getBytes());
                out.flush();
                return;
            }

            final var path = request.getPath();
            if (!validPaths.contains(path.split("\\?")[0])) { // Проверка пути без Query
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
                return;
            }

            // Обработка запроса
            String lastParam = request.getQueryParam("last") != null ? request.getQueryParam("last").getValue() : null;
            System.out.println("Last parameter: " + lastParam);

            // Остальная логика обработки запроса...
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close(); // Закрываем сокет после завершения обработки
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}