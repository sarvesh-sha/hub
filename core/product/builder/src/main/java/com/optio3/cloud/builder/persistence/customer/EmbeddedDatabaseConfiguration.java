/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.customer;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import com.optio3.cloud.persistence.EncryptedPayload;
import org.apache.commons.lang3.StringUtils;

@Embeddable
public class EmbeddedDatabaseConfiguration
{
    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false)
    private DatabaseMode mode;

    @Column(name = "server")
    private String server;

    @Column(name = "database_name")
    private String databaseName;

    @Column(name = "database_user")
    private String databaseUser;

    @Embedded
    private EncryptedPayload databasePassword;

    //--//

    public DatabaseMode getMode()
    {
        return mode;
    }

    public void setMode(DatabaseMode mode)
    {
        this.mode = mode;
    }

    public String getServer()
    {
        return server;
    }

    public void setServer(String server)
    {
        this.server = server;
    }

    public String getDatabaseName()
    {
        return databaseName;
    }

    public void setDatabaseName(String databaseName)
    {
        this.databaseName = databaseName;
    }

    public String getDatabaseUser()
    {
        return databaseUser;
    }

    public void setDatabaseUser(String databaseUser)
    {
        this.databaseUser = databaseUser;
    }

    public EncryptedPayload getDatabasePassword()
    {
        return databasePassword;
    }

    public void setDatabasePassword(EncryptedPayload databasePassword)
    {
        this.databasePassword = databasePassword;
    }

    //--//

    public String getServerName()
    {
        String[] parts = StringUtils.split(server, ':');
        return parts != null && parts.length >= 1 ? parts[0] : null;
    }
}

