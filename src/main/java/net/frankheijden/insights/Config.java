package net.frankheijden.insights;

import org.bukkit.Bukkit;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

public class Config {
    private Insights plugin;
    private YamlConfiguration config;

    public boolean GENERAL_UPDATES_CHECK = true;
    public boolean GENERAL_UPDATES_DOWNLOAD = true;
    public boolean GENERAL_DEBUG = false;
    public int GENERAL_LIMIT = -1;
    public boolean GENERAL_WORLDS_WHITELIST = true;
    public List<String> GENERAL_WORLDS_LIST = Arrays.asList("world", "world_nether", "world_the_end");
    public boolean GENERAL_REGIONS_WHITELIST = false;
    public List<String> GENERAL_REGIONS_LIST;
    public Map<String, Boolean> GENERAL_REGION_BLOCKS_WHITELIST;
    public Map<String, List<String>> GENERAL_REGION_BLOCKS_LIST;
    public String GENERAL_NOTIFICATION_TYPE = "BOSSBAR";
    private List<String> GENERAL_NOTIFICATION_TYPE_VALUES = Arrays.asList("BOSSBAR", "ACTIONBAR");
    public String GENERAL_NOTIFICATION_BOSSBAR_COLOR = "BLUE";
    private List<String> GENERAL_NOTIFICATION_BOSSBAR_COLOR_VALUES = Arrays.asList("BLUE", "GREEN", "PINK", "PURPLE", "RED", "WHITE", "YELLOW");
    public String GENERAL_NOTIFICATION_BOSSBAR_STYLE = "SEGMENTED_10";
    private List<String> GENERAL_NOTIFICATION_BOSSBAR_STYLE_VALUES = Arrays.asList("SOLID", "SEGMENTED_6", "SEGMENTED_10", "SEGMENTED_12", "SEGMENTED_20");
    public List<String> GENERAL_NOTIFICATION_BOSSBAR_FLAGS = Collections.emptyList();
    private List<String> GENERAL_NOTIFICATION_BOSSBAR_FLAGS_VALUES = Arrays.asList("DARKEN_SKY", "PLAY_BOSS_MUSIC", "CREATE_FOG");
    public int GENERAL_NOTIFICATION_BOSSBAR_DURATION = 60;
    public int GENERAL_SCANRADIUS_DEFAULT = 5;
    public boolean GENERAL_SCAN_NOTIFICATION = true;
    public boolean GENERAL_ALWAYS_SHOW_NOTIFICATION = true;
    public Map<String, Integer> GENERAL_MATERIALS;
    public Map<String, Map<String, Integer>> GENERAL_GROUPS;

    public Config(Insights plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            Bukkit.getLogger().info("[Insights] config.yml not found, creating!");
            plugin.saveDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        GENERAL_UPDATES_CHECK = config.getBoolean("general.updates.check");
        GENERAL_UPDATES_DOWNLOAD = config.getBoolean("general.updates.download");
        GENERAL_DEBUG = config.getBoolean("general.debug");

        int generalLimit = config.getInt("general.limit");
        if (generalLimit > -1) {
            GENERAL_LIMIT = generalLimit;
        } else {
            System.out.println("[Insights/Config] Chunk tile limit was chosen not to be enabled.");
        }
        GENERAL_WORLDS_WHITELIST = config.getBoolean("general.worlds.whitelist");
        GENERAL_WORLDS_LIST = config.getStringList("general.worlds.list");

        GENERAL_REGION_BLOCKS_WHITELIST = new HashMap<>();
        GENERAL_REGION_BLOCKS_LIST = new HashMap<>();
        MemorySection regions = (MemorySection) config.get("general.region_blocks");
        if (regions != null) {
            for (String region : regions.getKeys(false)) {
                GENERAL_REGION_BLOCKS_WHITELIST.put(region, regions.getBoolean(region + ".whitelist"));
                //                    Material material;
                //                    try {
                //                        material = Material.valueOf(materialString);
                //                    } catch (Exception ex) {
                //                        System.err.println("[Insights/Config] Invalid configuration in config.yml at path 'general.region_blocks.list', invalid material '" + materialString + "'!");
                //                        continue;
                //                    }
                List<String> materials = new ArrayList<>(regions.getStringList(region + ".list"));
                GENERAL_REGION_BLOCKS_LIST.put(region, materials);
            }
        } else {
            System.err.println("[Insights/Config] Configuration section in config.yml not found at path 'general.region_blocks'!");
        }

        updateString("general.notification.type", GENERAL_NOTIFICATION_TYPE_VALUES);
        updateString("general.notification.bossbar.color", GENERAL_NOTIFICATION_BOSSBAR_COLOR_VALUES);
        updateString("general.notification.bossbar.style", GENERAL_NOTIFICATION_BOSSBAR_STYLE_VALUES);

        List<String> bossbarFlags = config.getStringList("general.notification.bossbar.flags");
        bossbarFlags.removeIf(flag -> !GENERAL_NOTIFICATION_BOSSBAR_FLAGS_VALUES.contains(flag.toUpperCase()));
        GENERAL_NOTIFICATION_BOSSBAR_FLAGS = bossbarFlags;

        updateInt("general.notification.bossbar.duration", 1);
        updateInt("general.scanradius_default", 1);

        GENERAL_SCAN_NOTIFICATION = config.getBoolean("general.scan_notification");
        GENERAL_ALWAYS_SHOW_NOTIFICATION = config.getBoolean("general.always_show_notification");

        GENERAL_MATERIALS = new HashMap<>();
        MemorySection materials = (MemorySection) config.get("general.materials");
        if (materials != null) {
            for (String materialString : materials.getKeys(false)) {
//                Material material;
//                try {
//                    material = Material.valueOf(materialString);
//                } catch (Exception ex) {
//                    System.err.println("[Insights/Config] Invalid configuration in config.yml at path 'general.materials', invalid material '" + materialString + "'!");
//                    continue;
//                }

                int value = materials.getInt(materialString);
                if (value >= 0) {
                    GENERAL_MATERIALS.put(materialString, value);
                } else {
                    System.err.println("[Insights/Config] Invalid configuration in config.yml at path 'general.materials." + materialString + "', value must be at least 0!");
                }
            }
        } else {
            System.err.println("[Insights/Config] Configuration section in config.yml not found at path 'general.materials'!");
        }

        GENERAL_GROUPS = new HashMap<>();
        MemorySection groups = (MemorySection) config.get("general.groups");
        if (groups != null) {
            for (String permission : groups.getKeys(false)) {
                HashMap<String, Integer> groupMap = new HashMap<>();

                MemorySection group = (MemorySection) groups.get(permission);
                for (String materialString : group.getKeys(false)) {
                    int value = group.getInt(materialString);
                    if (value >= 0) {
                        groupMap.put(materialString, value);
                    } else {
                        System.err.println("[Insights/Config] Invalid configuration in config.yml at path 'general.groups." + group + "." + materialString + "', value must be at least 0!");
                    }
                }

                GENERAL_GROUPS.put(permission.replace("{dot}", "."), groupMap);
            }
        } else {
            System.err.println("[Insights/Config] Configuration section in config.yml not found at path 'general.groups'!");
        }

        GENERAL_REGIONS_WHITELIST = config.getBoolean("general.regions.whitelist");
        GENERAL_REGIONS_LIST = config.getStringList("general.regions.list");
    }

    private void updateInt(String path, int min) {
        String fieldName = path.replace(".", "_").toUpperCase();
        int i = config.getInt(path);
        if (i >= min) {
            try {
                Class<?> configClass = this.getClass();
                Field field = configClass.getDeclaredField(fieldName);
                field.set(this, i);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            System.err.println("[Insights/Config] Invalid configuration in config.yml at path '" + path + "', value must be at least " + min + "!");
        }
    }

    private void updateString(String path, List<String> possibleValues) {
        String fieldName = path.replace(".", "_").toUpperCase();
        String s = config.getString(path);
        if (s != null && possibleValues.contains(s.toUpperCase())) {
            try {
                Class<?> configClass = this.getClass();
                Field field = configClass.getDeclaredField(fieldName);
                field.set(this, s);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            StringBuilder sb = new StringBuilder();
            for (String value : possibleValues) {
                if (possibleValues.indexOf(value) + 1 == possibleValues.size()) {
                    sb.append(" or \"").append(value).append("\"");
                } else {
                    sb.append("\"").append(value).append("\", ");
                }
            }
            System.err.println("[Insights/Config] Invalid configuration in config.yml at path '" + path + "', value must be " + sb.toString() + "!");
        }
    }
}
