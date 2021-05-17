package bach.info;

import com.github.sormuras.bach.api.ExternalModuleLocation;
import com.github.sormuras.bach.api.ExternalModuleLocator;
import java.util.Optional;

public class MyLocator implements ExternalModuleLocator {
  @Override
  public Optional<ExternalModuleLocation> locate(String module) {
    return Optional.empty();
  }

  @Override
  public Stability stability() {
    return Stability.STABLE;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ExternalModuleLocator;
  }
}
