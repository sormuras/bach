package com.github.sormuras.bach.project;

import java.util.List;
import java.util.Optional;

public record ExternalTool(String name, Optional<String> from, List<ExternalAsset> assets) {}
