package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.RecordComponents;
import java.io.PrintWriter;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;

public record Settings(
    LogbookSettings logbookSettings,
    FolderSettings folderSettings,
    BrowserSettings browserSettings) {

  public static Settings of() {
    return new Settings(
        LogbookSettings.ofSystem(),
        FolderSettings.ofCurrentWorkingDirectory(),
        BrowserSettings.ofConnectTimeoutSeconds(10));
  }

  public Settings with(Object component) {
    RecordComponents.of(Settings.class).findUnique(component.getClass()).orElseThrow();
    return new Settings(
        component instanceof LogbookSettings settings ? settings : logbookSettings,
        component instanceof FolderSettings settings ? settings : folderSettings,
        component instanceof BrowserSettings settings ? settings : browserSettings);
  }

  public Settings withBrowserConnectTimeout(int seconds) {
    return with(BrowserSettings.ofConnectTimeoutSeconds(seconds));
  }

  public record LogbookSettings(PrintWriter out, PrintWriter err, boolean verbose) {
    public static LogbookSettings ofSystem() {
      var out = new PrintWriter(System.out, true);
      var err = new PrintWriter(System.err, true);
      return new LogbookSettings(out, err, false);
    }
  }

  public record FolderSettings(Path root) {
    public static FolderSettings ofCurrentWorkingDirectory() {
      return new FolderSettings(Path.of(""));
    }
  }

  public record BrowserSettings(HttpClient.Builder httpClientBuilder) {
    public static BrowserSettings ofConnectTimeoutSeconds(int seconds) {
      var builder =
          HttpClient.newBuilder()
              .connectTimeout(Duration.ofSeconds(seconds))
              .followRedirects(HttpClient.Redirect.NORMAL);
      return new BrowserSettings(builder);
    }
  }
}
