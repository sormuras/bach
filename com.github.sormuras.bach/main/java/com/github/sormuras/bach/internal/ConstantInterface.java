package com.github.sormuras.bach.internal;

public record ConstantInterface()
    implements DurationSupport,
        ModuleDescriptorSupport,
        StringSupport,
        ToolFinderSupport,
        ToolProviderSupport {}
