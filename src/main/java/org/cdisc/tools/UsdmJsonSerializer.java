package org.cdisc.tools;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class UsdmJsonSerializer implements JsonSerializer<ModelClass> {

    @Override
    public JsonElement serialize(ModelClass modelClass, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("className", jsonSerializationContext.serialize(modelClass.getClassName()));
        jsonObject.add("properties", jsonSerializationContext.serialize(modelClass.getProperties()));
        return jsonObject;
    }
}
