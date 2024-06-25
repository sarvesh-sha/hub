/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud;

import org.eclipse.jetty.http.HttpStatus;

public class RequestStatistics
{
    public final String path;

    public int  count;
    public long executionTime;
    public long bytesRead;
    public long bytesWritten;

    public int statusSuccess;
    public int statusClientError;
    public int statusServerError;

    public RequestStatistics(String path)
    {
        this.path = path;
    }

    public synchronized void update(int status,
                                    long executionTime,
                                    long bytesRead,
                                    long bytesWritten)
    {
        count++;
        this.executionTime += executionTime;
        this.bytesRead += bytesRead;
        this.bytesWritten += bytesWritten;

        if (HttpStatus.isInformational(status) || HttpStatus.isSuccess(status) || HttpStatus.isRedirection(status))
        {
            statusSuccess++;
        }
        else if (HttpStatus.isClientError(status))
        {
            statusClientError++;
        }
        else if (HttpStatus.isServerError(status))
        {
            statusServerError++;
        }
    }

    public synchronized RequestStatistics copy()
    {
        RequestStatistics res = new RequestStatistics(path);

        res.count         = count;
        res.executionTime = executionTime;
        res.bytesRead     = bytesRead;
        res.bytesWritten  = bytesWritten;

        res.statusSuccess     = statusSuccess;
        res.statusClientError = statusClientError;
        res.statusServerError = statusServerError;

        return res;
    }
}
