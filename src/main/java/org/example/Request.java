package org.example;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Request {
    private final String method;
    private final String path;
    private final List<String> headers;
    private final List<NameValuePair> params;

    private static final String GET = "GET";
    private static final String POST = "POST";

    // Конструктор без параметров
    public Request(String method, String path) {
        this.method = method;
        this.path = path;
        this.headers = Collections.emptyList(); // Используем пустой список вместо null
        this.params = Collections.emptyList(); // Также и для параметров
    }

    // Конструктор с параметрами
    public Request(String method, String path, List<String> headers, List<NameValuePair> params) {
        this.method = method;
        this.path = path;
        this.headers = headers != null ? headers : Collections.emptyList(); // Защита от null
        this.params = params != null ? params : Collections.emptyList(); // Защита от null
    }

    // Метод для создания объекта Request из входного потока
    static Request createRequest(BufferedInputStream in) throws IOException, URISyntaxException {
        final List<String> allowedMethods = List.of(GET, POST);
        final var limit = 4096;
        in.mark(limit);
        final var buffer = new byte[limit];
        final var read = in.read(buffer);

        // Ищем строку запроса
        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            return null; // Не удалось найти строку запроса
        }

        // Читаем строку запроса
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            return null; // Неверный формат строки запроса
        }

        final var method = requestLine[0];
        if (!allowedMethods.contains(method)) {
            return null; // Неподдерживаемый метод
        }

        final var path = requestLine[1];

        // Ищем заголовки
        final var headerDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headerDelimiter, headersStart, read);
        if (headersEnd == -1) {
            return null; // Не удалось найти заголовки
        }

        // Отматываем на начало буфера
        in.reset();
        // Пропускаем строку запроса
        in.skip(headersStart);

        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        List<String> headers = Arrays.asList(new String(headersBytes).split("\r\n"));

        // Извлечение параметров из Query String
        List<NameValuePair> params = URLEncodedUtils.parse(new URI(path), StandardCharsets.UTF_8);

        return new Request(method, path, headers, params);
    }

    // Метод для поиска индекса подмассива в массиве
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    // Получение значения параметра по имени
    public NameValuePair getQueryParam(String name) {
        return params.stream()
                .filter(param -> param.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null); // Возвращаем null, если параметр не найден
    }

    public List<NameValuePair> getQueryParams() {
        return params;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public List<String> getHeaders() {
        return headers;
    }
}