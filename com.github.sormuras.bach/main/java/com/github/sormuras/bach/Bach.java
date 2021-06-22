package com.github.sormuras.bach;

import java.nio.file.Path;

public interface Bach {

  Logbook logbook();

  Folders folders();

  Builder builder(Project project);

  static String version() {
    var module = Bach.class.getModule();
    if (!module.isNamed()) return "(unnamed)";
    return module.getDescriptor().version().map(Object::toString).orElse("(exploded)");
  }

  static Configuration configureBach() {
    return new Configuration(Logbook.ofSystem(), Folders.of(Path.of("")), Builder::new);
  }

  record Configuration(Logbook logbook, Folders folders, Builder.Factory builderFactory)
      implements Bach {

    @Override
    public Builder builder(Project project) {
      return builderFactory.newBuilder(this, project);
    }

    public Configuration with(Object component) {
      return new Configuration(
          component instanceof Logbook logbook ? logbook : logbook,
          component instanceof Folders folders ? folders : folders,
          component instanceof Builder.Factory factory ? factory : builderFactory);
    }

    public Configuration with(Builder.Factory factory) {
      return with((Object) factory);
    }
  }
}
