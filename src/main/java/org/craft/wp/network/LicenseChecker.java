package org.craft.wp.network;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class LicenseChecker {

    private static final String API_URL = "https://gvpvdwfzfaefzhqagfbi.supabase.co/functions/v1/license-api/verify/dropper";

    public static boolean verifyLicense(String licenseKey) {
        if (licenseKey == null || licenseKey.trim().isEmpty()) {
            return false;
        }

        try {
            String hwid = HardwareUtils.getHardwareId();

            if (hwid == null) {
                Bukkit.getLogger().severe("ERRO CRÍTICO: Não foi possível identificar o hardware do servidor.");
                Bukkit.getLogger().severe("Entre em contato com o suporte para verificar as permissões do sistema.");
                return false;
            }

            JsonObject jsonBody = new JsonObject();
            jsonBody.addProperty("key", licenseKey.trim());
            jsonBody.addProperty("hwid", hwid);

            String jsonPayload = jsonBody.toString();

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("X-Plugin-Token", "VtfmiajXm74jXXvQbSBY")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                return body.contains("\"valid\":true");
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("Entre em contato com o suporte para verificar as permissões do sistema.");
        }

        return false;
    }

}
