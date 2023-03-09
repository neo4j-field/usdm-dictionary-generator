package org.cdisc.tools;

import lombok.Getter;
import lombok.Setter;

public class Descriptor {
    @Getter
    private final String name;
    @Getter
    @Setter
    private String description;

    public Descriptor(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
