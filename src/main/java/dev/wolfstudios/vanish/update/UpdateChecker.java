package dev.wolfstudios.vanish.update;

import dev.wolfstudios.vanish.LogManager;
import dev.wolfstudios.vanish.VanishPlugin;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class UpdateChecker {

    private static final String GITHUB_API = "https://api.github.com/repos/EfraGamer300/GerenciarVanish/releases/latest";
    private static final String GITHUB_DL = "https://github.com/EfraGamer300/GerenciarVanish/releases/latest";

    private final VanishPlugin plugin;
    private final LogManager log;
    private final String currentVersion;

    public UpdateChecker(VanishPlugin plugin, LogManager log) {
        this.plugin = plugin;
        this.log = log;
        this.currentVersion = plugin.getDescription().getVersion();
    }

    public void checkAsync() {
        var client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        var request = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_API))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "VanishPlus/" + currentVersion)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        handleResponse(response.body());
                    } else if (response.statusCode() == 404) {
                        plugin.getLogger().info("[Vanish+] Nenhuma release encontrada no GitHub ainda.");
                        log.log("UPDATE", "GitHub", "Nenhuma release encontrada (404)");
                    } else {
                        plugin.getLogger().warning("[Vanish+] Falha ao verificar update. HTTP " + response.statusCode());
                        log.log("UPDATE", "GitHub", "Falha HTTP " + response.statusCode());
                    }
                })
                .exceptionally(t -> {
                    plugin.getLogger().warning("[Vanish+] Nao foi possivel verificar atualizacao: " + t.getMessage());
                    log.log("UPDATE", "GitHub", "Erro: " + t.getMessage());
                    return null;
                });
    }

    private void handleResponse(InputStream body) {
        try {
            String json = new String(body.readAllBytes());
            String tag = extractTag(json);
            if (tag == null) return;

            String latest = tag.replaceAll("^v", "");
            if (!latest.equals(currentVersion) && !latest.isEmpty()) {
                plugin.getLogger().info("");
                plugin.getLogger().info("§a[Vanish+] §eNova versao disponivel: §b" + latest + " §e(voce esta na §c" + currentVersion + "§e)");
                plugin.getLogger().info("§a[Vanish+] §7Baixe em: §f" + GITHUB_DL);
                plugin.getLogger().info("");
                log.log("UPDATE", "GitHub", "Nova versao: " + latest + " (atual: " + currentVersion + ")");
            }
        } catch (IOException e) {
            plugin.getLogger().warning("[Vanish+] Erro ao ler resposta do update: " + e.getMessage());
        }
    }

    private String extractTag(String json) {
        try {
            int tagIndex = json.indexOf("\"tag_name\"");
            if (tagIndex == -1) return null;
            int start = json.indexOf("\"", tagIndex + 11) + 1;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }
}
