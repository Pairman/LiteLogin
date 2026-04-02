package org.eu.pnxlr.git.litelogin.core.configuration;

import lombok.ToString;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.util.*;

/**
 * ChatSessionBlocker packet mapping configuration.
 */
@ToString
final class MapperConfig {
    private static final int MINIMUM_MAPPED_PROTOCOL = 761;
    private final TreeMap<Integer,Integer> packetMapping = new TreeMap<>() {
        @Override
        public Integer put(Integer key, Integer value) {
            if(key<MINIMUM_MAPPED_PROTOCOL) return value;
            if(this.containsValue(value)) {
                Integer existingKey = findKeyByValue(value);
                if (existingKey != null && existingKey > key) {
                    super.remove(existingKey);
                    super.put(key, value);
                }
                return value;
            }
            return super.put(key,value);
        }
        private Integer findKeyByValue(Integer value) {
            for (Map.Entry<Integer, Integer> entry : this.entrySet()) {
                if (entry.getValue().equals(value)) {
                    return entry.getKey();
                }
            }
            return null;
        }
    };
    private final Map<Integer, Integer> packetMappingView = Collections.unmodifiableNavigableMap(packetMapping);

    private final File dataFolder;
    MapperConfig(File dataFolder) {
        this.dataFolder = dataFolder;
        loadDefaults();
    }

    public Map<Integer, Integer> getPacketMapping() {
        return packetMappingView;
    }

    public boolean persistPacketMapping(int protocol, int packetId) {
        Map<Integer, Integer> oldMapping = new TreeMap<>(packetMapping);
        packetMapping.put(protocol, packetId);
        if (packetMapping.equals(oldMapping)) {
            return false;
        }
        save();
        return true;
    }

    private void save() {
        try {
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder().file(new File(dataFolder, "mapper.yml")).indent(2).build();
            CommentedConfigurationNode rootNode = loader.load();
            CommentedConfigurationNode mapperNode = rootNode.node("mapper");
            for (Map.Entry<Integer, Integer> entry : packetMapping.entrySet()) {
                mapperNode.node(entry.getKey().toString()).set(String.format("0x%02X", entry.getValue()));
            }
            loader.save(rootNode);
        } catch (ConfigurateException e) {
            throw new RuntimeException(e);
        }
    }

    void reload() {
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder().file(new File(dataFolder, "mapper.yml")).build();
        try {
            loadDefaults();
            ConfigurationNode mapperNode = loader.load().node("mapper");
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : mapperNode.childrenMap().entrySet()) {
                String key = entry.getKey().toString();
                String hexValue = entry.getValue().getString();
                if (hexValue != null) {
                    int intValue = Integer.decode(hexValue);
                    packetMapping.put(Integer.parseInt(key), intValue);
                }
            }
        } catch (ConfigurateException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadDefaults() {
        packetMapping.clear();
        packetMapping.put(761,0x20);
        packetMapping.put(762,0x06);
        packetMapping.put(765,0x07);
        packetMapping.put(768,0x08);
        packetMapping.put(771,0x09);
    }
}
