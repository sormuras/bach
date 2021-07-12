package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.RecordComponents;
import java.io.PrintWriter;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

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

  public Settings with(Options options) {
    var settings = options.settings();
    return with(settings.verbose(), this::withVerbose)
        .with(settings.timeout(), this::withBrowserConnectTimeout);
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private <T> Settings with(Optional<T> option, Function<T, Settings> with) {
    return option.isEmpty() ? this : with.apply(option.get());
  }

  public Settings withVerbose(boolean verbose) {
    return with(logbookSettings.withVerbose(verbose));
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

    public LogbookSettings withVerbose(boolean verbose) {
      return new LogbookSettings(out, err, verbose);
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
