package com.github.sormuras.bach;

import com.github.sormuras.bach.core.BuildAction;
import com.github.sormuras.bach.core.CleanAction;
import com.github.sormuras.bach.core.CompileMainCodeSpaceAction;
import com.github.sormuras.bach.core.CompileTestCodeSpaceAction;
import com.github.sormuras.bach.core.ExecuteTestsAction;
import com.github.sormuras.bach.core.ProjectBuilder;
import com.github.sormuras.bach.core.WriteLogbookAction;
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

  public BuildAction newBuildAction(Bach bach) {
    return new BuildAction(bach);
  }

  public CleanAction newCleanAction(Bach bach) {
    return new CleanAction(bach);
  }

  public CompileMainCodeSpaceAction newCompileMainCodeSpaceAction(Bach bach) {
    return new CompileMainCodeSpaceAction(bach);
  }

  public CompileTestCodeSpaceAction newCompileTestCodeSpaceAction(Bach bach) {
    return new CompileTestCodeSpaceAction(bach);
  }

  public ExecuteTestsAction newExecuteTestsAction(Bach bach) {
    return new ExecuteTestsAction(bach);
  }

  public WriteLogbookAction newWriteLogbookAction(Bach bach) {
    return new WriteLogbookAction(bach);
  }
}
