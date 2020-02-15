// default package

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.io.File;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
        private final boolean b = true;
        private final char c = 'c';
        private final int[] integers = {1, 2, 3};
        private final String[] strings = {"s", "t"};

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
      }

      assertLinesMatch(
          List.of(
              "Record",
              "  b = true",
              "  c = c",
              "  integers = [1, 2, 3]",
              "  strings = [\"s\", \"t\"]"),
          new Record().print());
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
    void moduleSourcePathWithModuleNameAtTheEnd() {
      var actual = Bach.Util.Modules.moduleSourcePath(Path.of("src/main/a.b.c"), "a.b.c");
      assertEquals(Path.of("src/main").toString(), actual);
    }

    @Test
    void moduleSourcePathWithNestedModuleName() {
      var actual = Bach.Util.Modules.moduleSourcePath(Path.of("src/a.b.c/main/java"), "a.b.c");
      assertEquals(String.join(File.separator, "src", "*", "main", "java"), actual);
    }
  }
}
