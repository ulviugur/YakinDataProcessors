package com.langpack.common;

import java.util.HashMap;
import java.util.Map;

public class CommandLineArgs {

    private final Map<String, String> arguments = new HashMap<>();

    public CommandLineArgs(String[] args) {
        parseArguments(args);
    }

    private void parseArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                String key = args[i];
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    String value = args[++i];
                    arguments.put(key, value);
                } else {
                    // Handle case where no value is provided after the key
                    arguments.put(key, null);
                }
            } else {
                System.err.println("Unknown option: " + args[i]);
            }
        }
    }

    public String get(String key) {
        return arguments.get(key);
    }

    public boolean has(String key) {
        return arguments.containsKey(key);
    }

    public Map<String, String> getAllArguments() {
        return new HashMap<>(arguments);
    }

    public static void main(String[] args) {
        CommandLineArgs cmdArgs = new CommandLineArgs(args);

        // Print all parsed arguments
        System.out.println("Parsed Arguments:");
        for (Map.Entry<String, String> entry : cmdArgs.getAllArguments().entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }
}
