package org.cdisc.tools;

import java.util.*;

import net.steppschuh.markdowngenerator.table.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    // TODO - This one is probably not necessary after all
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }


    public static Table.Builder printDifferences(Map<String, ModelClass> prev, Map<String, ModelClass> curr) {
        logger.info("ENTER printDifferences");
        //  Props: {ClassName: <Property Obj>}
        Map<String, ModelClassProperty> newProps = new HashMap<>();
        Map<String, ModelClassProperty> removedProps = new HashMap<>();
        List<ModelClass> newModelClasses = new ArrayList<>();
        List<ModelClass> removedModelClasses = new ArrayList<>();

        List<ModelClass> prevClasses = prev.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(item ->
                item.getValue()).toList();
        List<ModelClass> currClasses = curr.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(item ->
                item.getValue()).toList();

        int i = 0;
        int j = 0;

        while (i < prevClasses.size() || j < currClasses.size()) {
            // Begin by looking for changes in class name
            String prevClassName = (i < prevClasses.size()) ? prevClasses.get(i).getName():"";
            String currClassName = (j < currClasses.size()) ? currClasses.get(j).getName():"";

            if (!prevClassName.equals(currClassName)) {
                // A class name has been removed
                if (prevClassName.compareTo(currClassName) < 0 || currClassName.equals("")) {
                    removedModelClasses.add(prevClasses.get(i));
                    logger.debug(String.format("Class Name Removed! %1$s", prevClassName));
                    i++;
                }
                // A new class name has appeared
                else {
                    newModelClasses.add(currClasses.get(j));
                    logger.debug(String.format("New Class Name found! %1$s", currClassName));
                    j++;
                }
            }
            else {
                List<ModelClassProperty> prevProperties = prevClasses.get(i).getProperties().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey()).map(item -> item.getValue()).toList();
                List<ModelClassProperty> currProperties = currClasses.get(j).getProperties().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey()).map(item -> item.getValue()).toList();

                int k = 0;
                int l = 0;

                while (k < prevProperties.size() && l < currProperties.size()) {
                    String prevPropName = (k < prevProperties.size()) ? prevProperties.get(k).getName(): "";
                    String currPropName = (l < currProperties.size()) ? currProperties.get(l).getName(): "";

                    if (!prevPropName.equals(currPropName)) {
                        // A property name has been removed
                        if (prevPropName.compareTo(currPropName) < 0 || currPropName.equals("")) {
                            removedProps.put(prevClasses.get(i).getName(), prevProperties.get(k));
                            logger.debug(String.format("Property Name Removed! %1$s", prevPropName));
                            k++;
                        }
                        // A new property is present
                        else {
                            newProps.put(currClasses.get(j).getName(), currProperties.get(l));
                            logger.debug(String.format("New Property Name found! %1$s", currPropName));
                            l++;
                        }
                    }
                    else {
                        k++;
                        l++;
                    }
                }
                i++;
                j++;
            }
        }
        Table.Builder tableBuilder = new Table.Builder()
                .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_LEFT, Table.ALIGN_LEFT, Table.ALIGN_LEFT, Table.ALIGN_LEFT);
        tableBuilder.addRow("Status", "Class Name", "Property Name", "Data Type");
        newModelClasses.forEach(entry -> {
            // Class Row
            tableBuilder.addRow("Class - NEW", entry.getName(), null, null);
            entry.getProperties().entrySet().forEach(propEntry -> {
                // Property Rows
                tableBuilder.addRow(null, null, propEntry.getValue().getName(),
                        propEntry.getValue().getType().replace("<", "\\<"));
            });
        });
        removedModelClasses.forEach(entry -> {
            // Class Row
            tableBuilder.addRow("Class - DELETED", entry.getName(), null, null);
            entry.getProperties().entrySet().forEach(propEntry -> {
                // Property Rows
                tableBuilder.addRow(null, null, propEntry.getValue().getName(),
                        propEntry.getValue().getType().replace("<", "\\<"));
            });
        });
        tableBuilder.addRow("",null);
        newProps.forEach((entry, prop) -> {
            tableBuilder.addRow("Property - NEW", entry, prop.getName(), prop.getType());
        });
        removedProps.forEach((entry, prop) -> {
            tableBuilder.addRow("Property - DELETED", entry, prop.getName(), prop.getType());
        });
        logger.info("LEAVE printDifferences");
        return tableBuilder;
    }
}
