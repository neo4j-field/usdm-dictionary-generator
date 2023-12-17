package org.cdisc.tools;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ModelClassProperty extends Descriptor {
    @Getter
    @Setter
    private Set<String> types = new LinkedHashSet<>();
    @Getter
    @Setter
    private List<String> codeListReference;
    @Getter
    @Setter
    private String multiplicity;
    @Getter
    @Setter
    private String inheritedFrom;

    public ModelClassProperty(String name, String type, List<String> codeListReference, String description,
            String inheritedFrom, String multiplicity) {
        super(name, description);
        this.addType(type);
        this.codeListReference = codeListReference;
        this.inheritedFrom = inheritedFrom;
        this.multiplicity = multiplicity;
    }

    public void addType(String type) {
        this.types.add(type.replace("String", "string").replace("<", "\\<"));
    }

    public String printCodeLists() {
        var returnVal = "";
        if (this.codeListReference != null) {
            for (String element : this.codeListReference) {
                if (!element.toLowerCase().contains("point")) {
                    element = element.replace("(", "").replace(")", "");
                }
                returnVal = returnVal.concat(element);
            }
        }
        return returnVal;
    }

    public String printType() {
        return String.join(", ", this.types);
    }
}
