package com.microsoft.hydralab.common.appcenter.entity;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

public interface Log {
    /**
     * Get the type value.
     *
     * @return the type value
     */
    String getType();

    /**
     * Get the timestamp value.
     *
     * @return the timestamp value
     */
    String getTimestamp();


    /**
     * Get the sid value.
     *
     * @return the sid value
     */
    UUID getSid();


    /**
     * Get the distributionGroupId value.
     *
     * @return the distributionGroupId value.
     */
    String getDistributionGroupId();


    /**
     * Get the userId value.
     *
     * @return the userId value.
     */
    String getUserId();


    /**
     * Get the device value.
     *
     * @return the device value
     */
    @SuppressWarnings("unused")
    Device getDevice();

    /**
     * Adds a transmissionTargetToken that this log should be sent to.
     *
     * @param transmissionTargetToken the identifier of the transmissionTargetToken.
     */
    @SuppressWarnings("unused")
    void addTransmissionTarget(String transmissionTargetToken);

    /**
     * Gets all transmission targets that this log should be sent to.
     *
     * @return Collection of transmission targets that this log should be sent to.
     */
    @SuppressWarnings("unused")
    Set<String> getTransmissionTargetTokens();

    /**
     * Get internal tag for this log.
     *
     * @return internal tag or null.
     */
    Object getTag();
}
