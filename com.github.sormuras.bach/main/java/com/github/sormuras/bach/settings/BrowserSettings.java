package com.github.sormuras.bach.settings;

import java.net.http.HttpClient;
import java.time.Duration;

public record BrowserSettings(HttpClient.Builder httpClientBuilder) {

  public static BrowserSettings ofConnectTimeoutSeconds(int seconds) {
    var builder = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(seconds))
        .followRedirects(HttpClient.Redirect.NORMAL);
    return new BrowserSettings(builder);
  }

  public HttpClient newHttpClient() {
    return httpClientBuilder.build();
  }
}
