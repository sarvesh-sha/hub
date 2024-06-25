/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.state;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.optio3.cloud.builder.persistence.worker.ManagedDirectoryRecord;
import com.optio3.cloud.logic.BackgroundActivityHandler.GenericValue;
import com.optio3.cloud.logic.BackgroundActivityHandler.PrimitiveValue;
import com.optio3.cloud.logic.BackgroundActivityHandler.VariableResolver;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.Exceptions;

public class DualPath extends GenericValue implements VariableResolver
{
    public RecordLocator<ManagedDirectoryRecord> hostDir;
    public Path                                  hostPath;

    public Path guestPath;

    //--//

    public static DualPath newInstance(SessionHolder sessionHolder,
                                       ManagedDirectoryRecord dir,
                                       String guest)
    {
        DualPath res = new DualPath();
        res.hostDir = sessionHolder.createLocator(dir);
        res.hostPath = dir.getPath();
        res.guestPath = Paths.get(guest);
        return res;
    }

    public DualPath join(String relativePath)
    {
        DualPath res = new DualPath();

        res.hostDir = hostDir;
        res.hostPath = hostPath.resolve(relativePath);
        res.guestPath = guestPath.resolve(relativePath);

        return res;
    }

    //--//

    @Override
    public GenericValue resolve(String id)
    {
        switch (id)
        {
            case "host":
                return new PrimitiveValue(hostPath);

            case "guest":
                return new PrimitiveValue(guestPath);

            default:
                throw Exceptions.newIllegalArgumentException("Can't find property '%s'", id);
        }
    }

    @Override
    public String convertToString()
    {
        return guestPath.toString();
    }
}
