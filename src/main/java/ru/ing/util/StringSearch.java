package ru.ing.util;

import ru.ing.model.Key;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class StringSearch {

    public static final Pattern VALID_LINE_PATTERN = Pattern.compile("^(\"[^\"]*(\"{2}[^\"]*)*\";)*\"[^\"]*(\"{2}[^\"]*)*\"$");

    public void processData(String fileName) throws IOException {
        List<List<String>> records = readDataFromFile(fileName);
        Map<Integer, Set<List<String>>> uniqueRecords = groupUniqueRecords(records);
        Map<Integer, Set<List<String>>> sortedUniqueRecords = sortMapInDscOrder(uniqueRecords);
        writeResultsToFile(sortedUniqueRecords);
    }

    private static List<List<String>> readDataFromFile(String fileName) throws IOException {
        List<List<String>> records = new ArrayList<>();

        try (FileInputStream fileInputStream = new FileInputStream(fileName);
             GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);
             InputStreamReader inputStreamReader = new InputStreamReader(gzipInputStream);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (isValidLine(line)) {
                    List<String> recordValues = Arrays.asList(line.split(";"));
                    records.add(recordValues);
                } else {
                    System.out.println("Некорректная строка пропущена " + line);
                }
            }
        }

        return records.stream().distinct().toList();
    }

    private static boolean isValidLine(String line) {
        return VALID_LINE_PATTERN.matcher(line).matches();
    }

    private static List<String> findLongestRecord(List<List<String>> records) {
        List<String> longest = null;

        for (List<String> record : records) {
            if (longest == null || record.size() > longest.size()) {
                longest = record;
            }
        }

        return longest;
    }

    private static Map<Integer, Set<List<String>>> groupUniqueRecords(List<List<String>> uniqueRecords) {
        Map<Key, Integer> groupMapping = new HashMap<>();
        Map<Integer, Set<List<String>>> groupedData = new HashMap<>();

        int groupNumber = 0;
        List<String> longestRecord = findLongestRecord(uniqueRecords);

        Set<List<String>> initialGroup = new HashSet<>();
        initialGroup.add(longestRecord);
        groupedData.put(groupNumber, initialGroup);

        for (int i = 0; i < longestRecord.size(); i++) {
            Key key = new Key(longestRecord.get(i), i);
            groupMapping.put(key, groupNumber);
        }
        groupNumber++;

        for (List<String> record : uniqueRecords) {
            boolean matched = false;

            for (int i = 0; i < record.size(); i++) {
                Key key = new Key(record.get(i), i);

                if (groupMapping.containsKey(key)) {
                    int groupIndex = groupMapping.get(key);
                    Set<List<String>> group = groupedData.get(groupIndex);
                    group.add(record);

                    for (int j = 0; j < record.size(); j++) {
                        Key keyExtra = new Key(record.get(j), j);
                        groupMapping.put(keyExtra, groupIndex);
                    }

                    matched = true;
                    break;
                }
            }

            if (!matched) {
                Set<List<String>> newGroup = new HashSet<>();
                newGroup.add(record);
                groupedData.put(groupNumber, newGroup);
                for (int i = 0; i < record.size(); i++) {
                    Key key = new Key(record.get(i), i);
                    groupMapping.put(key, groupNumber);
                }
                groupNumber++;
            }
        }

        return groupedData;
    }

    private static Map<Integer, Set<List<String>>> sortMapInDscOrder(Map<Integer, Set<List<String>>> groupedData) {
        List<Map.Entry<Integer, Set<List<String>>>> entryList = new ArrayList<>(groupedData.entrySet());

        entryList.sort((entry1, entry2) -> {
            int size1 = entry1.getValue().size();
            int size2 = entry2.getValue().size();
            return Integer.compare(size2, size1);
        });

        Map<Integer, Set<List<String>>> sortedData = new LinkedHashMap<>();
        for (Map.Entry<Integer, Set<List<String>>> entry : entryList) {
            sortedData.put(entry.getKey(), entry.getValue());
        }

        return sortedData;
    }


    private static int countGroupsWithMultipleRecords(Map<Integer, Set<List<String>>> groupedData) {
        int groupCount = 0;
        for (Map.Entry<Integer, Set<List<String>>> entry : groupedData.entrySet()) {
            if (entry.getValue().size() > 1) {
                groupCount++;
            }
        }
        return groupCount;
    }

    private static void writeResultsToFile(Map<Integer, Set<List<String>>> simplifiedGroups) throws IOException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmm");
        String filename = "output_" + dateFormat.format(new Date()) + ".txt";

        FileWriter writer = new FileWriter(filename);

        int groupCount = countGroupsWithMultipleRecords(simplifiedGroups);

        writer.write("Групп с более чем одной записью: " + groupCount);
        writer.write("\n-------------------------");

        for (Map.Entry<Integer, Set<List<String>>> entry : simplifiedGroups.entrySet()) {
            int groupId = entry.getKey();
            Set<List<String>> groupRecords = entry.getValue();

            writer.write("\nГруппа " + groupId + ":\n");

            for (List<String> record : groupRecords) {
                String formattedRecord = String.join(";", record);

                writer.write(formattedRecord);
                writer.write("\n");
            }
        }
        writer.close();
    }

}

