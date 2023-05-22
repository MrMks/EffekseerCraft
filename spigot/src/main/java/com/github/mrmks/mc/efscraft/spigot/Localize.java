package com.github.mrmks.mc.efscraft.spigot;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Localize {
    private static final Pattern REGEX = Pattern.compile("%(\\d+\\$)?[\\d\\.]*[df]");
    private static final Splitter SPLITTER = Splitter.on('=').limit(2);
    private final Map<String, String> properties = new HashMap<>();

    void onLoad(InputStream stream) throws IOException {
        if (stream == null) return;

        for (String line : readLines(stream)) {
            if (!line.isEmpty() && line.charAt(0) != '#') {
                String[] pair = Iterables.toArray(SPLITTER.split(line), String.class);

                if (pair.length == 2) {
                    properties.put(pair[0], REGEX.matcher(pair[1]).replaceAll("%$1s"));
                }
            }
        }
    }

    String translate(String key, Object[] objects) {
        String value = properties.getOrDefault(key, key);
        try {
            return String.format(value, objects);
        } catch (IllegalFormatException e) {
            return "Format Error: " + value;
        }
    }

    private static List<String> readLines(InputStream stream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        return reader.lines().collect(Collectors.toList());
    }
}
