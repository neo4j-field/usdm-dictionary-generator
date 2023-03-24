package org.cdisc.tools;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class ModelClassProperty extends Descriptor {
    @Getter
    @Setter
    private String type;
    @Getter
    @Setter
    private List<String> codeListReference;
    @Getter
    @Setter
    private String multiplicity;

    public ModelClassProperty(String name, String type, List<String> codeListReference, String description) {
        super(name, description);
        this.type = type;
        this.codeListReference = codeListReference;
    }
}
