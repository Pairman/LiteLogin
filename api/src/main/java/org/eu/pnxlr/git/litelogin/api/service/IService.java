package org.eu.pnxlr.git.litelogin.api.service;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an authentication service.
 */
@ApiStatus.NonExtendable
public interface IService {

    /**
     * Returns this authentication service ID.
     * @return this authentication service ID
     */
    int getServiceId();

    /**
     * Returns the authentication service name.
     * @return the authentication service name
     */
    @NotNull String getServiceName();

    /**
     * Returns the authentication service type.
     * @return the authentication service type
     */
    @NotNull ServiceType getServiceType();
}
