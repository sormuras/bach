package com.github.sormuras.bach.project;

import com.github.sormuras.bach.Project;
import java.lang.module.ModuleDescriptor;

public record ProjectVersion(ModuleDescriptor.Version value) implements Project.Component {}
