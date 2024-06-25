/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.optio3.cloud.db.Optio3DataSourceFactory;

public class HubExportDefinition
{
    private String id;

    @JsonProperty("id")
    public String getId()
    {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id)
    {
        this.id = id;
    }

    private String packageName;

    @JsonProperty("package")
    public String getPackageName()
    {
        return packageName;
    }

    @JsonProperty("package")
    public void setPackageName(String packageName)
    {
        this.packageName = packageName;
    }

    @Valid
    private Optio3DataSourceFactory database;

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

    @JsonCreator
    public HubExportDefinition(@JsonProperty(value = "id", required = true) String id,
                               @JsonProperty(value = "package", required = true) String packageName,
                               @JsonProperty(value = "database", required = true) Optio3DataSourceFactory database)
    {
        this.id = id;
        this.packageName = packageName;
        this.database = database;
    }
}
