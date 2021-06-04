package test.integration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.api.CodeSpace;
import com.github.sormuras.bach.api.ExternalLibraryName;
import com.github.sormuras.bach.api.ExternalLibraryVersion;
import com.github.sormuras.bach.api.ExternalModuleLocation;
import com.github.sormuras.bach.api.ProjectInfo;
import com.github.sormuras.bach.api.Tweak;
import com.github.sormuras.bach.api.Workflow;
import com.github.sormuras.bach.tool.AnyCall;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.reflect.RecordComponent;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class OptionsTests {

  @Nested
  class EmptyOptionsTests {
    final Options empty = Options.of();

    @Test
    void emptyInstanceIsCached() {
      assertSame(empty, Options.of());
    }

    @TestFactory
    Stream<DynamicTest> componentIsNull() {
      return Stream.of(Options.class.getRecordComponents()).map(this::componentIsNull);
    }

    private DynamicTest componentIsNull(RecordComponent component) {
      return DynamicTest.dynamicTest(
          "%s is null".formatted(component.getName()),
          () -> assertNull(component.getAccessor().invoke(empty)));
    }
  }

  @Nested
  class DefaultOptionsTests {
    final Options defaults = Options.ofDefaultValues();

    @TestFactory
    Stream<DynamicTest> flagIsFalse() {
      return Stream.of(Options.class.getRecordComponents())
          .filter(component -> component.getType() == Boolean.class)
          .map(this::flagIsFalse);
    }

    private DynamicTest flagIsFalse(RecordComponent component) {
      return DynamicTest.dynamicTest(
          "%s is false".formatted(component.getName()),
          () -> assertFalse((Boolean) component.getAccessor().invoke(defaults)));
    }

    @TestFactory
    Stream<DynamicTest> listIsNotNull() {
      return Stream.of(Options.class.getRecordComponents())
          .filter(component -> component.getType() == List.class)
          .map(this::listIsNotNull);
    }

    private DynamicTest listIsNotNull(RecordComponent component) {
      return DynamicTest.dynamicTest(
          "%s is not null".formatted(component.getName()),
          () -> assertNotNull(component.getAccessor().invoke(defaults)));
    }
  }

  @Test
  void assertProjectInfoAnnotationDefaults() {
    var annotation = Options.class.getModule().getAnnotation(ProjectInfo.class);
    var options = Options.ofProjectInfo(annotation);
    assertArrayEquals(new String[0], annotation.arguments());
    assertEquals(ProjectInfo.DEFAULT_NAME, options.project_name());
    assertEquals(Version.parse(ProjectInfo.DEFAULT_VERSION), options.project_version());
    assertEquals(List.of(), options.project_requires());
  }

  @Test
  void withEverything() {

    var expected =
        new Options(
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            "TOOL",
            "MODULE",
            true,
            new AnyCall("NAME").with("ARG1", "ARG2"),
            Path.of("PATH"),
            "MODULE",
            "NAME",
            Version.parse("0-ea+VERSION"),
            List.of("M1", "M2"),
            List.of("*", "**"),
            List.of("PATH", "PATH"),
            9,
            true,
            List.of("test", "**/test", "**/test/**"),
            List.of("PATH", "PATH"),
            List.of("TOOL"),
            List.of("TOOL"),
            List.of(new Tweak(EnumSet.allOf(CodeSpace.class), "TRIGGER", List.of("ARGS..."))),
            List.of(new ExternalModuleLocation("M1", "U1"), new ExternalModuleLocation("M2", "U2")),
            List.of(new ExternalLibraryVersion(ExternalLibraryName.JUNIT, "VERSION")),
            List.copyOf(EnumSet.allOf(Workflow.class)));

    var arguments =
        """
          --bach-info
            MODULE
          --chroot
            PATH
          --describe-tool
            TOOL
          --dry-run
          --external-library-version
            JUNIT=VERSION
          --external-module-location
            M1=U1
          --external-module-location
            M2=U2
          --help
          --help-extra
          --limit-tool
            TOOL
          --print-configuration
          --print-modules
          --print-declared-modules
          --print-external-modules
          --print-system-modules
          --print-tools
          --load-external-module
            MODULE
          --load-missing-external-modules
          --main-jar-with-sources
          --main-java-release
            9
          --main-module-path
            PATH
          --main-module-path
            PATH
          --main-module-pattern
            *
          --main-module-pattern
            **
          --project-name
            NAME
          --project-requires
            M1
          --project-requires
            M2
          --project-version
            0-ea+VERSION
          --run-commands-sequentially
          --skip-tool
            TOOL
          --test-module-path
            PATH
          --test-module-path
            PATH
          --test-module-pattern
            test
          --test-module-pattern
            **/test
          --test-module-pattern
            **/test/**
          --tweak
            main,test\\nTRIGGER\\nARGS...
          --verbose
          --version
          --workflow
            BUILD
          --workflow
            CLEAN
          --workflow
            RESOLVE
          --workflow
            COMPILE_MAIN
          --workflow
            COMPILE_TEST
          --workflow
            EXECUTE_TESTS
          --workflow
            GENERATE_DOCUMENTATION
          --workflow
            GENERATE_IMAGE
          --workflow
            WRITE_LOGBOOK
          --tool
            NAME
            ARG1
            ARG2
          """;

    var actual =
        Options.ofCommandLineArguments(arguments.lines().map(String::translateEscapes).toList());
    assertEquals(expected, actual);
  }
}
