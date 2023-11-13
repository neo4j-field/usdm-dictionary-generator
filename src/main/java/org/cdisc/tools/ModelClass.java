package org.cdisc.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class ModelClass extends Descriptor {
    @Getter
    @Setter
    private Map<String, ModelClassProperty> properties;
    @Getter
    @Setter
    private Set<String> superClasses;
    @Getter
    @Setter
    private Set<String> subClasses;

    public ModelClass(String name, Map<String, ModelClassProperty> properties, String description) {
        super(name, description);
        this.properties = properties;
        this.superClasses = new TreeSet<>();
        this.subClasses = new TreeSet<>();
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().registerTypeAdapter(ModelClass.class, new UsdmJsonSerializer()).create();
        return gson.toJson(this);
    }

}
