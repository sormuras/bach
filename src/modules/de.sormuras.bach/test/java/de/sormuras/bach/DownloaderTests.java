package de.sormuras.bach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisabledIfSystemProperty(named = "bach.offline", matches = "true")
class DownloaderTests {

  @ParameterizedTest
  @ValueSource(strings = {"a.b", "a.b#c", "a.b?c", "https://host/path/../a.b#c?d=e&more=b.a"})
  void extractFileName(String uri) {
    assertEquals("a.b", Downloader.extractFileName(URI.create(uri)));
  }

  @Test
  void extractFileNameFromContentDispositionHeaderField() throws Exception {
    var uri = URI.create("https://github.com/sormuras/bach/archive/master.zip");
    var connection = uri.toURL().openConnection();
    assertEquals("master.zip", Downloader.extractFileName(uri));
    assertEquals("bach-master.zip", Downloader.extractFileName(connection));
  }

  @Test
  void relativeUriThrows() {
    var test = new TestRun();
    var downloader = new Downloader(test, Path.of(""));
    var e = assertThrows(Exception.class, () -> downloader.download(URI.create("void")));
    assertTrue(e.getMessage().contains("URI is not absolute"));
  }

  @DisabledIfSystemProperty(named = "bach.offline", matches = "true")
  @ParameterizedTest
  @ValueSource(strings = {"http", "https"})
  void downloadLicenseFromApacheOrg(String protocol, @TempDir Path temp) throws Exception {
    var test = new TestRun();
    var downloader = new Downloader(test, temp);
    var uri = URI.create(protocol + "://www.apache.org/licenses/LICENSE-2.0.txt");

    test.log(System.Logger.Level.TRACE, "[1]");
    var first = downloader.download(uri);
    assertTrue(Files.readString(first).contains("Apache License"));

    test.log(System.Logger.Level.TRACE, "[2]");
    var second = downloader.download(uri);
    assertEquals(first, second);
    Files.writeString(first, "Lorem ipsum...");
    assertFalse(Files.readString(first).contains("Apache License"));

    test.log(System.Logger.Level.TRACE, "[3]");
    var third = downloader.download(uri);
    assertEquals(first, third);

    test.log(System.Logger.Level.TRACE, "[4]");
    test.setOffline(true);
    var forth = downloader.download(uri);
    assertEquals(first, forth);

    test.log(System.Logger.Level.TRACE, "[5]");
    Files.delete(first);
    var e = assertThrows(Exception.class, () -> downloader.download(uri));
    assertEquals("Offline mode is active and target is missing: " + first, e.getMessage());

    assertLinesMatch(
        List.of(
            "[1]",
            "download(" + uri + ")",
            ">> TRANSFER >>",
            "Downloaded LICENSE-2.0.txt, 11358 bytes.",
            "[2]",
            "download(" + uri + ")",
            ">> TIMESTAMP COMPARISON >>",
            "Timestamp match: LICENSE-2.0.txt, 11358 bytes.",
            "[3]",
            "download(" + uri + ")",
            ">> TIMESTAMP COMPARISON >>",
            "Local target file differs from remote source -- replacing it...",
            ">> TRANSFER >>",
            "Downloaded LICENSE-2.0.txt, 11358 bytes.",
            "[4]",
            "download(" + uri + ")",
            "Offline mode is active!",
            "Target already exists: LICENSE-2.0.txt, 11358 bytes.",
            "[5]",
            "download(" + uri + ")",
            "Offline mode is active!"),
        test.outLines());

    assertLinesMatch(
        List.of("Offline mode is active and target is missing: " + first), test.errLines());
  }
}
