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
}
