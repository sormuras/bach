package com.github.sormuras.bach.project;

import java.util.List;

public record Tools(List<ExternalTool> externals) implements Project.Component {}