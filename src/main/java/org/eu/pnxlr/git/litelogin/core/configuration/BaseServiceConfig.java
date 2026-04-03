package org.eu.pnxlr.git.litelogin.core.configuration;

import lombok.Getter;
import org.eu.pnxlr.git.litelogin.api.service.IService;
import org.eu.pnxlr.git.litelogin.api.service.ServiceType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

@Getter
public abstract class BaseServiceConfig implements IService {
    private final int id;
    private final String name;
    private final boolean whitelist;
    private final SkinRestorerConfig skinRestorer;

    protected BaseServiceConfig(int id, String name, boolean whitelist, SkinRestorerConfig skinRestorer) throws IOException {
        this.id = id;
        this.name = name;
        this.whitelist = whitelist;
        this.skinRestorer = skinRestorer;

        checkValid();
    }

    protected void checkValid() throws IOException {
        if (this.id > 127 || this.id < 0)
            throw new IOException(String.format(
                    "Yggdrasil id %d is out of bounds, The value can only be between 0 and "
                    , this.id
            ));
    }

    @Override
    public int getServiceId() {
        return id;
    }

    @NotNull
    @Override
    public String getServiceName() {
        return name;
    }

    @NotNull
    public abstract ServiceType getServiceType();
}
