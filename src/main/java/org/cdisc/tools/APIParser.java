package org.cdisc.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import lombok.Getter;

public class APIParser {
    private String inputFileName;

    public APIParser(String inputFileName) {
        this.inputFileName = inputFileName;
    }

    public Map<String, ModelClass> getEntitiesMap() throws IOException, PathNotFoundException {
        try (
                var currAPIFile = GeneratorApp.class.getClassLoader()
                        .getResourceAsStream(this.inputFileName);) {

            Object jsonDocument = Configuration.defaultConfiguration().jsonProvider()
                    .parse(new String(currAPIFile.readAllBytes()));

            // final String root = "$.components.schemas.Study-Output";
            final String root = "#/components/schemas/Study-Output";
            Map<String, ModelClass> elements = new TreeMap<>();
            List<TypeDefinition> rootTypes = new ArrayList<>();
            getTypes(rootTypes, jsonDocument, Map.of("$ref", root));
            buildEntitiesMap(jsonDocument, elements, rootTypes.get(0));
            return elements;
        }
    }

    private static class TypeDefinition {
        @Getter
        private String type;
        @Getter
        private Map<String, ?> definition;

        private TypeDefinition(String type, Map<String, ?> definition) {
            this.type = type;
            this.definition = definition;
        }

        private TypeDefinition(String type) {
            this.type = type;
        }
    }

    private TypeDefinition classNameFromRef(Object jsonDocument, Map<String, ?> property) {
        if (!property.containsKey("$ref")) {
            return null;
        }
        String ref = (String) property.get("$ref");
        ref = ref.replaceFirst("\\#", "\\$");
        ref = ref.replaceAll("\\/", "\\.");
        Map<String, ?> definition = ((Map<String, ?>) JsonPath.read(jsonDocument, ref));
        return new TypeDefinition((String) definition.get("title"), definition);
    }

    private void getTypes(List<TypeDefinition> types, Object jsonDocument, Map<String, ?> property) {
        if (property.containsKey("type")) {
            String propertyJSONType = (String) property.get("type");
            if (propertyJSONType.equals("array")) {
                getTypes(types, jsonDocument, (Map<String, ?>) property.get("items"));
            } else {
                types.add(new TypeDefinition(propertyJSONType));
            }
        } else if (property.containsKey("const")) {
            Object propertyJSONValue = property.get("const");
            if (propertyJSONValue instanceof String) {
                types.add(new TypeDefinition("string"));
            }
        } else if (property.containsKey("anyOf")) {
            for (Map<String, String> typeObject : ((List<Map<String, String>>) property.get("anyOf"))) {
                getTypes(types, jsonDocument, typeObject);
            }
        } else if (property.containsKey("$ref")) {
            types.add(classNameFromRef(jsonDocument, property));
        }
    }

    private void buildEntitiesMap(Object jsonDocument, Map<String, ModelClass> elements, TypeDefinition classPath) {
        String className = classPath.type;
        if (elements.containsKey(className)) {
            return;
        }
        ModelClass modelClass = new ModelClass(className, new LinkedHashMap<>(), null);
        elements.put(className, modelClass);
        Map<String, Map<String, ?>> propertiesFromAPI = (Map<String, Map<String, ?>>) classPath.definition
                .get("properties");
        for (Map.Entry<String, Map<String, ?>> propertyFromAPI : propertiesFromAPI.entrySet()) {
            String propertyName = propertyFromAPI.getKey();
            List<TypeDefinition> types = new ArrayList<>();
            getTypes(types, jsonDocument, propertyFromAPI.getValue());
            if (types.isEmpty()) {
                return;
            }
            ModelClassProperty property = new ModelClassProperty(propertyName,
                    types.stream().map(TypeDefinition::getType).collect(Collectors.joining(", ")),
                    null, null, null, null);
            for (TypeDefinition type : types) {
                if (type.definition != null) {
                    buildEntitiesMap(jsonDocument, elements, type);
                }
            }

            elements.get(className).getProperties().put(propertyName, property);
        }
    }

}
