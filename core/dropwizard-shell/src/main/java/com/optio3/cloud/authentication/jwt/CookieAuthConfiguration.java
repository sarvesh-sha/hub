/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.authentication.jwt;

public class CookieAuthConfiguration
{
    private String secretSeed;

    private boolean secure = false;

    private boolean httpOnly = true;

    private String volatileSessionDuration = "PT30m";

    private String persistentSessionDuration = "P7d";

    /**
     * The secret seed use to generate the signing key. It can be used to keep
     * the same key value across application reboots.
     *
     * @return the signing key seed
     */
    public String getSecretSeed()
    {
        return secretSeed;
    }

    /**
     * Indicates if the 'secure' flag must be set on cookies
     *
     * @return if the 'secure' flag must be set on cookies
     */
    public boolean isSecure()
    {
        return secure;
    }

    /**
     * Indicates if the 'secure' flag must be set on cookies
     *
     * @return if the 'secure' flag must be set on cookies
     */
    public boolean isHttpOnly()
    {
        return httpOnly;
    }

    /**
     * duration of volatile cookies (in ISO 8601 format)
     *
     * @return the duration of volatile cookies
     */
    public String getSessionExpiryVolatile()
    {
        return volatileSessionDuration;
    }

    /**
     * duration of persistent cookies (in ISO 8601 format)
     *
     * @return the duration of persistent cookies
     */
    public String getSessionExpiryPersistent()
    {
        return persistentSessionDuration;
    }
}
