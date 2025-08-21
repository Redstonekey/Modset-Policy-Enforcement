package de.joancode.anticheat.config; 

import de.joancode.anticheat.Anticheat;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ConfigManager {

    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(Anticheat.MOD_ID);
    private static final Path BANNED_MODS_FILE = CONFIG_DIR.resolve("banned-mods.txt");

    public static Set<String> loadBannedMods() {
        try {
            if (Files.notExists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }
            if (Files.notExists(BANNED_MODS_FILE)) {
                // Create a default file with some examples
                Files.write(BANNED_MODS_FILE, List.of(
                        "# Add Mod IDs of cheats you want to block, one per line.",
                        "# Examples:",
                        "wurst",
                        "meteor-client",
                        "inertia",
                        "bleachhack"
                ));
            }
            return Files.readAllLines(BANNED_MODS_FILE).stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && !s.startsWith("#"))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            Anticheat.LOGGER.error("Failed to load banned mods config", e);
            return Collections.emptySet();
        }
    }
}
