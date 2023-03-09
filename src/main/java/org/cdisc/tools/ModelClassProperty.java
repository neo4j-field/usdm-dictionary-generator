package org.cdisc.tools;

import lombok.Getter;
import lombok.Setter;

public class ModelClassProperty extends Descriptor {
    @Getter
    @Setter
    private String type;

    public ModelClassProperty(String name, String type, String description) {
        super(name, description);
        this.type = type;
    }
}
