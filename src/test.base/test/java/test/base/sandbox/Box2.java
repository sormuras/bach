package test.base.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.lang.module.ModuleDescriptor.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public interface Box2 {

  interface Option {
    void append(List<String> strings);
  }

  class Options {

    public static Options of(Option... options) {
      return new Options(List.of(options));
    }

    private final List<Option> options;

    public Options(List<Option> options) {
      this.options = options;
    }

    public List<Option> options() {
      return options;
    }

    public <T extends Option> Stream<T> filter(Class<T> type) {
      return options.stream().filter(type::isInstance).map(type::cast);
    }

    public <T extends Option> Optional<T> find(Class<T> type) {
      return filter(type).findAny();
    }

    public <T extends Option> T get(Class<T> type) {
      return find(type).orElseThrow();
    }

    public List<String> toStrings() {
      var list = new ArrayList<String>();
      options.forEach(option -> option.append(list));
      return list;
    }
  }

  class CompileModulesCheckingTimestamps implements Option {

    private final List<String> modules;

    public CompileModulesCheckingTimestamps(List<String> modules) {
      this.modules = modules;
    }

    public List<String> modules() {
      return modules;
    }

    @Override
    public void append(List<String> strings) {
      strings.add("--module");
      strings.add(String.join(",", modules));
    }
  }

  class VersionOfModulesThatAreBeingCompiled implements Option {

    private final Version version;

    public VersionOfModulesThatAreBeingCompiled(Version version) {
      this.version = version;
    }

    public Version version() {
      return version;
    }

    @Override
    public void append(List<String> strings) {
      strings.add("--module-version");
      strings.add(String.valueOf(version));
    }
  }

  class Tests {
    @Test
    void javac() {
      var modules = List.of("a", "b", "c");
      var version = Version.parse("123");
      var options =
          Options.of(
              new CompileModulesCheckingTimestamps(modules),
              new VersionOfModulesThatAreBeingCompiled(version));

      assertEquals(2, options.options().size());
      assertEquals(modules, options.get(CompileModulesCheckingTimestamps.class).modules());
      assertEquals(version, options.get(VersionOfModulesThatAreBeingCompiled.class).version());

      assertLinesMatch(
          List.of("--module", "a,b,c", "--module-version", "123"), options.toStrings());
    }
  }
}
