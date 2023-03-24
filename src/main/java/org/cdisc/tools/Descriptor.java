package org.cdisc.tools;

import lombok.Getter;
import lombok.Setter;

public class Descriptor {
    @Getter
    private final String name;
    @Getter @Setter
    private String definition;
    @Getter @Setter
    private String cardinality;
    @Getter @Setter
    private String preferredTerm;
    @Getter @Setter
    private String defNciCode;


    public Descriptor(String name, String definition) {
        this.name = name;
        this.definition = definition;
    }
}
