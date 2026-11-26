package org.craft.wp.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class HardwareUtils {

    public static String getHardwareId() {
        try {
            StringBuilder sb = new StringBuilder();

            sb.append(executeRegQuery("HKLM\\SOFTWARE\\Microsoft\\Cryptography", "MachineGuid"));

            sb.append(executeRegQuery("HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0", "ProcessorNameString"));

            if (sb.isEmpty() || sb.toString().trim().isEmpty()) {
                return null;
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String executeRegQuery(String path, String key) {
        try {
            ProcessBuilder pb = new ProcessBuilder("reg", "query", path, "/v", key);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(key)) {
                        String[] parts = line.split("\\s{2,}");
                        if (parts.length >= 3) {
                            return parts[parts.length - 1].trim();
                        }
                    }
                }
            }
            process.waitFor();
        } catch (Exception ignored) {}
        return "";
    }
}
