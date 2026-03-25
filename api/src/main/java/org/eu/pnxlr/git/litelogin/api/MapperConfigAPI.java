package org.eu.pnxlr.git.litelogin.api;

import java.util.LinkedHashMap;
import java.util.Map;

public interface MapperConfigAPI {
    Map<Integer,Integer> getPacketMapping();
    void save();
    void reload();
}
