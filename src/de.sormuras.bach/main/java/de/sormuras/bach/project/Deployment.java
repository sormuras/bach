package de.sormuras.bach.project;

import java.net.URI;
import java.nio.file.Path;

/** Properties used to upload compiled modules. */
public class /*record*/ Deployment {
  private final Path mavenPomTemplate;
  private final String mavenRepositoryId;
  private final URI mavenUri;

  public Deployment(Path mavenPomTemplate, String mavenRepositoryId, URI mavenUri) {
    this.mavenPomTemplate = mavenPomTemplate;
    this.mavenRepositoryId = mavenRepositoryId;
    this.mavenUri = mavenUri;
  }

  public Path mavenPomTemplate() {
    return mavenPomTemplate;
  }

  /** Maven repository id. */
  public String mavenRepositoryId() {
    return mavenRepositoryId;
  }

  /** Maven URL as an URI. */
  public URI mavenUri() {
    return mavenUri;
  }
}
