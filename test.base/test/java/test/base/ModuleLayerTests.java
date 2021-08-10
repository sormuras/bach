package test.base;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModuleLayerTests {

  @Test
  void findsOnlyFooToolProvider(@TempDir Path temp) throws Exception {
    Files.createDirectories(temp.resolve("src/foo/foo"));
    Files.writeString(
        temp.resolve("src/foo/module-info.java"),
        """
        module foo {
          provides java.util.spi.ToolProvider with foo.FooToolProvider;
        }
        """);
    Files.writeString(
        temp.resolve("src/foo/foo/FooToolProvider.java"),
        """
        package foo;
        public record FooToolProvider() implements java.util.spi.ToolProvider {
          public String name() { return "foo"; }
          public int run(java.io.PrintWriter out, java.io.PrintWriter err, String... args) { return 0; }
        }
        """);
    ToolProviders.run(
        "javac",
        "--module-source-path",
        temp.resolve("src"),
        "--module",
        "foo",
        "-d",
        temp.resolve("classes"));
    ToolProviders.run(
        "jar",
        "--create",
        "--file",
        temp.resolve("foo.jar"),
        "-C",
        temp.resolve("classes/foo"),
        ".");

    try {
      assertLinesMatch(List.of("foo"), listNamesOfToolProviders(temp));
    } finally {
      System.gc(); // Windows...
    }
  }

  private List<String> listNamesOfToolProviders(Path temp) {
    var before = ModuleFinder.of(temp);
    var after = ModuleFinder.of();
    var parentLayer = ModuleLayer.boot();
    var configuration = parentLayer.configuration().resolveAndBind(before, after, Set.of());
    var parentLoader = ClassLoader.getSystemClassLoader();
    var layer = parentLayer.defineModulesWithOneLoader(configuration, parentLoader);
    return
        ServiceLoader.load(layer, ToolProvider.class).stream()
            .filter(provider -> provider.type().getModule().getLayer() == layer)
            .map(ServiceLoader.Provider::get)
            .map(ToolProvider::name)
            .sorted()
            .toList();
  }
}
