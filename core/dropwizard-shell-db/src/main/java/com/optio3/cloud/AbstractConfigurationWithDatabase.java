/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.optio3.cloud.db.Optio3DataSourceFactory;
import com.optio3.cloud.persistence.EncryptedPayload;

public abstract class AbstractConfigurationWithDatabase extends AbstractConfiguration
{
    @Valid
    @NotNull
    private Optio3DataSourceFactory database = new Optio3DataSourceFactory();

    @JsonProperty("database")
    public void setDataSourceFactory(Optio3DataSourceFactory factory)
    {
        this.database = factory;
    }

    @JsonProperty("database")
    public Optio3DataSourceFactory getDataSourceFactory()
    {
        return database;
    }

    //--//

    public int hibernateIndexingGateDelay = 20;

    //--//

    /**
     * If the instance has to store credentials in the database, this should be the key used to encrypt/decrypt them.
     */
    public String masterEncryptionKey;

    public EncryptedPayload encrypt(String value) throws
                                                  Exception
    {
        return EncryptedPayload.build(masterEncryptionKey, value);
    }

    public String decrypt(EncryptedPayload ep) throws
                                               Exception
    {
        return ep.decrypt(masterEncryptionKey);
    }
}

