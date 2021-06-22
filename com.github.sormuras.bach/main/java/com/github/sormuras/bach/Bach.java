package com.github.sormuras.bach;

import java.nio.file.Path;

public interface Bach {

  System.Logger.Level level();

  Folders folders();

  Builder builder(Project project);

  static String version() {
    var module = Bach.class.getModule();
    if (!module.isNamed()) return "(unnamed)";
    return module.getDescriptor().version().map(Object::toString).orElse("(exploded)");
  }

  static Configuration configureBach() {
    return new Configuration(System.Logger.Level.INFO, Folders.of(Path.of("")), Builder::new);
  }

  record Configuration(System.Logger.Level level, Folders folders, Builder.Factory builderFactory)
      implements Bach {

    @Override
    public Builder builder(Project project) {
      return builderFactory.newBuilder(this, project);
    }

    public Configuration with(Object component) {
      return new Configuration(
          component instanceof System.Logger.Level level ? level : level,
          component instanceof Folders folders ? folders : folders,
          component instanceof Builder.Factory factory ? factory : builderFactory);
    }

    public Configuration with(Builder.Factory factory) {
      return with((Object) factory);
    }
  }
}
