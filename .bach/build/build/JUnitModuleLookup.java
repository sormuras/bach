package build;

import com.github.sormuras.bach.project.ModuleLookup;
import java.util.Optional;

public class JUnitModuleLookup implements ModuleLookup {

  private final ModuleLookup junitJupiter;
  private final ModuleLookup junitPlatform;

  public JUnitModuleLookup() {
    this.junitJupiter = new JUnitJupiter("5.7.0");
    this.junitPlatform = new JUnitPlatform("1.7.0");
  }

  @Override
  public Optional<String> lookup(String module) {
    return junitPlatform.lookup(module)
        .or(() -> junitJupiter.lookup(module))
        .or(Optional::empty);
  }
}
