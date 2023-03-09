package org.cdisc.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

public class ModelClass extends Descriptor {
    @Getter
    @Setter
    private Map<String, ModelClassProperty> properties;

    public ModelClass(String name, Map<String, ModelClassProperty> properties, String description) {
        super(name, description);
        this.properties = properties;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().registerTypeAdapter(ModelClass.class, new UsdmJsonSerializer()).create();
        return gson.toJson(this);
    }
}
