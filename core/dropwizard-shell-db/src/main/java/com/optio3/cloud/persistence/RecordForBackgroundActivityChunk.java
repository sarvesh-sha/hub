/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.util.Arrays;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;

import com.optio3.serialization.ObjectMappers;

@MappedSuperclass
public abstract class RecordForBackgroundActivityChunk<R extends RecordForBackgroundActivity<R, C, W>, C extends RecordForBackgroundActivityChunk<R, C, W>, W extends RecordForWorker<W>> extends RecordWithCommonFields
{
    @Lob
    @Column(name = "state")
    @Basic(fetch = FetchType.LAZY)
    private byte[] state;

    //--//

    protected RecordForBackgroundActivityChunk()
    {
    }

    //--//

    public abstract R getOwningActivity();

    public abstract void setOwningActivity(R owningActivity);

    public <S> S getState(Class<S> clz)
    {
        return ObjectMappers.deserializeFromGzip(getRawState(), clz);
    }

    public void setState(Object state)
    {
        byte[] newState = ObjectMappers.serializeToGzip(state);
        setRawState(newState);
    }

    public byte[] getRawState()
    {
        return this.state;
    }

    public void setRawState(byte[] state)
    {
        if (!Arrays.equals(this.state, state))
        {
            this.state = state;
        }
    }
}
