package org.cdisc.tools;

import lombok.Getter;

public class Descriptor {
    @Getter
    private final String name;
    @Getter
    private final String description;

    public Descriptor(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
