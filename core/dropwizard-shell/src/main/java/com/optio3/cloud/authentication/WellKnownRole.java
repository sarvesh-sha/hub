/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.authentication;

import java.util.Base64;

import com.optio3.util.Encryption;
import org.apache.commons.lang3.StringUtils;

public enum WellKnownRole
{
    Administrator(WellKnownRoleIds.Administrator, "Administrator"),
    Maintenance(WellKnownRoleIds.Maintenance, "Maintenance"),
    Infrastructure(WellKnownRoleIds.Infrastructure, "Infrastructure"),
    Machine(WellKnownRoleIds.Machine, "Machine"),
    User(WellKnownRoleIds.User, "User"),
    Publisher(WellKnownRoleIds.Publisher, "Publisher");

    private final String m_id;
    private final String m_displayName;

    WellKnownRole(String id,
                  String displayName)
    {
        m_id          = id;
        m_displayName = displayName;
    }

    public static WellKnownRole parse(String id)
    {
        for (WellKnownRole role : values())
        {
            if (role.getId()
                    .equals(id))
            {
                return role;
            }
        }

        return null;
    }

    public String getId()
    {
        return m_id;
    }

    public String getDisplayName()
    {
        return m_displayName;
    }

    public String generateAuthPrincipal(String version,
                                        String hostId)
    {
        return m_id + "@" + version + "@" + encodeHostId(hostId);
    }

    public String generateAuthCode(String version,
                                   String hostId)
    {
        switch (version)
        {
            case "v1":
                // Some random value. DO NOT CHANGE!!!!!!
                String seedV1 = "iWKqYrItjXN2INNp3Bj83YVkqt3s0Z8M";
                return Encryption.computeSha1AsText(hostId + "@" + seedV1 + "@" + m_id);
        }

        throw new IllegalArgumentException("Invalid version");
    }

    public boolean validateAuthCode(String principal,
                                    String password,
                                    boolean checkPassword)
    {
        String[] parts = StringUtils.split(principal, '@');
        if (parts.length == 3 && parts[0].equals(m_id))
        {
            String version = parts[1];
            String hostId  = decodeHostId(parts[2]);

            String authCode = generateAuthCode(version, hostId);

            if (!checkPassword)
            {
                return true;
            }

            return StringUtils.equals(authCode, password);
        }

        return false;
    }

    private static String encodeHostId(String hostId)
    {
        return Base64.getEncoder()
                     .encodeToString(hostId.getBytes());
    }

    private static String decodeHostId(String hostId)
    {
        return new String(Base64.getDecoder()
                                .decode(hostId));
    }
}
