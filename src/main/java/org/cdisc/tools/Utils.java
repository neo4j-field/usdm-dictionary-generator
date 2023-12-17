package org.cdisc.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import net.steppschuh.markdowngenerator.table.Table;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static Table.Builder printDifferences(Map<String, ModelClass> prev, Map<String, ModelClass> curr) {
        logger.info("ENTER printDifferences");
        // Props: {ClassName: <Property Obj>}
        Map<String, List<ModelClassProperty>> newProps = new HashMap<>();
        Map<String, List<ModelClassProperty>> removedProps = new HashMap<>();
        List<ModelClass> newModelClasses = new ArrayList<>();
        List<ModelClass> removedModelClasses = new ArrayList<>();

        List<ModelClass> prevClasses = prev.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(item -> item.getValue()).toList();
        List<ModelClass> currClasses = curr.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(item -> item.getValue()).toList();

        int i = 0;
        int j = 0;

        while (i < prevClasses.size() || j < currClasses.size()) {
            // Begin by looking for changes in class name
            String prevClassName = (i < prevClasses.size()) ? prevClasses.get(i).getName() : "";
            String currClassName = (j < currClasses.size()) ? currClasses.get(j).getName() : "";

            List<ModelClassProperty> removedPropsList = new ArrayList<>();
            List<ModelClassProperty> addedPropsList = new ArrayList<>();

            if (!prevClassName.equals(currClassName)) {
                // A class name has been removed
                if ((!prevClassName.equals("") && prevClassName.compareTo(currClassName) < 0)
                        || currClassName.equals("")) {
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
            } else {
                List<ModelClassProperty> prevProperties = prevClasses.get(i).getProperties().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey()).map(item -> item.getValue()).toList();
                List<ModelClassProperty> currProperties = currClasses.get(j).getProperties().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey()).map(item -> item.getValue()).toList();

                int k = 0;
                int l = 0;

                while (k < prevProperties.size() || l < currProperties.size()) {
                    String prevPropName = (k < prevProperties.size()) ? prevProperties.get(k).getName() : "";
                    String currPropName = (l < currProperties.size()) ? currProperties.get(l).getName() : "";
                    if (!prevPropName.equals(currPropName)) {
                        // A property name has been removed
                        if ((!prevPropName.equals("") && prevPropName.compareTo(currPropName) < 0)
                                || currPropName.equals("")) {
                            removedPropsList.add(prevProperties.get(k));
                            // removedProps.put(prevClasses.get(i).getName(), prevProperties.get(k));
                            logger.debug(String.format("Property Name Removed! %1$s", prevPropName));
                            k++;
                        }
                        // A new property is present
                        else {
                            addedPropsList.add(currProperties.get(l));
                            // newProps.put(currClasses.get(j).getName(), currProperties.get(l));
                            logger.debug(String.format("New Property Name found! %1$s", currPropName));
                            l++;
                        }
                    } else {
                        k++;
                        l++;
                    }
                }
                removedProps.put(prevClasses.get(i).getName(), removedPropsList);
                newProps.put(currClasses.get(j).getName(), addedPropsList);
                i++;
                j++;
            }
        }
        Table.Builder tableBuilder = new Table.Builder()
                .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_LEFT, Table.ALIGN_LEFT, Table.ALIGN_LEFT,
                        Table.ALIGN_LEFT);
        // Prepare Alternative CSV Output
        enum outputHeaders {
            status, class_name, property_name, data_type
        }

        try {
            CSVPrinter printer = new CSVPrinter(new FileWriter("UML_DELTA.csv"), CSVFormat.RFC4180
                    .withHeader(outputHeaders.class));

            tableBuilder.addRow("Status", "Class Name", "Property Name", "Data Type");
            for (ModelClass newModelClass : newModelClasses) {// Class Row
                printer.printRecord("Class - NEW", newModelClass.getName(), null, null);
                tableBuilder.addRow("Class - NEW", newModelClass.getName(), null, null);
                for (Map.Entry<String, ModelClassProperty> propEntry : newModelClass.getProperties().entrySet()) {// Property
                                                                                                                  // Rows
                    tableBuilder.addRow(null, null, propEntry.getValue().getName(),
                            propEntry.getValue().printType());
                    printer.printRecord(null, null, propEntry.getValue().getName(),
                            propEntry.getValue().printType());
                }
            }
            for (ModelClass removedModelClass : removedModelClasses) {// Class Row
                tableBuilder.addRow("Class - DELETED", removedModelClass.getName(), null, null);
                printer.printRecord("Class - DELETED", removedModelClass.getName(), null, null);
                for (Map.Entry<String, ModelClassProperty> propEntry : removedModelClass.getProperties().entrySet()) {// Property
                                                                                                                      // Rows
                    printer.printRecord(null, null, propEntry.getValue().getName(),
                            propEntry.getValue().printType());
                    tableBuilder.addRow(null, null, propEntry.getValue().getName(),
                            propEntry.getValue().printType());
                }
            }
            tableBuilder.addRow("", null);
            printer.printRecord(null, null, null, null);
            for (Map.Entry<String, List<ModelClassProperty>> e : newProps.entrySet()) {
                String key = e.getKey();
                List<ModelClassProperty> value = e.getValue();
                for (ModelClassProperty property : value) {
                    printer.printRecord("Property - NEW", key, property.getName(), property.printType());
                    tableBuilder.addRow("Property - NEW", key, property.getName(), property.printType());
                }
            }
            for (Map.Entry<String, List<ModelClassProperty>> entry : removedProps.entrySet()) {
                String key = entry.getKey();
                List<ModelClassProperty> prop = entry.getValue();
                for (ModelClassProperty property : prop) {
                    printer.printRecord("Property - DELETED", key, property.getName(), property.printType());
                    tableBuilder.addRow("Property - DELETED", key, property.getName(), property.printType());
                }
            }
            printer.close(true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.info("LEAVE printDifferences");
        return tableBuilder;
    }

    public static void printListOfMaps(List<String> header, List<Map<String, Object>> records, String path)
            throws IOException {
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(path), CSVFormat.EXCEL)) {
            printer.printRecord(header);
            for (Map<String, Object> record : records) {
                List<Object> rec = new ArrayList<>();
                for (String column : header) {
                    rec.add(record.getOrDefault(column, ""));
                }
                printer.printRecord(rec);
            }
        }
    }
}
