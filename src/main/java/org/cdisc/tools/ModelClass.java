package org.cdisc.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

public class ModelClass {
    @Getter @Setter
    private Collection<ModelClassProperty> properties;
    @Getter @Setter
    private String className;

    public ModelClass(String name, Collection<ModelClassProperty> properties) {
        this.properties = properties;
        this.className = name;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().registerTypeAdapter(ModelClass.class, new UsdmJsonSerializer()).create();
        return gson.toJson(this);
    }
}
