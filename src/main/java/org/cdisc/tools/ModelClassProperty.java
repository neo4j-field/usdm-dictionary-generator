package org.cdisc.tools;

import lombok.Getter;

public class ModelClassProperty extends Descriptor {
    @Getter
    private final String type;

    public ModelClassProperty(String name, String description, String type) {
        super(name, description);
        this.type = type;
    }
}
