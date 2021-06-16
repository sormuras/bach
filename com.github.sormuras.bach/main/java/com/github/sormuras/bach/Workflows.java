package com.github.sormuras.bach;

import com.github.sormuras.bach.workflow.BuildWorkflow;
import com.github.sormuras.bach.workflow.CleanWorkflow;
import com.github.sormuras.bach.workflow.CompileMainCodeSpaceWorkflow;
import com.github.sormuras.bach.workflow.CompileTestCodeSpaceWorkflow;
import com.github.sormuras.bach.workflow.ExecuteTestsWorkflow;
import com.github.sormuras.bach.workflow.GenerateDocumentationWorkflow;
import com.github.sormuras.bach.workflow.GenerateImageWorkflow;
import com.github.sormuras.bach.workflow.ResolveWorkflow;
import com.github.sormuras.bach.workflow.WriteLogbookWorkflow;
import java.util.Map;

public record Workflows(
    New<BuildWorkflow> newBuildWorkflow,
    New<CleanWorkflow> newCleanWorkflow,
    New<CompileMainCodeSpaceWorkflow> newCompileMainCodeSpaceWorkflow,
    New<CompileTestCodeSpaceWorkflow> newCompileTestCodeSpaceWorkflow,
    New<ExecuteTestsWorkflow> newExecuteTestsWorkflow,
    New<GenerateDocumentationWorkflow> newGenerateDocumentationWorkflow,
    New<GenerateImageWorkflow> newGenerateImageWorkflow,
    New<ResolveWorkflow> newResolveWorkflow,
    New<WriteLogbookWorkflow> newWriteLogbookWorkflow) {

  @FunctionalInterface
  public interface New<WORKFLOW> {
    WORKFLOW with(Bach bach);
  }

  public static Workflows of() {
    return new Workflows(
        BuildWorkflow::new,
        CleanWorkflow::new,
        CompileMainCodeSpaceWorkflow::new,
        CompileTestCodeSpaceWorkflow::new,
        ExecuteTestsWorkflow::new,
        GenerateDocumentationWorkflow::new,
        GenerateImageWorkflow::new,
        ResolveWorkflow::new,
        WriteLogbookWorkflow::new);
  }

  public <T> Workflows with(Class<T> type, New<? extends T> factory) {
    return with(Map.of(type, factory));
  }

  public Workflows with(Map<Class<?>, New<?>> factories) {
    return new Workflows(
        with(newBuildWorkflow, factories.get(BuildWorkflow.class)),
        with(newCleanWorkflow, factories.get(CleanWorkflow.class)),
        with(newCompileMainCodeSpaceWorkflow, factories.get(CompileMainCodeSpaceWorkflow.class)),
        with(newCompileTestCodeSpaceWorkflow, factories.get(CompileTestCodeSpaceWorkflow.class)),
        with(newExecuteTestsWorkflow, factories.get(ExecuteTestsWorkflow.class)),
        with(newGenerateDocumentationWorkflow, factories.get(GenerateDocumentationWorkflow.class)),
        with(newGenerateImageWorkflow, factories.get(GenerateImageWorkflow.class)),
        with(newResolveWorkflow, factories.get(ResolveWorkflow.class)),
        with(newWriteLogbookWorkflow, factories.get(WriteLogbookWorkflow.class)));
  }

  @SuppressWarnings("unchecked")
  private static <T> T with(T old, Object value) {
    return (value == null) ? old : (T) value;
  }
}
