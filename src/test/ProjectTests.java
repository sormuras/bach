// default package

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProjectTests {

  @Nested
  class CustomProject {

    final Bach.Project project =
        new Bach.Project.Builder("custom")
            .paths(Path.of("custom"))
            .version("1.2-C")
            // .requires("java.base", "11")
            .requires("foo", "4711")
            .requires("bar", "1701")
            .units(List.of())
            .realms(List.of())
            .mapper(this::customModuleMapper)
            .build();

    Bach.Project.ModuleMapper.Mapping customModuleMapper(String module, Version version) {
      switch (module) {
        case "bar":
          return new Bach.Project.ModuleMapper.Mapping(module, version, URI.create("bar-123.jar"));
        case "foo":
          return new Bach.Project.ModuleMapper.Mapping(module, version, URI.create("foo-4.5.jar"));
      }
      return null;
    }

    @Test
    void print() {
      assertLinesMatch(
          List.of(
              "Project",
              "  descriptor = module { name: custom@1.2-C, requires: [synthetic bar (@1701), synthetic foo (@4711), mandated java.base] }",
              "  library -> instance of Bach$Project$Library",
              "  Library",
              "    mapper = \\QProjectTests$CustomProject$$Lambda$\\E.+",
              "  paths -> instance of Bach$Project$Paths",
              "  Paths",
              "    base = custom",
              "    lib = " + Path.of("custom/lib"),
              "    out = " + Path.of("custom/.bach"),
              "  structure -> instance of Bach$Project$Structure",
              "  Structure",
              "    realms = []",
              "    survey -> instance of Bach$Project$ModuleSurvey",
              "    ModuleSurvey",
              "      declaredModules = []",
              "      requiredModules = {}",
              "    units = []"),
          project.print());
    }

    @Test
    void toStringRepresentationIsLegit() {
      assertNotNull(project.toString());
      assertFalse(project.toString().isBlank());
    }

    @Test
    void base() {
      assertEquals(Path.of("custom"), project.paths().base());
    }

    @Test
    void out() {
      assertEquals(Path.of("custom", ".bach"), project.paths().out());
    }

    @Test
    void lib() {
      assertEquals(Path.of("custom", "lib"), project.paths().lib());
    }

    @Test
    void mainModule() {
      assertEquals(Optional.empty(), project.mainModule());
    }

    @Test
    void name() {
      assertEquals("custom", project.descriptor().name());
    }

    @Test
    void units() {
      assertEquals(List.of(), project.units());
    }

    @Test
    void realms() {
      assertEquals(List.of(), project.realms());
    }

    @Test
    void version() {
      assertEquals(Version.parse("1.2-C"), project.descriptor().version().orElseThrow());
    }

    @Test
    void requires() {
      assertEquals(
          List.of("mandated java.base", "synthetic bar (@1701)", "synthetic foo (@4711)"),
          project.descriptor().requires().stream()
              .map(Object::toString)
              .sorted()
              .collect(Collectors.toList()));
    }

    @Test
    void moduleMappings() {
      var library = project.library();
      var zero = Version.parse("0");
      assertEquals("foo-4.5.jar", library.uri("foo", zero).toString());
      assertEquals("bar-123.jar", library.uri("bar", zero).toString());
      assertThrows(Bach.Util.Modules.UnmappedModuleException.class, () -> library.uri("abc", zero));
    }
  }

  @Nested
  class Layouts {
    @Test
    void jigsawed() {
      var path = Path.of("prefix/module/postfix/main/java");
      var unit =
          new Bach.Project.Unit(
              path.resolve("module-info.java"),
              ModuleDescriptor.newModule("module").build(),
              "prefix/*/postfix/main/java",
              List.of(),
              List.of());
      var flat = Bach.Project.Layout.FLAT;
      assertEquals("", flat.realmOf(unit).orElseThrow());
      assertEquals("prefix/*/postfix/main/java", unit.moduleSourcePath());
    }
  }

  @Nested
  class Sources {

    @Test
    void simple() {
      var source = Bach.Project.Source.of(Path.of("src"));
      assertEquals("src", source.path().toString());
      assertEquals(0, source.release());
      assertEquals(OptionalInt.empty(), source.target());
      assertFalse(source.isTargeted());
      assertFalse(source.isVersioned());
    }

    @Test
    void targeted() {
      var source = new Bach.Project.Source(Path.of("src"), 123, Set.of());
      assertEquals("src", source.path().toString());
      assertEquals(123, source.release());
      assertEquals(123, source.target().orElseThrow());
      assertTrue(source.isTargeted());
      assertFalse(source.isVersioned());
    }

    @Test
    void versioned() {
      var versioned = Set.of(Bach.Project.Source.Modifier.VERSIONED);
      var source = new Bach.Project.Source(Path.of("src-789"), 789, versioned);
      assertEquals("src-789", source.path().toString());
      assertEquals(789, source.release());
      assertEquals(789, source.target().orElseThrow());
      assertTrue(source.isTargeted());
      assertTrue(source.isVersioned());
    }
  }

  @Nested
  class Units {
    @Test
    void canonical() {
      var path = Path.of("canonical/module-info.java");
      var descriptor = ModuleDescriptor.newModule("canonical").build();
      var unit = new Bach.Project.Unit(path, descriptor, ".", List.of(), List.of());
      assertSame(path, unit.path());
      assertSame(descriptor, unit.descriptor());
      assertEquals(".", unit.moduleSourcePath());
      assertEquals(List.of(), unit.sources());
      assertEquals(List.of(), unit.sources(Bach.Project.Source::path));
      assertEquals(List.of(), unit.resources());
      assertEquals(Map.of("canonical", unit), Bach.Project.Unit.toMap(Stream.of(unit)));
    }
  }

  @Nested
  class Realms {
    @Test
    void canonical() {
      var realm = new Bach.Project.Realm("canonical", Set.of(), 0, ".", Map.of(), List.of());
      assertEquals("canonical", realm.name());
      assertEquals(Set.of(), realm.modifiers());
      for (var modifier : Bach.Project.Realm.Modifier.values()) {
        assertTrue(realm.lacks(modifier));
        assertFalse(realm.test(modifier));
      }
      assertTrue(realm.release().isEmpty());
      assertEquals(".", realm.moduleSourcePath());
    }
  }

  @Nested
  class ModuleSurveys {

    private void assertABC(Bach.Project.ModuleSurvey survey) {
      assertEquals(Set.of("a", "b"), survey.declaredModules());
      assertEquals(Set.of("a", "c"), survey.requiredModuleNames());
      assertNull(survey.requiredModules().get("a"));
      assertEquals("2", survey.requiredVersion("c").orElseThrow().toString());
      var expected = Bach.Util.Modules.UnmappedModuleException.class;
      assertThrows(expected, () -> survey.requiredVersion("b"));
      assertThrows(expected, () -> survey.requiredVersion("x"));
    }

    @Test
    void ofConstructor() {
      var declared = Set.of("a", "b");
      var required = new TreeMap<String, Version>();
      required.put("a", null);
      required.put("c", Version.parse("2"));
      assertABC(new Bach.Project.ModuleSurvey(declared, required));
    }

    @Test
    void ofStreamWithMultipleVersions() {
      var descriptors =
          Stream.of(
              ModuleDescriptor.newModule("a").requires("x").build(),
              ModuleDescriptor.newModule("b").requires(Set.of(), "x", Version.parse("1")).build(),
              ModuleDescriptor.newModule("c").requires("x").build(),
              ModuleDescriptor.newModule("d").requires(Set.of(), "x", Version.parse("2")).build());
      assertThrows(IllegalArgumentException.class, () -> Bach.Project.ModuleSurvey.of(descriptors));
    }

    @Test
    void surveyOfSystemModules() {
      var survey = Bach.Project.ModuleSurvey.of(ModuleFinder.ofSystem());
      assertTrue(survey.declaredModules().contains("java.base"));
      assertFalse(survey.requiredModules().containsKey("java.base")); // mandated are ignored
      assertTrue(survey.requiredModuleNames().contains("java.logging"));
      assertTrue(survey.declaredModules().size() > survey.requiredModules().size());
      assertLinesMatch(
          List.of(
              "ModuleSurvey",
              "\\Q  declaredModules = [\\E.*java\\.base.*\\]",
              "\\Q  requiredModules = {\\E.*java\\.logging.*\\}"),
          survey.print());
    }
  }
}
