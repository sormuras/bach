package com.github.sormuras.bach;

import com.github.sormuras.bach.workflow.ResolveWorkflow;
import com.github.sormuras.bach.workflow.BuildWorkflow;
import com.github.sormuras.bach.workflow.CleanWorkflow;
import com.github.sormuras.bach.workflow.CompileMainCodeSpaceWorkflow;
import com.github.sormuras.bach.workflow.CompileTestCodeSpaceWorkflow;
import com.github.sormuras.bach.workflow.ExecuteTestsWorkflow;
import com.github.sormuras.bach.workflow.GenerateDocumentationWorkflow;
import com.github.sormuras.bach.workflow.GenerateImageWorkflow;
import com.github.sormuras.bach.workflow.WriteLogbookWorkflow;
import com.github.sormuras.bach.internal.Strings;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

public class Factory {

  private final AtomicReference<HttpClient> atomicHttpClient = new AtomicReference<>();

  public Factory() {}

  public HttpClient defaultHttpClient(Bach bach) {
    var oldClient = atomicHttpClient.get();
    if (oldClient != null) return oldClient;
    var newClient = newHttpClientBuilder(bach).build();
    return atomicHttpClient.compareAndSet(null, newClient) ? newClient : atomicHttpClient.get();
  }

  public ProjectBuilder newProjectBuilder(Logbook logbook, Options options) {
    return new ProjectBuilder(logbook, options);
  }

  public HttpClient.Builder newHttpClientBuilder(Bach bach) {
    var timeout = Duration.ofSeconds(9);
    var policy = HttpClient.Redirect.NORMAL;
    bach.log(
        "New HttpClient.Builder with %s connect timeout and %s redirect policy"
            .formatted(Strings.toString(timeout), policy));
    return HttpClient.newBuilder().connectTimeout(timeout).followRedirects(policy);
  }

  public BuildWorkflow newBuildWorkflow(Bach bach) {
    return new BuildWorkflow(bach);
  }

  public ResolveWorkflow newResolveWorkflow(Bach bach) {
    return new ResolveWorkflow(bach);
  }

  public CleanWorkflow newCleanWorkflow(Bach bach) {
    return new CleanWorkflow(bach);
  }

  public CompileMainCodeSpaceWorkflow newCompileMainCodeSpaceWorkflow(Bach bach) {
    return new CompileMainCodeSpaceWorkflow(bach);
  }

  public CompileTestCodeSpaceWorkflow newCompileTestCodeSpaceWorkflow(Bach bach) {
    return new CompileTestCodeSpaceWorkflow(bach);
  }

  public ExecuteTestsWorkflow newExecuteTestsWorkflow(Bach bach) {
    return new ExecuteTestsWorkflow(bach);
  }

  public GenerateDocumentationWorkflow newGenerateDocumentationWorkflow(Bach bach) {
    return new GenerateDocumentationWorkflow(bach);
  }

  public GenerateImageWorkflow newGenerateImageWorkflow(Bach bach) {
    return new GenerateImageWorkflow(bach);
  }

  public WriteLogbookWorkflow newWriteLogbookWorkflow(Bach bach) {
    return new WriteLogbookWorkflow(bach);
  }
}
