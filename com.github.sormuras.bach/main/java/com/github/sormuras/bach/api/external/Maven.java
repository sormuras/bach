package com.github.sormuras.bach.api.external;

import java.util.Objects;
import java.util.StringJoiner;

public final class Maven {

  public static final String CENTRAL_REPOSITORY = "https://repo.maven.apache.org/maven2";

  public static String central(String group, String artifact, String version) {
    return Joiner.of(group, artifact, version).toString();
  }

  public static String central(String group, String artifact, String version, String classifier) {
    return Joiner.of(group, artifact, version).classifier(classifier).toString();
  }

  public static class Joiner {

    public static Joiner of(String group, String artifact, String version) {
      return new Joiner().group(group).artifact(artifact).version(version);
    }

    private String repository = CENTRAL_REPOSITORY;
    private String group;
    private String artifact;
    private String version;
    private String classifier = "";
    private String type = "jar";

    /** Hidden default constructor. */
    private Joiner() {}

    @Override
    public String toString() {
      var joiner = new StringJoiner("/").add(repository);
      joiner.add(group.replace('.', '/')).add(artifact).add(version);
      var file = artifact + '-' + (classifier.isBlank() ? version : version + '-' + classifier);
      return joiner.add(file + '.' + type).toString();
    }

    public Joiner repository(String repository) {
      this.repository = Objects.requireNonNull(repository, "repository");
      return this;
    }

    public Joiner group(String group) {
      this.group = Objects.requireNonNull(group, "group");
      return this;
    }

    public Joiner artifact(String artifact) {
      this.artifact = Objects.requireNonNull(artifact, "artifact");
      return this;
    }

    public Joiner version(String version) {
      this.version = Objects.requireNonNull(version, "version");
      return this;
    }

    public Joiner classifier(String classifier) {
      this.classifier = Objects.requireNonNull(classifier, "classifier");
      return this;
    }

    public Joiner type(String type) {
      this.type = Objects.requireNonNull(type, "type");
      return this;
    }
  }

  /** Hidden default constructor. */
  private Maven() {}
}