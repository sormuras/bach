package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.github.sormuras.bach.Bach;
import java.lang.module.ModuleDescriptor;
import java.net.http.HttpClient;
import org.junit.jupiter.api.Test;

class BachTests {
  @Test
  void defaults() {
    var bach = Bach.ofSystem();
    assertSame(System.out, bach.printStream());
    assertEquals(HttpClient.Redirect.NORMAL, bach.httpClient().followRedirects());
  }

  @Test
  void versionIsNotNull() {
    assertNotNull(Bach.version());
  }

  @Test
  void versionIsParsable() {
    var version = Bach.version();
    assertEquals(version, ModuleDescriptor.Version.parse(version).toString());
  }
}
