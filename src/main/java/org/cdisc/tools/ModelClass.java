package org.cdisc.tools;

import lombok.Getter;

import java.util.Collection;

public class ModelClass extends Descriptor {
    @Getter
    private final Collection<ModelClassProperty> properties;

    public ModelClass(String name, String description, Collection<ModelClassProperty> properties) {
        super(name, description);
        this.properties = properties;
    }

    @Override
    public String toString() {
        return String.format("'%1$s': 'properties': { %2$s }", this.getName(), this.getProperties());
    }
}
