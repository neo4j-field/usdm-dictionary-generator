package org.cdisc.tools;

import lombok.Getter;

public class ModelClassProperty extends Descriptor {
    @Getter
    private final String type;

    public ModelClassProperty(String name, String description, String type) {
        super(name, description);
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("'name': '%1$s', 'type': '%2$s'", this.getName(), this.getType());
    }
}
