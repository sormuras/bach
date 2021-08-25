package com.github.sormuras.bach.internal;

record ConstantInterface()
    implements DurationSupport,
        ModuleDescriptorSupport,
        ModuleSupport,
        PathSupport,
        StringSupport,
        ToolFinderSupport,
        ToolProviderSupport,
        VersionSupport {}
