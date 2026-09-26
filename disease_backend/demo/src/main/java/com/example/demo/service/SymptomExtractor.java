package com.example.demo.service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class SymptomExtractor {
    private final Map<String, List<String>> synonyms;
    private final Set<String> colsNorm;
    private final Map<String, String> normToOriginalCol;
    public SymptomExtractor() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            try (InputStream is = new ClassPathResource("symptom_columns.json").getInputStream()) {
                List<String> cols = mapper.readValue(is, new TypeReference<>() {});
                colsNorm = new HashSet<>();
                normToOriginalCol = new HashMap<>();
                for (String c : cols) {
                    String n = norm(c);
                    if (!n.isBlank()) {
                        colsNorm.add(n);
                        normToOriginalCol.put(n, c);
                    }
                }
            }
            try (InputStream is = new ClassPathResource("symptom_synonyms.json").getInputStream()) {
                synonyms = mapper.readValue(is, new TypeReference<>() {});
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load symptom resources", e);
        }
    }
    private String norm(String s) {
        if (s == null) return "";
        String t = s.toLowerCase(Locale.ROOT).trim();
        t = t.replace("_", " ");
        t = t.replaceAll("[^a-z0-9\\s]", " ");
        t = t.replaceAll("\\s+", " ");
        return t;
    }
    private boolean containsWholePhrase(String normalizedText, String normalizedPhrase) {
        if (normalizedPhrase == null) return false;
        String p = normalizedPhrase.trim();
        if (p.isEmpty()) return false;
        if (p.length() < 3) return false;
        String regex = "(^|\\s)" + Pattern.quote(p) + "(\\s|$)";
        return Pattern.compile(regex).matcher(normalizedText).find();
    }
    private Set<String> directMatch(String text) {
        String t = norm(text);
        Set<String> found = new HashSet<>();
        List<String> sortedCols = new ArrayList<>(colsNorm);
        sortedCols.sort((a, b) -> Integer.compare(b.length(), a.length()));

        for (String colN : sortedCols) {
            if (containsWholePhrase(t, colN)) {
                found.add(normToOriginalCol.get(colN));
            }
        }
        return found;
    }
    private Set<String> synonymMatch(String text) {
        String t = norm(text);
        Set<String> found = new HashSet<>();
        for (Map.Entry<String, List<String>> e : synonyms.entrySet()) {
            String symptomKey = e.getKey();
            String symptomKeyNorm = norm(symptomKey);
            List<String> phrases = new ArrayList<>(e.getValue() == null ? List.of() : e.getValue());
            phrases.sort((a, b) -> Integer.compare(norm(b).length(), norm(a).length()));
            for (String phrase : phrases) {
                String pNorm = norm(phrase);
                if (containsWholePhrase(t, pNorm)) {
                    String col = normToOriginalCol.getOrDefault(symptomKeyNorm, symptomKey);
                    found.add(col);
                    break;
                }}}
        return found;
    }
    public Set<String> extract(String text) {
        if (text == null || text.isBlank()) return Set.of();

        Set<String> direct = directMatch(text);
        if (!direct.isEmpty()) return direct;

        return synonymMatch(text);
    }
}