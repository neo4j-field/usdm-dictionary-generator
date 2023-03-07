package org.cdisc.tools;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;

public class ModelClassProperty {
    @Getter @Setter
    private String type;
    @Getter @Setter
    private String propertyName;

    public ModelClassProperty(String name, String type) {
        this.type = type;
        this.propertyName = name;
    }

//    @Override
//    public String toString() {
//        return new Gson().toJson(this);
//    }
}
