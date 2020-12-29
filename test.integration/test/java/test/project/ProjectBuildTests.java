package test.project;

import static com.github.sormuras.bach.project.Feature.INCLUDE_SOURCES_IN_MODULAR_JAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.project.Feature;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.base.Classes;

class ProjectBuildTests {

  private static String release(int release) {
    return "-Dbach.project.main.release=" + release;
  }

  private static String features(Feature... features) {
    var names = Stream.of(features).map(Enum::name).toArray(String[]::new);
    return "-Dbach.project.main.features=" + String.join(",", names);
  }

  private static Set<String> requires(ModuleDescriptor descriptor) {
    return descriptor.requires().stream()
        .map(ModuleDescriptor.Requires::name)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private static Set<String> exports(ModuleDescriptor descriptor) {
    return descriptor.exports().stream()
        .map(ModuleDescriptor.Exports::source)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  @Test
  void buildSimple(@TempDir Path temp) throws Exception {
    var context = new Context("Simple", temp);
    var output = context.build(features(INCLUDE_SOURCES_IN_MODULAR_JAR));

    assertLinesMatch(
        """
        Build project Simple 0-ea
        Compile 1 main module
        >> TOOL CALLS >>
        Build took .+s
        Logbook written to %s
        """
            .formatted(context.workspace("logbook.md").toUri())
            .lines(),
        output.lines());

    var reference = context.newModuleFinder().find("simple").orElseThrow();
    var descriptor = reference.descriptor();
    assertTrue(reference.location().isPresent());
    assertEquals("simple@0-ea", descriptor.toNameAndVersion());
    assertEquals(Set.of("java.base"), requires(descriptor));
    assertFalse(descriptor.isAutomatic());
    assertFalse(descriptor.isOpen());
    assertEquals(Set.of("simple"), exports(descriptor));
    assertEquals("simple.Main", descriptor.mainClass().orElseThrow());
    assertTrue(descriptor.modifiers().isEmpty());
    assertTrue(descriptor.opens().isEmpty());
    assertEquals(Set.of("simple", "simple.internal"), descriptor.packages());
    assertTrue(descriptor.provides().isEmpty());

    var path = Path.of(reference.location().orElseThrow());
    try (var jar = new JarFile(path.toFile())) {
      assertFalse(jar.isMultiRelease(), "A multi-release JAR file?! -> " + path);
      var names = new ArrayList<String>();
      jar.entries().asIterator().forEachRemaining(e -> names.add(e.getName()));
      assertLinesMatch(
          """
          META-INF/
          META-INF/MANIFEST.MF
          module-info.class
          module-info.java
          simple/
          simple/Main.class
          simple/Main.java
          simple/Main.txt
          simple/internal/
          simple/internal/Interface.class
          simple/internal/Interface.java
          """
              .lines(),
          names.stream().sorted());
    }

    var feature = Runtime.version().feature();
    var simple = context.workspace("classes-main", "" + feature, "simple");
    assertEquals(feature, Classes.feature(simple.resolve("module-info.class")));
    assertEquals(feature, Classes.feature(simple.resolve("simple/Main.class")));
  }

  @Test
  void buildSimplicissimus(@TempDir Path temp) throws Exception {
    var context = new Context("Simplicissimus", temp);
    var output = context.build(features(INCLUDE_SOURCES_IN_MODULAR_JAR));

    assertLinesMatch(
        """
        Build project Simplicissimus 0-ea
        Compile 1 main module
        >> TOOL CALLS >>
        Build took .+s
        Logbook written to %s
        """
            .formatted(context.workspace("logbook.md").toUri())
            .lines(),
        output.lines());

    var reference = context.newModuleFinder().find("simplicissimus").orElseThrow();
    var descriptor = reference.descriptor();
    assertTrue(reference.location().isPresent());
    assertEquals("simplicissimus@0-ea", descriptor.toNameAndVersion());
    assertEquals(Set.of("java.base"), requires(descriptor));
    assertFalse(descriptor.isAutomatic());
    assertFalse(descriptor.isOpen());
    assertTrue(descriptor.exports().isEmpty());
    assertTrue(descriptor.mainClass().isEmpty());
    assertTrue(descriptor.modifiers().isEmpty());
    assertTrue(descriptor.opens().isEmpty());
    assertTrue(descriptor.packages().isEmpty());
    assertTrue(descriptor.provides().isEmpty());

    var path = Path.of(reference.location().orElseThrow());
    try (var jar = new JarFile(path.toFile())) {
      assertFalse(jar.isMultiRelease(), "A multi-release JAR file?! -> " + path);
      var names = new ArrayList<String>();
      jar.entries().asIterator().forEachRemaining(e -> names.add(e.getName()));
      assertLinesMatch(
          """
          META-INF/
          META-INF/MANIFEST.MF
          module-info.class
          module-info.java
          """
              .lines(),
          names.stream().sorted());
    }

    var feature = Runtime.version().feature();
    var simplicissimus = context.workspace("classes-main", "" + feature, "simplicissimus");
    assertEquals(feature, Classes.feature(simplicissimus.resolve("module-info.class")));
  }

  @Test
  void buildJigsawQuickStartGreetings(@TempDir Path temp) throws Exception {
    var context = new Context("JigsawQuickStartGreetings", temp);
    var output = context.build(features(INCLUDE_SOURCES_IN_MODULAR_JAR));

    assertLinesMatch(
        """
        Build project JigsawQuickStartGreetings 0-ea
        Compile 1 main module
        >> TOOL CALLS >>
        Build took .+s
        Logbook written to %s
        """
            .formatted(context.workspace("logbook.md").toUri())
            .lines(),
        output.lines());

    var reference = context.newModuleFinder().find("com.greetings").orElseThrow();
    var descriptor = reference.descriptor();
    assertTrue(reference.location().isPresent());
    assertEquals("com.greetings@0-ea", descriptor.toNameAndVersion());
    assertEquals(Set.of("java.base"), requires(descriptor));
    assertEquals(Set.of("com.greetings"), descriptor.packages());
    assertTrue(descriptor.exports().isEmpty());
    assertTrue(descriptor.mainClass().isPresent());

    var path = Path.of(reference.location().orElseThrow());
    try (var jar = new JarFile(path.toFile())) {
      assertFalse(jar.isMultiRelease(), "A multi-release JAR file?! -> " + path);
      var names = new ArrayList<String>();
      jar.entries().asIterator().forEachRemaining(e -> names.add(e.getName()));
      assertLinesMatch(
          """
          META-INF/
          META-INF/MANIFEST.MF
          com/
          com/greetings/
          com/greetings/Main.class
          com/greetings/Main.java
          module-info.class
          module-info.java
          """
              .lines(),
          names.stream().sorted());
    }
  }

  @Test
  void buildJigsawQuickStartWorld(@TempDir Path temp) throws Exception {
    var context = new Context("JigsawQuickStartWorld", temp);
    var output = context.build(features(INCLUDE_SOURCES_IN_MODULAR_JAR));

    assertLinesMatch(
        """
        Build project JigsawQuickStartWorld 0-ea
        Compile 2 main modules
        >> TOOL CALLS >>
        Build took .+s
        Logbook written to %s
        """
            .formatted(context.workspace("logbook.md").toUri())
            .lines(),
        output.lines());

    var greetings = context.newModuleFinder().find("com.greetings").orElseThrow();
    var descriptor = greetings.descriptor();
    assertTrue(greetings.location().isPresent());
    assertEquals("com.greetings@0-ea", descriptor.toNameAndVersion());
    assertEquals(Set.of("java.base", "org.astro"), requires(descriptor));
    assertEquals(Set.of("com.greetings"), descriptor.packages());
    assertTrue(descriptor.exports().isEmpty());
    assertTrue(descriptor.mainClass().isPresent());

    try (var jar = new JarFile(Path.of(greetings.location().orElseThrow()).toFile())) {
      assertFalse(jar.isMultiRelease(), "A multi-release JAR file?!");
      var names = new ArrayList<String>();
      jar.entries().asIterator().forEachRemaining(e -> names.add(e.getName()));
      assertLinesMatch(
          """
          META-INF/
          META-INF/MANIFEST.MF
          com/
          com/greetings/
          com/greetings/Main.class
          com/greetings/Main.java
          module-info.class
          module-info.java
          """
              .lines(),
          names.stream().sorted());
    }
  }

  @Test
  void buildSingleRelease7(@TempDir Path temp) throws Exception {
    var context = new Context("SingleRelease-7", temp);
    var output = context.build(release(7), features(INCLUDE_SOURCES_IN_MODULAR_JAR));

    assertLinesMatch(
        """
        Build project SingleRelease-7 0-ea
        Compile 1 main module
        >> TOOL CALLS >>
        Build took .+
        Logbook written to %s
        """
            .formatted(context.workspace("logbook.md").toUri())
            .lines(),
        output.lines());

    var foo = context.newModuleFinder().find("foo").orElseThrow();
    assertEquals("foo", foo.descriptor().name());
    var path = Path.of(foo.location().orElseThrow());
    try (var jar = new JarFile(path.toFile())) {
      assertFalse(jar.isMultiRelease(), "A multi-release JAR file?! -> " + path);
      var names = new ArrayList<String>();
      jar.entries().asIterator().forEachRemaining(e -> names.add(e.getName()));
      assertLinesMatch(
          """
          META-INF/
          META-INF/MANIFEST.MF
          foo/
          foo/Foo.class
          foo/Foo.java
          module-info.class
          module-info.java
          """
              .lines(),
          names.stream().sorted());
    }

    assertEquals(9, Classes.feature(context.workspace("classes-main/7/foo", "module-info.class")));
    assertEquals(7, Classes.feature(context.workspace("classes-main/7/foo", "foo/Foo.class")));
  }

  @Test
  void buildSingleRelease8(@TempDir Path temp) throws Exception {
    var context = new Context("SingleRelease-8", temp);
    var output = context.build(release(8), features(INCLUDE_SOURCES_IN_MODULAR_JAR));

    assertLinesMatch(
        """
        Build project SingleRelease-8 0-ea
        Compile 1 main module
        >> TOOL CALLS >>
        Build took .+
        Logbook written to %s
        """
            .formatted(context.workspace("logbook.md").toUri())
            .lines(),
        output.lines());

    var foo = context.newModuleFinder().find("foo").orElseThrow();
    assertEquals("foo", foo.descriptor().name());
    var path = Path.of(foo.location().orElseThrow());
    try (var jar = new JarFile(path.toFile())) {
      assertFalse(jar.isMultiRelease(), "A multi-release JAR file?! -> " + path);
      var names = new ArrayList<String>();
      jar.entries().asIterator().forEachRemaining(e -> names.add(e.getName()));
      assertLinesMatch(
          """
          META-INF/
          META-INF/MANIFEST.MF
          foo/
          foo/Foo.class
          foo/Foo.java
          foo/Foo.txt
          module-info.class
          module-info.java
          """
              .lines(),
          names.stream().sorted());
    }

    assertEquals(9, Classes.feature(context.workspace("classes-main/8/foo", "module-info.class")));
    assertEquals(8, Classes.feature(context.workspace("classes-main/8/foo", "foo/Foo.class")));
  }

  @Test
  void buildSingleRelease9(@TempDir Path temp) throws Exception {
    var context = new Context("SingleRelease-9", temp);
    var output = context.build(release(9), features(INCLUDE_SOURCES_IN_MODULAR_JAR));

    assertLinesMatch(
        """
        Build project SingleRelease-9 0-ea
        Compile 1 main module
        >> TOOL CALLS >>
        Build took .+
        Logbook written to %s
        """
            .formatted(context.workspace("logbook.md").toUri())
            .lines(),
        output.lines());

    var foo = context.newModuleFinder().find("foo").orElseThrow();
    assertEquals("foo", foo.descriptor().name());
    var path = Path.of(foo.location().orElseThrow());
    try (var jar = new JarFile(path.toFile())) {
      assertFalse(jar.isMultiRelease(), "A multi-release JAR file?! -> " + path);
      var names = new ArrayList<String>();
      jar.entries().asIterator().forEachRemaining(e -> names.add(e.getName()));
      assertLinesMatch(
          """
          META-INF/
          META-INF/MANIFEST.MF
          foo/
          foo/Foo.class
          foo/Foo.java
          module-info.class
          module-info.java
          """
              .lines(),
          names.stream().sorted());
    }

    assertEquals(9, Classes.feature(context.workspace("classes-main/9/foo", "module-info.class")));
    assertEquals(9, Classes.feature(context.workspace("classes-main/9/foo", "foo/Foo.class")));
  }

  @Test
  void buildMultiRelease9(@TempDir Path temp) throws Exception {
    var context = new Context("MultiRelease-9", temp);
    var output = context.build(release(9), features(INCLUDE_SOURCES_IN_MODULAR_JAR));

    assertLinesMatch(
        """
        Build project MultiRelease-9 0-ea
        Compile 1 main module
        >> TOOL CALLS >>
        Build took .+s
        Logbook written to %s
        """
            .formatted(context.workspace("logbook.md").toUri())
            .lines(),
        output.lines());

    var foo = context.newModuleFinder().find("foo").orElseThrow();
    assertEquals("foo", foo.descriptor().name());
    var path = Path.of(foo.location().orElseThrow());
    try (var jar = new JarFile(path.toFile())) {
      assertTrue(jar.isMultiRelease(), "Not a multi-release JAR file: " + path);
      var names = new ArrayList<String>();
      jar.entries().asIterator().forEachRemaining(e -> names.add(e.getName()));
      assertLinesMatch(
          """
          META-INF/
          META-INF/MANIFEST.MF
          META-INF/versions/11/
          META-INF/versions/11/foo/
          META-INF/versions/11/foo/Foo.class
          META-INF/versions/11/foo/Foo.java
          META-INF/versions/13/
          META-INF/versions/13/foo/
          META-INF/versions/13/foo/Foo.txt
          META-INF/versions/15/
          META-INF/versions/15/foo/
          META-INF/versions/15/foo/Foo.class
          META-INF/versions/15/foo/Foo.java
          foo/
          foo/Foo.class
          foo/Foo.java
          foo/Foo.txt
          module-info.class
          module-info.java
          """
              .lines(),
          names.stream().sorted());
    }

    assertEquals(9, Classes.feature(context.workspace("classes-main/9/foo", "module-info.class")));
    assertEquals(9, Classes.feature(context.workspace("classes-main/9/foo", "foo/Foo.class")));
    assertEquals(11, Classes.feature(context.workspace("classes-mr/11/foo", "foo/Foo.class")));
    assertEquals(15, Classes.feature(context.workspace("classes-mr/15/foo", "foo/Foo.class")));
  }

  @Test
  void buildMultiRelease11(@TempDir Path temp) throws Exception {
    var context = new Context("MultiRelease-11", temp);
    var output = context.build(release(11), features(INCLUDE_SOURCES_IN_MODULAR_JAR));

    assertLinesMatch(
        """
        Build project MultiRelease-11 0-ea
        Compile 1 main module
        >> TOOL CALLS >>
        Build took .+s
        Logbook written to %s
        """
            .formatted(context.workspace("logbook.md").toUri())
            .lines(),
        output.lines());

    var foo = context.newModuleFinder().find("foo").orElseThrow();
    assertEquals("foo", foo.descriptor().name());
    var path = Path.of(foo.location().orElseThrow());
    try (var jar = new JarFile(path.toFile())) {
      assertTrue(jar.isMultiRelease(), "Not a multi-release JAR file: " + path);
      var names = new ArrayList<String>();
      jar.entries().asIterator().forEachRemaining(e -> names.add(e.getName()));
      assertLinesMatch(
          """
          META-INF/
          META-INF/MANIFEST.MF
          META-INF/versions/12/
          META-INF/versions/12/foo/
          META-INF/versions/12/foo/Foo.class
          META-INF/versions/12/foo/Foo.java
          META-INF/versions/13/
          META-INF/versions/13/foo/
          META-INF/versions/13/foo/Foo.class
          META-INF/versions/13/foo/Foo.java
          META-INF/versions/14/
          META-INF/versions/14/foo/
          META-INF/versions/14/foo/Foo.class
          META-INF/versions/14/foo/Foo.java
          META-INF/versions/15/
          META-INF/versions/15/foo/
          META-INF/versions/15/foo/Foo.class
          META-INF/versions/15/foo/Foo.java
          META-INF/versions/16/
          META-INF/versions/16/foo/
          META-INF/versions/16/foo/Foo.class
          META-INF/versions/16/foo/Foo.java
          foo/
          foo/Foo.class
          foo/Foo.java
          module-info.class
          module-info.java
          """
              .lines(),
          names.stream().sorted());
    }

    assertEquals(
        11, Classes.feature(context.workspace("classes-main/11/foo", "module-info.class")));
    assertEquals(11, Classes.feature(context.workspace("classes-main/11/foo", "foo/Foo.class")));
    assertEquals(12, Classes.feature(context.workspace("classes-mr/12/foo", "foo/Foo.class")));
    assertEquals(13, Classes.feature(context.workspace("classes-mr/13/foo", "foo/Foo.class")));
    assertEquals(14, Classes.feature(context.workspace("classes-mr/14/foo", "foo/Foo.class")));
    assertEquals(15, Classes.feature(context.workspace("classes-mr/15/foo", "foo/Foo.class")));
    assertEquals(16, Classes.feature(context.workspace("classes-mr/16/foo", "foo/Foo.class")));
  }

  @Test
  void buildModuleLookup(@TempDir Path temp) throws Exception {
    var context = new Context("ModuleLookup", temp);
    var foo = context.base.resolve(Bach.EXTERNALS).resolve("foo.jar");
    var zip = context.base.resolve("module-foo.zip");

    Files.deleteIfExists(foo);
    var output = context.build();
    assertTrue(Files.exists(foo), "File not found: " + foo);

    assertLinesMatch(
        """
        Build project ModuleLookup 0-ea
        Load required and missing external modules
          %s << %s
        Compile 1 main module
        >> TOOL CALLS >>
        Build took .+s
        Logbook written to %s
        """
            .formatted(
                context.base.relativize(foo),
                zip.toUri(),
                context.workspace("logbook.md").toUri()
            )
            .lines(),
        output.lines());
  }
}
