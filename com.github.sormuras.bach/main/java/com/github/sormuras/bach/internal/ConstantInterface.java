package com.github.sormuras.bach.internal;

public record ConstantInterface()
    implements DurationSupport,
        ModuleDescriptorSupport,
        ModuleSupport,
        PathSupport,
        StringSupport,
        ToolFinderSupport,
        ToolProviderSupport {}
