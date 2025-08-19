package me.lyrica.managers;

import com.google.gson.*;
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import me.lyrica.Lyrica;
import me.lyrica.modules.Module;
import me.lyrica.settings.Setting;
import me.lyrica.settings.impl.*;
import me.lyrica.utils.minecraft.IdentifierUtils;
import me.lyrica.utils.system.FileUtils;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.StringJoiner;

@Getter @Setter
public class ConfigManager {
    private String currentConfig = "default";

    public ConfigManager() {
        loadConfig();
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveConfig));
    }

    public void loadConfig() {
        try {
            FileUtils.createDirectory(Lyrica.MOD_NAME);
            FileUtils.createDirectory(Lyrica.MOD_NAME + "/Configs");
            FileUtils.createDirectory(Lyrica.MOD_NAME + "/Client");

            loadGeneral();
            loadWaypoints();
            loadModules(currentConfig);
        } catch (IOException exception) {
            Lyrica.LOGGER.error("Failed to load the client's configuration!", exception);
            Lyrica.CHAT_MANAGER.await("The configuration has not been loaded properly. Read the stacktrace for more information.");
        }
    }

    public void saveConfig() {
        try {
            FileUtils.createDirectory(Lyrica.MOD_NAME);
            FileUtils.createDirectory(Lyrica.MOD_NAME + "/Configs");
            FileUtils.createDirectory(Lyrica.MOD_NAME + "/Client");

            saveGeneral();
            saveWaypoints();
            saveModules(currentConfig);
        } catch (IOException exception) {
            Lyrica.LOGGER.error("Failed to save the client's configuration!", exception);
        }
    }

    public void loadWaypoints() throws IOException {
        if (!FileUtils.fileExists(Lyrica.MOD_NAME + "/Waypoints.json")) return;
        @Cleanup InputStream stream = Files.newInputStream(Paths.get(Lyrica.MOD_NAME + "/Waypoints.json"));

        JsonObject configObject;
        try {
            configObject = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
        } catch (IllegalStateException exception) {
            Lyrica.LOGGER.error("Failed to load the client's Waypoint configuration!", exception);
            Lyrica.CHAT_MANAGER.await("The Waypoint configuration has not been loaded properly. Read the stacktrace for more information.");
            return;
        }

        if(configObject.has("Waypoints")) {
            for(JsonElement element : configObject.get("Waypoints").getAsJsonArray()) {
                String[] args = element.getAsString().split(":");
                if(Lyrica.WAYPOINT_MANAGER.contains(args[0])) continue;
                Lyrica.WAYPOINT_MANAGER.add(args[0], new Vec3d(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3])), args[4], args[5]);
            }
        }
    }

    public void loadGeneral() throws IOException {
        if (!FileUtils.fileExists(Lyrica.MOD_NAME + "/General.json")) return;
        @Cleanup InputStream stream = Files.newInputStream(Paths.get(Lyrica.MOD_NAME + "/General.json"));

        JsonObject configObject;
        try {
            configObject = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
        } catch (IllegalStateException exception) {
            Lyrica.LOGGER.error("Failed to load the client's General configuration!", exception);
            Lyrica.CHAT_MANAGER.await("The General configuration has not been loaded properly. Read the stacktrace for more information.");
            return;
        }

        if (configObject.has("Config")) currentConfig = configObject.get("Config").getAsString();
        if (configObject.has("Prefix")) Lyrica.COMMAND_MANAGER.setPrefix(configObject.get("Prefix").getAsString());
        if (configObject.has("Friends")) {
            for (JsonElement element : configObject.get("Friends").getAsJsonArray()) {
                if (Lyrica.FRIEND_MANAGER.contains(element.getAsString())) continue;
                Lyrica.FRIEND_MANAGER.add(element.getAsString());
            }
        }

        if (configObject.has("Macros")) {
            for (JsonElement element : configObject.get("Macros").getAsJsonArray()) {
                try {
                    String[] split = element.getAsString().split(":", 2);
                    if (split.length <= 1) continue;

                    int key = Integer.parseInt(split[0]);
                    String message = split[1];

                    Lyrica.MACRO_MANAGER.add(message, key);
                } catch (NumberFormatException exception) {
                    Lyrica.LOGGER.error("Failed to load the " + element.getAsString() + " macro!", exception);
                    Lyrica.CHAT_MANAGER.await("The " + element.getAsString() + " macro has failed to load. Read the stacktrace for more information.");
                    continue;
                }
            }
        }
    }

    public void saveGeneral() throws IOException {
        FileUtils.resetFile(Lyrica.MOD_NAME + "/General.json");

        JsonObject configObject = new JsonObject();
        configObject.add("Config", new JsonPrimitive(currentConfig));
        configObject.add("Prefix", new JsonPrimitive(Lyrica.COMMAND_MANAGER.getPrefix()));

        JsonArray friendsArray = new JsonArray();
        Lyrica.FRIEND_MANAGER.getFriends().forEach(friendsArray::add);
        configObject.add("Friends", friendsArray);

        JsonArray macroArray = new JsonArray();
        Lyrica.MACRO_MANAGER.getMacros().forEach((key, value) -> {
            macroArray.add(value + ":" + key);
        });
        configObject.add("Macros", macroArray);

        @Cleanup OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(Lyrica.MOD_NAME + "/General.json"), StandardCharsets.UTF_8);
        writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(JsonParser.parseString(configObject.toString())));
    }

    public void saveWaypoints() throws IOException {
        FileUtils.resetFile(Lyrica.MOD_NAME + "/Waypoints.json");

        JsonObject configObject = new JsonObject();

        JsonArray waypointArray = new JsonArray();
        for(WaypointManager.Waypoint waypoint : Lyrica.WAYPOINT_MANAGER.getWaypoints()) {
            waypointArray.add(waypoint.getName() + ":" + (int)waypoint.getPos().x + ":" + (int)waypoint.getPos().y + ":" + (int)waypoint.getPos().z + ":" + waypoint.getDimension() + ":" + waypoint.getServer());
        }
        configObject.add("Waypoints", waypointArray);

        @Cleanup OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(Lyrica.MOD_NAME + "/Waypoints.json"), StandardCharsets.UTF_8);
        writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(JsonParser.parseString(configObject.toString())));
    }

    public void loadModules(String config) throws IOException {
        if (!FileUtils.fileExists(Lyrica.MOD_NAME + "/Configs/" + config + ".json")) return;
        @Cleanup InputStream stream = Files.newInputStream(Paths.get(Lyrica.MOD_NAME + "/Configs/" + config + ".json"));

        JsonObject configObject;
        try {
            configObject = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
        } catch (IllegalStateException exception) {
            Lyrica.LOGGER.error("Failed to load the client's Module configuration!", exception);
            Lyrica.CHAT_MANAGER.await("The configuration for the Modules has not been loaded properly. Read the stacktrace for more information.");
            return;
        }

        if (!configObject.has("Modules")) return;
        JsonObject modulesObject = configObject.get("Modules").getAsJsonObject();

        for (Module module : Lyrica.MODULE_MANAGER.getModules()) {
            if (!modulesObject.has(module.getName())) {
                module.setToggled(false);
                module.resetValues();
                continue;
            }

            JsonObject moduleObject = modulesObject.get(module.getName()).getAsJsonObject();

            module.setToggled(moduleObject.has("Status") && moduleObject.get("Status").getAsBoolean(), false);

            if (!moduleObject.has("Settings")){
                module.resetValues();
                continue;
            }

            JsonObject settingsObject = moduleObject.get("Settings").getAsJsonObject();

            for (Setting uncastedSetting : module.getSettings()) {
                JsonElement valueObject = settingsObject.get(uncastedSetting.getName());
                if (valueObject == null || !valueObject.isJsonPrimitive()) {
                    switch (uncastedSetting) {
                        case BooleanSetting setting -> setting.resetValue();
                        case NumberSetting setting -> setting.resetValue();
                        case ModeSetting setting -> setting.resetValue();
                        case StringSetting setting -> setting.resetValue();
                        case BindSetting setting -> setting.resetValue();
                        case ColorSetting setting -> setting.resetValue();
                        case WhitelistSetting setting -> setting.clear();
                        default -> {}
                    }

                    continue;
                }

                switch (uncastedSetting) {
                    case BooleanSetting setting -> setting.setValue(valueObject.getAsBoolean());
                    case NumberSetting setting -> setting.setValue(valueObject.getAsNumber());
                    case ModeSetting setting -> setting.setValue(valueObject.getAsString());
                    case StringSetting setting -> setting.setValue(valueObject.getAsString());
                    case BindSetting setting -> {
                        String bindData = valueObject.getAsString();
                        if (bindData.contains(",")) {
                            // Yeni format (bind değeri,mode)
                            String[] data = bindData.split(",");
                            setting.setValue(Integer.parseInt(data[0]));
                            if (data.length > 1) {
                                if (data[1].equals("HOLD")) {
                                    setting.setBindMode(BindSetting.BindMode.HOLD);
                                } else {
                                    setting.setBindMode(BindSetting.BindMode.TOGGLE);
                                }
                            }
                        } else {
                            // Eski format (sadece bind değeri)
                            setting.setValue(valueObject.getAsInt());
                        }
                    }
                    case ColorSetting setting -> {
                        String[] data = valueObject.getAsString().split(",");
                        if (data.length != 6) continue;

                        setting.setColor(new Color(Math.clamp(Integer.parseInt(data[0]), 0, 255), Math.clamp(Integer.parseInt(data[1]), 0, 255), Math.clamp(Integer.parseInt(data[2]), 0, 255), Math.clamp(Integer.parseInt(data[3]), 0, 255)));
                        setting.setSync(Boolean.parseBoolean(data[4]));
                        setting.setRainbow(Boolean.parseBoolean(data[5]));
                    }
                    case WhitelistSetting setting -> {
                        String[] data = valueObject.getAsString().split(",");
                        if (data.length == 0) continue;

                        for (String object : data) {
                            if (setting.isWhitelistContains(object)) continue;

                            if (setting.getType() == WhitelistSetting.Type.ITEMS) {
                                Item item = IdentifierUtils.getItem(object);
                                if (item == null) continue;

                                setting.add(item);
                            } else if (setting.getType() == WhitelistSetting.Type.BLOCKS) {
                                Block block = IdentifierUtils.getBlock(object);
                                if (block == null) continue;

                                setting.add(block);
                            }
                        }
                    }
                    default -> {}
                }
            }
        }

        this.currentConfig = config;
    }

    public void saveModules(String config) throws IOException {
        FileUtils.resetFile(Lyrica.MOD_NAME + "/Configs/" + config + ".json");

        JsonObject configObject = new JsonObject();
        configObject.add("Config", new JsonPrimitive(config));

        JsonObject modulesObject = new JsonObject();
        for (Module module : Lyrica.MODULE_MANAGER.getModules()) {
            JsonObject moduleObject = new JsonObject();
            moduleObject.add("Status", new JsonPrimitive(module.isToggled()));

            JsonObject settingsObject = new JsonObject();
            for (Setting uncastedSetting : module.getSettings()) {
                switch (uncastedSetting) {
                    case BooleanSetting setting -> settingsObject.add(setting.getName(), new JsonPrimitive(setting.getValue()));
                    case NumberSetting setting -> settingsObject.add(setting.getName(), new JsonPrimitive(setting.getValue()));
                    case ModeSetting setting -> settingsObject.add(setting.getName(), new JsonPrimitive(setting.getValue()));
                    case StringSetting setting -> settingsObject.add(setting.getName(), new JsonPrimitive(setting.getValue()));
                    case BindSetting setting -> settingsObject.add(setting.getName(), new JsonPrimitive(setting.getValue() + "," + setting.getBindMode().name()));
                    case ColorSetting setting -> settingsObject.add(setting.getName(), new JsonPrimitive(setting.getValue().getColor().getRed() + "," + setting.getValue().getColor().getGreen() + "," + setting.getValue().getColor().getBlue() + "," + setting.getValue().getColor().getAlpha() + "," + setting.isSync() + "," + setting.isRainbow()));
                    case WhitelistSetting setting -> {
                        StringJoiner objects = new StringJoiner(",");
                        for (String id : setting.getWhitelistIds()) objects.add(id);
                        settingsObject.add(setting.getName(), new JsonPrimitive(objects.toString()));
                    }
                    default -> {}
                }
            }

            moduleObject.add("Settings", settingsObject);
            modulesObject.add(module.getName(), moduleObject);
        }

        configObject.add("Modules", modulesObject);

        @Cleanup OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(Lyrica.MOD_NAME + "/Configs/" + config + ".json"), StandardCharsets.UTF_8);
        writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(JsonParser.parseString(configObject.toString())));

        this.currentConfig = config;
    }
}