package com.test.orientdb;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class DockerUtils {
    public static void start() {
        try {
            Process proc = Runtime.getRuntime().exec(new String[]{"docker-compose", "up", "-d"});
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.err.println(line);
                }
            }
            Thread.sleep(6000);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void stop() {
        try {
            Runtime.getRuntime().exec(new String[]{"docker-compose", "down"}).waitFor();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
