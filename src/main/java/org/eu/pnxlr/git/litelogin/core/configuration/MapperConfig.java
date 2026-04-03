package org.eu.pnxlr.git.litelogin.core.configuration;

import lombok.ToString;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * ChatSessionBlocker packet mapping configuration.
 */
@ToString
final class MapperConfig {
    private static final int MINIMUM_MAPPED_PROTOCOL = 761;
    private static final String MAPPER_NODE_NAME = "mapper";
    private static final String PACKET_ID_HEX_FORMAT = "0x%02X";

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

    private final File file;

    MapperConfig(File file) {
        this.file = file;
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

    void save() {
        try {
            Map<String, Object> mapperNode = new LinkedHashMap<>();
            for (Map.Entry<Integer, Integer> entry : packetMapping.entrySet()) {
                mapperNode.put(entry.getKey().toString(), String.format(PACKET_ID_HEX_FORMAT, entry.getValue()));
            }
            YamlConfigHelper.saveMap(file.toPath(), Map.of(MAPPER_NODE_NAME, mapperNode));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void load() {
        try {
            loadDefaults();
            Map<String, Object> mapperNode = YamlConfigHelper.getMap(YamlConfigHelper.loadMap(file.toPath()), MAPPER_NODE_NAME);
            for (Map.Entry<String, Object> entry : mapperNode.entrySet()) {
                int intValue = Integer.decode(String.valueOf(entry.getValue()));
                packetMapping.put(Integer.parseInt(entry.getKey()), intValue);
            }
        } catch (IOException e) {
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
