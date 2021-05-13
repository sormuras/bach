package test.integration.trait;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.api.ExternalModuleLocation;
import java.lang.module.FindException;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import test.base.resource.ResourceManager;
import test.base.resource.ResourceManager.Singleton;
import test.base.resource.TempDir;
import test.base.resource.WebServer;
import test.integration.Auxiliary;

@ExtendWith(ResourceManager.class)
class ResolveTraitTests {

  @Test
  void loadFooFails() {
    var bach = Auxiliary.newEmptyBach();
    assertThrows(FindException.class, () -> bach.loadExternalModules("foo"));
  }

  @Test
  void loadFoo(@Singleton(VolatileServer.class) WebServer server, @TempDir Path temp) {
    var foo = new ExternalModuleLocation("foo", server.uri("foo.jar").toString());
    var bach =
        Bach.of(
            Logbook.ofErrorPrinter(),
            Options.of()
                .with("chroot", Optional.of(temp))
                .with("externalModuleLocations", List.of(foo)));

    bach.loadExternalModules("foo");

    var finder = ModuleFinder.of(bach.project().folders().externals());
    assertTrue(finder.find("foo").isPresent());
  }

  @Test
  void loadMissingExternalModules(@Singleton(VolatileServer.class) WebServer server, @TempDir Path temp) {
    var bar = new ExternalModuleLocation("bar", server.uri("bar.jar").toString());
    var foo = new ExternalModuleLocation("foo", server.uri("foo.jar").toString());
    var bach =
        Bach.of(
            Logbook.ofErrorPrinter(),
            Options.of()
                .with("chroot", Optional.of(temp))
                .with("projectRequires", List.of("bar")) // bar requires foo
                .with("externalModuleLocations", List.of(bar, foo)));

    bach.loadMissingExternalModules();

    var finder = ModuleFinder.of(bach.project().folders().externals());
    assertTrue(finder.find("bar").isPresent());
    assertTrue(finder.find("foo").isPresent());
  }
}
