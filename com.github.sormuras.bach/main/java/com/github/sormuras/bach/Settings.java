package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.RecordComponents;
import java.io.PrintWriter;
import java.net.http.HttpClient;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;

public interface Settings {

  static NewSettings newSettings() {
    return new NewSettings(
        LogbookSettings.ofSystem(),
        FolderSettings.ofCurrentWorkingDirectory(),
        BrowserSettings.ofConnectTimeoutSeconds(10),
        SourceSettings.of(StandardCharsets.UTF_8));
  }

  LogbookSettings logbookSettings();

  FolderSettings folderSettings();

  BrowserSettings browserSettings();

  SourceSettings sourceSettings();

  record LogbookSettings(PrintWriter out, PrintWriter err, boolean verbose) {
    public static LogbookSettings ofSystem() {
      var out = new PrintWriter(System.out, true);
      var err = new PrintWriter(System.err, true);
      return new LogbookSettings(out, err, false);
    }
  }

  record FolderSettings(Path root) {
    public static FolderSettings ofCurrentWorkingDirectory() {
      return new FolderSettings(Path.of(""));
    }
  }

  record BrowserSettings(HttpClient.Builder httpClientBuilder) {
    public static BrowserSettings ofConnectTimeoutSeconds(int seconds) {
      var builder =
          HttpClient.newBuilder()
              .connectTimeout(Duration.ofSeconds(seconds))
              .followRedirects(HttpClient.Redirect.NORMAL);
      return new BrowserSettings(builder);
    }
  }

  record SourceSettings(Charset encoding) {
    public static SourceSettings of(Charset encoding) {
      return new SourceSettings(encoding);
    }
  }

  record NewSettings(
      LogbookSettings logbookSettings,
      FolderSettings folderSettings,
      BrowserSettings browserSettings,
      SourceSettings sourceSettings)
      implements Settings {

    public NewSettings with(Object component) {
      RecordComponents.of(NewSettings.class).findUnique(component.getClass()).orElseThrow();
      return new NewSettings(
          component instanceof LogbookSettings settings ? settings : logbookSettings,
          component instanceof FolderSettings settings ? settings : folderSettings,
          component instanceof BrowserSettings settings ? settings : browserSettings,
          component instanceof SourceSettings settings ? settings : sourceSettings);
    }

    public NewSettings withSourceEncoding(Charset encoding) {
      return with(SourceSettings.of(encoding));
    }

    public NewSettings withBrowserConnectTimeout(int seconds) {
      return with(BrowserSettings.ofConnectTimeoutSeconds(seconds));
    }
  }
}
