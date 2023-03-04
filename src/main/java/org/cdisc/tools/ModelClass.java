package org.cdisc.tools;

import java.util.Collection;

public class ModelClass extends Descriptor {

    private final Collection<ModelClassProperty> properties;

    public ModelClass(String name, String description, Collection<ModelClassProperty> properties) {
        super(name, description);
        this.properties = properties;
    }

    // TODO - Produce JSON
    @Override
    public String toString() {
        return "ModelClass{" +
                "properties=" + properties +
                '}';
    }
}
