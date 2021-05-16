package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;

public class ResolveWorkflow extends BachWorkflow {

  public ResolveWorkflow(Bach bach) {
    super(bach);
  }

  public void resolve() {
    downloadMissingExternalModules();
    verifyExternalModules();
  }

  protected void downloadMissingExternalModules() {
    bach().loadMissingExternalModules();
  }

  protected void verifyExternalModules() {
    // TODO Find unused external modules
    // TODO Compute and compare with configured checksums
  }
}
