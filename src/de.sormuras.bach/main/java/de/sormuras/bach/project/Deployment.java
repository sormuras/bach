package de.sormuras.bach.project;

import java.net.URI;

/** Properties used to upload compiled modules. */
public class /*record*/ Deployment {
  private final String mavenRepositoryId;
  private final URI mavenUri;

  public Deployment(String mavenRepositoryId, URI mavenUri) {
    this.mavenRepositoryId = mavenRepositoryId;
    this.mavenUri = mavenUri;
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
