package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.project.DeclaredModule;
import java.util.Set;

public record Space(String name, Set<DeclaredModule> modules) {}
