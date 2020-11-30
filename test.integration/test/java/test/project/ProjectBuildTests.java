package test.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.base.Classes;

class ProjectBuildTests {

  private static Set<String> requires(ModuleDescriptor descriptor) {
    return descriptor.requires().stream()
        .map(ModuleDescriptor.Requires::name)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  @Test
  void buildSimplicissimus(@TempDir Path temp) throws Exception {
    var context = new Context("Simplicissimus", temp);
    var output = context.build();

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
    var output = context.build();

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
    var output = context.build();

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

    var finder = context.newModuleFinder();
    assertTrue(finder.find("com.greetings").isPresent());
    assertTrue(finder.find("org.astro").isPresent());
  }

  @Test
  void buildSingleRelease7(@TempDir Path temp) throws Exception {
    var context = new Context("SingleRelease-7", temp);
    var output = context.build("-Dbach.project.main.release=7");

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
    var output = context.build("-Dbach.project.main.release=8");

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
    var output = context.build("-Dbach.project.main.release=9");

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
    var output = context.build("-Dbach.project.main.release=9");

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
    var output = context.build("-Dbach.project.main.release=11");

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
}
