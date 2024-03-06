package org.example;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApi {

    private final int requestLimit;
    private final long timeIntervalMillis;
    private final Lock lock = new ReentrantLock();
    private long lastRequestTimestamp = System.currentTimeMillis();
    private int requestCount = 0;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestLimit = requestLimit;
        this.timeIntervalMillis = timeUnit.toMillis(1);
    }

    public void createDocument(Document document, String signature) {
        try {
            lock.lock();
            checkRateLimit();

            // Создаем JSON объект с данными документа
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode documentJson = objectMapper.valueToTree(document);

            // Для примера, выводим JSON в консоль
            System.out.println("Sending JSON: " + documentJson);

            // Выполняем HTTP POST запрос к API
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .header("Content-Type", "application/json")
                    .header("Signature", signature)  // Добавляем заголовок с подписью
                    .POST(HttpRequest.BodyPublishers.ofString(documentJson.toString()))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Обрабатываем ответ API по необходимости
            int statusCode = response.statusCode();
            String responseBody = response.body();

            System.out.println("Received response. Status code: " + statusCode);
            System.out.println("Response body: " + responseBody);

            // Обновляем счетчик запросов и временную метку
            requestCount++;
            lastRequestTimestamp = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace(); // Обрабатываем исключения соответственно
        } finally {
            lock.unlock();
        }
    }

    private void checkRateLimit() throws InterruptedException {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastRequestTimestamp;

        if (elapsedTime < timeIntervalMillis && requestCount >= requestLimit) {
            long sleepTime = timeIntervalMillis - elapsedTime;
            Thread.sleep(sleepTime);
            lastRequestTimestamp = System.currentTimeMillis();
            requestCount = 0; // Сбрасываем счетчик запросов после ожидания
        } else if (elapsedTime >= timeIntervalMillis) {
            lastRequestTimestamp = currentTime;
            requestCount = 0; // Сбрасываем счетчик запросов после прошествия интервала
        }
    }

    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 5);

        Document.Product product = new Document.Product();
        product.setCertificate_document("Certificate123");


        Document document = new Document();
        document.setDoc_id("123");
        document.setDoc_status("Draft");


        crptApi.createDocument(document, "signature123");
    }
}
