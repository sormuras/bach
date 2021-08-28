package com.github.sormuras.bach.internal;

record ConstantInterface()
    implements DurationSupport,
        ModuleDescriptorSupport,
        ModuleFinderSupport,
        ModuleSupport,
        PathSupport,
        StringSupport,
        ToolFinderSupport,
        ToolProviderSupport,
        VersionSupport {}
