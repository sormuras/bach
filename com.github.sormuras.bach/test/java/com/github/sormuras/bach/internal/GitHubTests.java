package com.github.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Logbook;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GitHubTests {

  @Test
  void simplicissimus() {
    var book = new Logbook(text -> {}, System.Logger.Level.ALL);
    var bach = new Bach(book, Bach::newHttpClient);
    var hub = new GitHub(bach, "sormuras", "simplicissimus");
    assertTrue(hub.findLatestCommitHash().isPresent());

    var module = "com.github.sormuras.simplicissimus";
    assertEquals(Optional.empty(), hub.findReleasedModule(module, "0"));
    assertEquals(Optional.empty(), hub.findReleasedModule(module, "1"));
    assertEquals(Optional.empty(), hub.findReleasedModule(module, "1.3.1"));
    assertEquals(
        "https://github.com/sormuras/simplicissimus/releases/download/1.4/com.github.sormuras.simplicissimus@1.4.jar",
        hub.findReleasedModule(module, "1.4").orElseThrow());
    assertEquals(
        "https://github.com/sormuras/simplicissimus/releases/download/1.4.6/com.github.sormuras.simplicissimus@1.4.6.jar",
        hub.findReleasedModule(module, "1.4.6").orElseThrow());
    assertEquals(
        "https://github.com/sormuras/simplicissimus/releases/download/1.5/com.github.sormuras.simplicissimus@1.5.jar",
        hub.findReleasedModule(module, "1.5").orElseThrow());

    var latest = hub.findLatestReleaseTag().orElseThrow();
    assertEquals(
        "https://github.com/sormuras/simplicissimus/releases/download/"
            + latest
            + "/com.github.sormuras.simplicissimus@"
            + latest
            + ".jar",
        hub.findReleasedModule(module, latest).orElseThrow());
  }

  @ParameterizedTest
  @ValueSource(strings = {"early-access", "1-ea+2", "1-ea+1", "0"})
  void sawdust(String version) {
    var book = new Logbook(text -> {}, System.Logger.Level.ALL);
    var bach = new Bach(book, Bach::newHttpClient);
    var hub = new GitHub(bach, "sormuras", "sawdust");
    assertTrue(hub.findReleasedModule("com.github.sormuras.sawdust", version).isPresent());
    assertTrue(hub.findReleasedModule("com.github.sormuras.sawdust.api", version).isPresent());
    assertTrue(hub.findReleasedModule("com.github.sormuras.sawdust.core", version).isPresent());
  }
}
