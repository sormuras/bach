package configuration;

import com.github.sormuras.bach.project.ModuleLookup;
import java.nio.file.Path;
import java.util.Optional;

public class FooModuleLookup implements ModuleLookup {

  public FooModuleLookup() {}

  public Optional<String> lookup(String module) {
    var uri = Path.of("module-foo.zip").toUri().toString();
    return module.equals("foo") ? Optional.of(uri) : Optional.empty();
  }
}
