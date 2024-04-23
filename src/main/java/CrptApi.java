import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class CrptApi {

    private final long intervalMillis;
    private final int requestLimit;
    private final AtomicInteger requestCount;
    private long lastRequestTime;
    private final HttpClient httpClient;
    private final Gson gson;


    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.intervalMillis = timeUnit.toMillis(1);
        this.requestLimit = requestLimit;
        this.requestCount = new AtomicInteger(0);
        this.lastRequestTime = System.currentTimeMillis();
        this.httpClient = HttpClients.createDefault();
        this.gson = new Gson();
    }

    public void createDocument(Object document, String signature) throws IOException {
        synchronized (this) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRequestTime >= intervalMillis) {
                requestCount.set(0);
                lastRequestTime = currentTime;
            }
            while (requestCount.get() >= requestLimit) {
                try {
                    wait(intervalMillis - (currentTime - lastRequestTime));
                    currentTime = System.currentTimeMillis();
                    if (currentTime - lastRequestTime >= intervalMillis) {
                        requestCount.set(0);
                        lastRequestTime = currentTime;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            String jsonDocument = gson.toJson(document);
            HttpPost httpPost = new HttpPost("https://ismp.crpt.ru/api/v3/lk/documents/create");
            StringEntity entity = new StringEntity(jsonDocument);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            httpClient.execute(httpPost);
            requestCount.incrementAndGet();
            notifyAll();
        }
    }

    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 3);
        Object document = new Object(); //заменить на класс который будет использоваться
        String signature = "Signature data";
        try {
            crptApi.createDocument(document, signature);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

