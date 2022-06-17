package dev.anthonyashco.jirautilities;

import dev.anthonyashco.exceptions.HTTPException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class UrlUtil {
    private final String urlHost;
    private final String authKey;

    public UrlUtil(String urlHost, String authKey) {
        this.urlHost = urlHost;
        this.authKey = authKey;
    }

    private HttpURLConnection getConn(String urlPath) throws IOException {
        URL url = new URL(urlHost + urlPath);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Basic " + authKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-Atlassian-Token", "no-check");
        conn.setRequestProperty("Accept", "application/json");
        return conn;
    }

    public JsonObject getJson(String urlPath) throws IOException, HTTPException {
        HttpURLConnection conn = getConn(urlPath);
        conn.connect();
        int status = conn.getResponseCode();
        switch (status) {
            case 200:
            case 201:
                try (BufferedReader bfr = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    return JsonParser.parseReader(bfr).getAsJsonObject();
                }
            default:
                throw new HTTPException(String.format("Unexpected response %s received.", status));
        }
    }

    public JsonObject putJson(String urlPath, JsonObject payload) throws IOException, HTTPException {
        HttpURLConnection conn = getConn(urlPath);
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }
        int status = conn.getResponseCode();
        switch (status) {
            case 200:
            case 201:
                try (BufferedReader bfr = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    return JsonParser.parseReader(bfr).getAsJsonObject();
                }
            default:
                try (BufferedReader bfr = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    JsonObject json = JsonParser.parseReader(bfr).getAsJsonObject();
                    System.out.println(json.toString());
                }
                throw new HTTPException(String.format("Unexpected response %s received.", status));
        }
    }

    public JsonObject postJson(String urlPath, JsonObject payload) throws IOException, HTTPException {
        HttpURLConnection conn = getConn(urlPath);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }
        int status = conn.getResponseCode();
        switch (status) {
            case 200:
            case 201:
                try (BufferedReader bfr = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    return JsonParser.parseReader(bfr).getAsJsonObject();
                }
            default:
                try (BufferedReader bfr = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    JsonObject json = JsonParser.parseReader(bfr).getAsJsonObject();
                    System.out.println(json.toString());
                }
                throw new HTTPException(String.format("Unexpected response %s received.", status));
        }
    }

    public JsonObject postFile(String urlPath, File file, String fileMimeType) throws HTTPException, IOException {
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), RequestBody.create(file, MediaType.parse(fileMimeType)))
                .build();
        Request request = new Request.Builder()
                .addHeader("Authorization", "Basic " + authKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Atlassian-Token", "no-check")
                .addHeader("Accept", "application/json")
                .url(urlHost + urlPath).post(requestBody).build();
        Response response = client.newCall(request).execute();

        int status = response.code();
        switch (status) {
            case 200:
            case 201:
                return JsonParser.parseString(response.body().string()).getAsJsonObject();
            default:
                System.out.println(response.body().string());
                throw new HTTPException(String.format("Unexpected response %s received.", status));
        }
    }
}
