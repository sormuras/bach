package com.github.sormuras.bach.project;

import java.util.List;

public record ProjectExternals(List<ExternalTool> tools) implements Project.Component {}
