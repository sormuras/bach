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
  void relativeUriThrows() {
    var test = new TestRun();
    var downloader = new Downloader(test, Path.of(""));
    var e = assertThrows(Exception.class, () -> downloader.download(URI.create("void")));
    assertTrue(e.getMessage().contains("URI is not absolute"));
  }

//  @Test
//  void https(@TempDir Path temp) throws Exception {
//    var test = new TestRun();
//    var downloader = new Downloader(test, temp);
//    var uri = URI.create("https://junit.org/junit5/index.html");
//    var path = downloader.download(uri);
//    var text = Files.readString(path);
//    assertTrue(text.contains("<title>JUnit 5</title>"));
//    assertLinesMatch(
//        List.of(
//            "download(https://junit.org/junit5/index.html)",
//                "Remote was modified on .+",
//                "Local target file is " + path.toUri(),
//                "Transferring index.html...",
//                "Downloaded index.html, .+ bytes."
//        ),
//        test.outLines());
//    System.out.println(text);
//  }

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
    // run.offline = true
    // test.log(System.Logger.Level.TRACE, "[4]");
    // var forth = downloader.download(uri);
    // assertEquals(first, forth);
    // test.log(System.Logger.Level.TRACE, "[5]");
    // Files.delete(first);
    // var e = assertThrows(Exception.class, () -> downloader.download(uri));
    // assertEquals("Target is missing and being offline: " + first, e.getMessage());
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
            "Downloaded LICENSE-2.0.txt, 11358 bytes."
            // "[4]",
            // "download(" + uri + ")",
            // "...",
            // "Offline mode is active and target already exists.",
            // "[5]",
            // "download(" + uri + ")"
            ),
        test.outLines());
  }

//  @Test
//  void defaultFileSystem(@TempDir Path tempRoot) throws Exception {
//    var content = List.of("Lorem", "ipsum", "dolor", "sit", "amet");
//    var tempFile = Files.createFile(tempRoot.resolve("source.txt"));
//    Files.write(tempFile, content);
//    var tempPath = Files.createDirectory(tempRoot.resolve("target"));
//    var name = tempFile.getFileName().toString();
//    var actual = tempPath.resolve(name);
//
//    // initial download
//    bach.utilities.download(tempPath, tempFile.toUri());
//    assertTrue(Files.exists(actual));
//    assertLinesMatch(content, Files.readAllLines(actual));
//    assertLinesMatch(
//        List.of(
//            "Downloading " + tempFile.toUri() + "...",
//            "Transferring " + tempFile.toUri() + "...",
//            "Downloaded source.txt successfully.",
//            " o Size -> .. bytes", // 32 on Windows, 27 on Linux/Mac
//            " o Last Modified .+"),
//        logger.getLines());
//
//    // reload
//    logger.clear();
//    bach.utilities.download(tempPath, tempFile.toUri());
//    assertLinesMatch(
//        List.of(
//            "Downloading " + tempFile.toUri() + "...",
//            "Local file exists. Comparing attributes to remote file...",
//            "Local and remote file attributes seem to match."),
//        logger.getLines());
//
//    // offline mode
//    logger.clear();
//    bach.var.offline = true;
//    bach.utilities.download(tempPath, tempFile.toUri());
//    assertLinesMatch(
//        List.of(
//            "Downloading " + tempFile.toUri() + "...",
//            "Offline mode is active and target already exists."),
//        logger.getLines());
//
//    // offline mode with error
//    logger.clear();
//    Files.delete(actual);
//    var e =
//        assertThrows(Exception.class, () -> bach.utilities.download(tempPath, tempFile.toUri()));
//    assertEquals("Target is missing and being offline: " + actual, e.getMessage());
//    assertLinesMatch(List.of("Downloading " + tempFile.toUri() + "..."), logger.getLines());
//    // online but different file
//    logger.clear();
//    bach.var.offline = false;
//    Files.write(actual, List.of("Hello world!"));
//    bach.utilities.download(tempPath, tempFile.toUri());
//    assertLinesMatch(content, Files.readAllLines(actual));
//    assertLinesMatch(
//        List.of(
//            "Downloading " + tempFile.toUri() + "...",
//            "Local file exists. Comparing attributes to remote file...",
//            "Local file differs from remote -- replacing it...",
//            "Transferring " + tempFile.toUri() + "...",
//            "Downloaded source.txt successfully.",
//            " o Size -> .. bytes", // 32 on Windows, 27 on Linux/Mac
//            " o Last Modified .+"),
//        logger.getLines());
//  }
}
