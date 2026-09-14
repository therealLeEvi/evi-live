package com.evi.live;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;

interface LocalTransport {
  int send(String key, String json) throws IOException;

  /** Fixed destination, no listener, redirects, system proxy or game commands. */
  final class Http implements LocalTransport {
    public int send(String key, String json) throws IOException {
      HttpURLConnection connection = (HttpURLConnection)new URL("http://127.0.0.1:51743/api/events").openConnection(Proxy.NO_PROXY);
      try {
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(1500);
        connection.setReadTimeout(2000);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + key);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(bytes.length);
        try(OutputStream stream = connection.getOutputStream()) { stream.write(bytes); }
        return connection.getResponseCode();
      } finally { connection.disconnect(); }
    }
  }
}
