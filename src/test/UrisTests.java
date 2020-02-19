// default package

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

class UrisTests {
  @Test
  @DisabledIfSystemProperty(named = "offline", matches = "true")
  void requestHeadOfHttpsGoogleComUsingSystemUrisIsRespondedWithStatus200() throws Exception {
    assertEquals(200, new Bach.Util.Uris().head(URI.create("https://google.com"), 9).statusCode());
  }

  @Test
  void copyFilesUsingFileScheme(@TempDir Path temp) throws Exception {
    var source = Files.createDirectories(temp.resolve("source"));
    var sa = Files.writeString(source.resolve("a.txt"), "a");
    var sb = Files.writeString(source.resolve("b.txt"), "b");
    var sc = Files.writeString(source.resolve("c.txt"), "c");

    var target = Files.createDirectories(temp.resolve("target"));
    var ta = target.resolve("a.txt");
    var tb = target.resolve("b.txt");
    var tc = target.resolve("c.txt");

    var log = new Log();
    var uris = new Bach.Util.Uris(log, HttpClient.newBuilder().build());
    assertTrue(Files.isRegularFile(uris.copy(sa.toUri(), ta)));
    uris.copy(sb.toUri(), tb);
    uris.copy(sc.toUri(), tc);

    assertEquals(Files.readString(sa), uris.read(ta.toUri()));
    assertEquals(Files.readString(sb), uris.read(tb.toUri()));
    assertEquals(Files.readString(sc), uris.read(tc.toUri()));

    assertLinesMatch(
        List.of(
            "L Copy " + sa.toUri() + " to " + ta,
            "L Copy " + sb.toUri() + " to " + tb,
            "L Copy " + sc.toUri() + " to " + tc),
        log.lines());
  }
}
