package de.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

class ResourcesTests {

  final HttpClient client = HttpClient.newBuilder().followRedirects(Redirect.NORMAL).build();

  @Test
  @DisabledIfSystemProperty(named = "offline", matches = "true")
  void requestHeadOfHttpsGoogleComUsingSystemUrisIsRespondedWithStatus200() throws Exception {
    assertEquals(200, new Resources(client).head(URI.create("https://google.com"), 9).statusCode());
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

    var resources = new Resources(client);
    assertTrue(Files.isRegularFile(resources.copy(sa.toUri(), ta)));
    resources.copy(sb.toUri(), tb);
    resources.copy(sc.toUri(), tc);

    assertEquals(Files.readString(sa), resources.read(ta.toUri()));
    assertEquals(Files.readString(sb), resources.read(tb.toUri()));
    assertEquals(Files.readString(sc), resources.read(tc.toUri()));
  }
}
