package run.bach.internal;

import java.lang.module.ModuleReader;
import java.net.URI;
import java.util.Optional;
import java.util.stream.Stream;

class NullModuleReader implements ModuleReader {

  @Override
  public Optional<URI> find(String name) {
    return Optional.empty();
  }

  @Override
  public Stream<String> list() {
    return Stream.empty();
  }

  @Override
  public void close() {}
}
