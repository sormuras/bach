// default package

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UtilTests {

  @Nested
  class Args {

    @Test
    void empty() {
      var args = new Bach.Util.Args();
      assertLinesMatch(List.of(), args.list());
      assertEquals("Args{}", args.toString());
      assertArrayEquals(new String[0], args.toStrings());
    }

    @Test
    void touch() {
      var args =
          new Bach.Util.Args()
              .add(1)
              .add("key", "value")
              .add(true, "first")
              .add(true, "second", "more")
              .add(false, "suppressed")
              .forEach(List.of('a', 'b', 'c'), Bach.Util.Args::add);
      assertLinesMatch(
          List.of("1", "key", "value", "first", "second", "more", "a", "b", "c"), args.list());
    }
  }

  @Nested
  class Printable {

    @Test
    void object() {
      assertLinesMatch(List.of("Object"), Bach.Util.Printable.print(new Object()));
    }

    @Test
    void record() {

      @SuppressWarnings({"unused", "FieldCanBeLocal"})
      class Record implements Bach.Util.Printable {
        private final boolean active;
        private final boolean b = true;
        private final char c = 'c';
        private final int[] integers = {1, 2, 3};
        private final String[] strings = {"s", "t"};

        Record(boolean active) {
          this.active = active;
        }

        public boolean b() {
          return b;
        }

        public char c() {
          return c;
        }

        public int[] integers() {
          return integers;
        }

        public String[] strings() {
          return strings;
        }

        @Override
        public boolean printTest(String name, Object value) {
          return active;
        }
      }

      assertLinesMatch(List.of("Record"), new Record(false).print());
      assertLinesMatch(
          List.of(
              "Record",
              "  b = true",
              "  c = c",
              "  integers = [1, 2, 3]",
              "  strings = [\"s\", \"t\"]"),
          new Record(true).print());
    }
  }

  @Nested
  class Modules {
    @Test
    void originOfJavaLangObjectIsJavaBase() {
      var expected = Object.class.getModule().getDescriptor().toNameAndVersion();
      assertEquals(expected, Bach.Util.Modules.origin(new Object()));
    }

    @Test
    void originOfThisObjectIsTheLocationOfTheClassFileContainer() {
      var expected = getClass().getProtectionDomain().getCodeSource().getLocation().toString();
      assertEquals(expected, Bach.Util.Modules.origin(this));
    }

    private ModuleDescriptor describe(String source) {
      return Bach.Util.Modules.newModule(source).build();
    }

    @Test
    void minimalisticModuleDeclaration() {
      var actual = describe("module a{}");
      assertEquals(ModuleDescriptor.newModule("a").build(), actual);
    }

    @Test
    void moduleDeclarationWithRequires() {
      var actual = describe("module a{requires b;}");
      assertEquals(ModuleDescriptor.newModule("a").requires("b").build(), actual);
    }

    @Test
    void moduleDeclarationWithRequiresAndVersion() {
      var actual = describe("module a{requires b/*1.2*/;}");
      assertEquals(
          ModuleDescriptor.newModule("a").requires(Set.of(), "b", Version.parse("1.2")).build(),
          actual);
    }

    @Test
    void moduleDeclarationWithComments() {
      var actual = describe("open /*test*/ module a /*extends a*/ {}");
      assertEquals(ModuleDescriptor.newModule("a").build(), actual);
    }

    @Test
    void moduleSourcePathWithModuleNameAtTheBeginning() {
      var actual = moduleSourcePath(Path.of("a.b.c/module-info.java"), "a.b.c");
      assertEquals(Path.of(".").toString(), actual);
    }

    @Test
    void moduleSourcePathWithModuleNameAtTheBeginningWithOffset() {
      var actual = moduleSourcePath(Path.of("a.b.c/offset/module-info.java"), "a.b.c");
      assertEquals(Path.of(".", "offset").toString(), actual);
    }

    @Test
    void moduleSourcePathWithModuleNameAtTheEnd() {
      var actual = moduleSourcePath(Path.of("src/main/a.b.c/module-info.java"), "a.b.c");
      assertEquals(Path.of("src/main").toString(), actual);
    }

    @Test
    void moduleSourcePathWithNestedModuleName() {
      var actual = moduleSourcePath(Path.of("src/a.b.c/main/java/module-info.java"), "a.b.c");
      assertEquals(String.join(File.separator, "src", "*", "main", "java"), actual);
    }

    @Test
    void moduleSourcePathWithNonUniqueModuleNameInPath() {
      var path = Path.of("a/a/module-info.java");
      assertThrows(IllegalArgumentException.class, () -> moduleSourcePath(path, "a"));
    }

    @Test
    void moduleSourcePathWithoutModuleNameInPath() {
      var path = Path.of("a/a/module-info.java");
      assertThrows(IllegalArgumentException.class, () -> moduleSourcePath(path, "b"));
    }

    /** Compute module's source path. */
    String moduleSourcePath(Path info, String module) {
      return Bach.Util.Modules.moduleSourcePath(info, module);
    }
  }

  @Nested
  class Surveys {

    @Test
    void ofConstructor() {
      var declared = Set.of("a", "b");
      Map<String, Set<Version>> requires = Map.of("a", Set.of(), "c", Set.of(Version.parse("2")));
      assertABC(new Bach.Util.Modules.Survey(declared, requires));
    }

    @Test
    void ofModuleInfoSourceStrings() {
      var a = "module a {}";
      var b = "module b { requires a; requires c/*2*/; }";
      assertABC(Bach.Util.Modules.Survey.of(a, b));
    }

    @Test
    void ofModuleInfoSourceFiles(@TempDir Path temp) throws Exception {
      var a = declare(temp, "a", "module a {}");
      var b = declare(temp, "b", "module b { requires a; requires c /*2*/;}");
      assertABC(Bach.Util.Modules.Survey.of(Set.of(a, b)));
    }

    @Test
    void ofModuleFinder(@TempDir Path temp) throws Exception {
      var a = declare(temp, "a", "module a {}");
      var b = declare(temp, "b", "module b { requires a; requires c;}");
      var c = declare(temp, "c", "module c {}");
      var javac = ToolProvider.findFirst("javac").orElseThrow();
      javac.run(System.out, System.err, a.toString());
      javac.run(System.out, System.err, "--module-version", "2", c.toString()); // c@2
      javac.run(System.out, System.err, "--module-path", temp.toString(), b.toString());
      Files.delete(temp.resolve("c/module-info.class")); // Make module "c" magically disappear...
      assertABC(Bach.Util.Modules.Survey.of(ModuleFinder.of(temp)));
    }

    @Test
    void ofSystem() {
      var system = Bach.Util.Modules.Survey.of(ModuleFinder.ofSystem());
      assertTrue(system.declaredModules().contains("java.base"));
      assertFalse(system.requiredModules().contains("java.base")); // mandated are ignored
      assertTrue(system.declaredModules().size() > system.requiredModules().size());
    }

    private Path declare(Path path, String name, String source) throws Exception {
      var directory = Files.createDirectory(path.resolve(name));
      return Files.writeString(directory.resolve("module-info.java"), source);
    }

    private void assertABC(Bach.Util.Modules.Survey survey) {
      assertEquals(Set.of("a", "b"), survey.declaredModules());
      assertEquals(Set.of("a", "c"), survey.requiredModules());
      assertEquals(Optional.empty(), survey.requiredVersion("a"));
      assertEquals("2", survey.requiredVersion("c").orElseThrow().toString());
      var expected = Bach.Util.Modules.UnmappedModuleException.class;
      assertThrows(expected, () -> survey.requiredVersion("b"));
      assertThrows(expected, () -> survey.requiredVersion("x"));
    }
  }
}
