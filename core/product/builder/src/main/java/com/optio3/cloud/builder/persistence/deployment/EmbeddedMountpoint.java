/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.deployment;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class EmbeddedMountpoint
{
    @Column(nullable = false)
    private String type;

    @Column
    private String name;

    @Column
    private boolean readWrite;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private String destination;

    @Column
    private String driver;

    @Column
    private String mode;

    @Column
    private String propagation;

    //--//

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public boolean isReadWrite()
    {
        return readWrite;
    }

    public void setReadWrite(boolean readWrite)
    {
        this.readWrite = readWrite;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    public String getDestination()
    {
        return destination;
    }

    public void setDestination(String destination)
    {
        this.destination = destination;
    }

    public String getDriver()
    {
        return driver;
    }

    public void setDriver(String driver)
    {
        this.driver = driver;
    }

    public String getMode()
    {
        return mode;
    }

    public void setMode(String mode)
    {
        this.mode = mode;
    }

    public String getPropagation()
    {
        return propagation;
    }

    public void setPropagation(String propagation)
    {
        this.propagation = propagation;
    }
}
