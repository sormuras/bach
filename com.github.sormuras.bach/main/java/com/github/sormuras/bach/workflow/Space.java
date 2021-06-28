package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.project.Module;
import java.util.Set;

public record Space(String name, Set<Module> modules) {}
