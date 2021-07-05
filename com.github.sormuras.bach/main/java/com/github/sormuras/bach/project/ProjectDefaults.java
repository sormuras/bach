package com.github.sormuras.bach.project;

import java.nio.charset.Charset;

/**
 * Project-wide default properties.
 *
 * @param encoding Character encoding used by source files.
 */
public record ProjectDefaults(Charset encoding) {}
