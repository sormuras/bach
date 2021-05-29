package com.github.sormuras.bach;

import com.github.sormuras.bach.api.Folders;
import com.github.sormuras.bach.trait.HttpTrait;

public record Configuration(Logbook logbook, ModuleLayer layer, Options options, Factory factory, Folders folders) implements HttpTrait {
	@Override
	public Configuration configuration() {
		return this;
	}
}
