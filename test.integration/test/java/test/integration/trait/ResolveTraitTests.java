package test.integration.trait;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Settings;
import com.github.sormuras.bach.api.CodeSpaceMain;
import com.github.sormuras.bach.api.CodeSpaceTest;
import com.github.sormuras.bach.api.ExternalModuleLocation;
import com.github.sormuras.bach.api.Externals;
import com.github.sormuras.bach.api.Folders;
import com.github.sormuras.bach.api.Project;
import com.github.sormuras.bach.api.Spaces;
import com.github.sormuras.bach.api.Tools;
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import test.base.resource.ResourceManager;
import test.base.resource.ResourceManager.Singleton;
import test.base.resource.TempDir;
import test.base.resource.WebServer;
import test.integration.Auxiliary;
import test.integration.VolatileServer;

@ExtendWith(ResourceManager.class)
class ResolveTraitTests {

  @Test
  void loadFooFails() {
    var bach = Auxiliary.newEmptyBach();
    assertThrows(FindException.class, () -> bach.loadExternalModules("foo"));
  }

  @Test
  void loadFoo(@Singleton(VolatileServer.class) WebServer server, @TempDir Path temp) {
    var project =
        new Project(
            "LoadFoo",
            ModuleDescriptor.Version.parse("99"),
            Folders.of(temp),
            Spaces.of(CodeSpaceMain.empty(), CodeSpaceTest.empty()),
            Tools.of(),
            new Externals(
                Set.of(),
                List.of(new ExternalModuleLocation("foo", server.uri("foo.jar").toString()))));
    var bach = new Bach(Settings.of(Options.ofDefaultValues(), Logbook.ofErrorPrinter()), project);

    bach.loadExternalModules("foo");

    var finder = ModuleFinder.of(project.folders().externalModules());
    assertTrue(finder.find("foo").isPresent());
  }

  @Test
  void loadMissingExternalModules(
      @Singleton(VolatileServer.class) WebServer server, @TempDir Path temp) {
    var project =
        new Project(
            "LoadFoo",
            ModuleDescriptor.Version.parse("99"),
            Folders.of(temp),
            Spaces.of(CodeSpaceMain.empty(), CodeSpaceTest.empty()),
            Tools.of(),
            new Externals(
                Set.of("bar"), // module bar { requires foo; }
                List.of(
                    new ExternalModuleLocation("bar", server.uri("bar.jar").toString()),
                    new ExternalModuleLocation("foo", server.uri("foo.jar").toString()))));
    var bach = new Bach(Settings.of(Options.ofDefaultValues(), Logbook.ofErrorPrinter()), project);

    bach.loadMissingExternalModules();

    var finder = ModuleFinder.of(bach.project().folders().externalModules());
    assertTrue(finder.find("bar").isPresent());
    assertTrue(finder.find("foo").isPresent());
  }
}
