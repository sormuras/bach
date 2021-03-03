package test.integration.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.project.ModuleInfoReference;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Modifier;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ModuleInfoReferenceTests {

  @Nested
  class Describe {

    private ModuleDescriptor describe(String module, Consumer<ModuleDescriptor.Builder> consumer) {
      var builder = ModuleDescriptor.newModule(module, Set.of(Modifier.SYNTHETIC));
      consumer.accept(builder);
      return builder.build();
    }

    private ModuleDescriptor describe(String source) {
      return ModuleInfoReference.parse(source).build();
    }

    @Test
    void parsingArbitraryTextFails() {
      assertThrows(IllegalArgumentException.class, () -> describe("C="));
    }

    @Test
    void minimalisticModuleDeclaration() {
      var actual = describe("module a{}");
      assertEquals(describe("a", a -> {}), actual);
    }

    @Test
    void moduleDeclarationWithRequires() {
      var actual = describe("module a{requires b;}");
      assertEquals(describe("a", a -> a.requires("b")), actual);
    }

    @Test
    void moduleDeclarationWithRequiresAndVersion() {
      var actual = describe("module a{requires b/*1.2*/;}");
      assertEquals(describe("a", a -> a.requires(Set.of(), "b", Version.parse("1.2"))), actual);
    }

    @Test
    void moduleDeclarationWithComments() {
      var source =
          """
          open /*test*/ module a /*extends a*/ {
            // requires nothing;
          }""";
      assertEquals(describe("a", a -> {}), describe(source));
    }
  }

  @Nested
  class MainClassConvention {

    @Test
    void mainClassOfBachIsPresent() {
      var module = "com.github.sormuras.bach";
      var info = Path.of(module, "main", "java", "module-info.java");
      var reference = ModuleInfoReference.of(info);
      var mainClass = reference.descriptor().mainClass();
      assertTrue(mainClass.isPresent());
      assertEquals(module + ".Main", mainClass.get());
    }

    @Test
    void mainClassOfNotExistingModuleInfoIsNotPresent() {
      assertFalse(ModuleInfoReference.findMainClass(Path.of("module-info.java"), "a").isPresent());
    }
  }

  @Nested
  class MainModuleConvention {

    /** Return name of the main module by finding a single main class containing descriptor. */
    static Optional<String> findMainModule(Stream<ModuleDescriptor> descriptors) {
      var mains = descriptors.filter(d -> d.mainClass().isPresent()).toList();
      return mains.size() == 1 ? Optional.of(mains.get(0).name()) : Optional.empty();
    }

    @Test
    void empty() {
      assertTrue(findMainModule(Stream.empty()).isEmpty());
    }

    @Test
    void single() {
      var a = ModuleDescriptor.newModule("a").mainClass("a.A").build();
      assertEquals("a", findMainModule(Stream.of(a)).orElseThrow());
    }

    @Test
    void multipleModuleWithSingletonMainClass() {
      var a = ModuleDescriptor.newModule("a").build();
      var b = ModuleDescriptor.newModule("b").mainClass("b.B").build();
      var c = ModuleDescriptor.newModule("c").build();
      assertEquals("b", findMainModule(Stream.of(a, b, c)).orElseThrow());
    }

    @Test
    void multipleModuleWithMultipleMainClasses() {
      var a = ModuleDescriptor.newModule("a").mainClass("a.A").build();
      var b = ModuleDescriptor.newModule("b").mainClass("b.B").build();
      var c = ModuleDescriptor.newModule("c").mainClass("c.C").build();
      assertTrue(findMainModule(Stream.of(a, b, c)).isEmpty());
    }
  }
}
