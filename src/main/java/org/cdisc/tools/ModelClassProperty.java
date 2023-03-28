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

    public String printCodeLists() {
        var returnVal = "";
        if (this.codeListReference != null) {
            for (String element: this.codeListReference) {
                if (!element.toLowerCase().contains("point")) {
                    element = element.replace("(","").replace(")","");
                }
                returnVal = returnVal.concat(element);
            }
        }
        return returnVal;
    }

    public String printType() {
        var returnVal = "";
        if (this.type != null) {
            returnVal = this.type.replace("String", "string");
        }
        return returnVal;
    }
}
