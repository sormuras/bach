package test.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModuleFinderTests {

  @Test
  void findsModuleOnlyIfItsArchiveExistedOnCreationTime(@TempDir Path temp) throws Exception {
    var emptyFinder = ModuleFinder.of(temp);
    assertEquals(Set.of(), findAllModuleNames(emptyFinder));
    buildModule(temp, "foo");
    assertEquals(Set.of(), findAllModuleNames(emptyFinder));
    assertEquals(Set.of("foo"), findAllModuleNames(ModuleFinder.of(temp))); // <- new finder
  }

  static void buildModule(Path temp, String name, String... directives) throws Exception {
    var javac = ToolProvider.findFirst("javac").orElseThrow();
    var jar = ToolProvider.findFirst("jar").orElseThrow();
    var z = new PrintWriter(OutputStream.nullOutputStream());
    var lines = new ArrayList<String>();
    lines.add("module " + name + " {");
    lines.addAll(List.of(directives));
    lines.add("}");
    var file = Files.write(temp.resolve("module-info.java"), lines);
    javac.run(z, z, file.toString());
    var foo = temp.resolve(name + ".jar").toString();
    jar.run(z, z, "--create", "--file", foo, temp + "/module-info.class");
  }

  static Set<String> findAllModuleNames(ModuleFinder finder) {
    return finder.findAll().stream()
        .map(ModuleReference::descriptor)
        .map(ModuleDescriptor::name)
        .collect(Collectors.toCollection(TreeSet::new));
  }
}
