/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.lang.reflect.Field;

public class ModelMapperPolicy
{
    public static final ModelMapperPolicy Default = new ModelMapperPolicy();

    public boolean canOverrideReadOnlyField(Field modelField)
    {
        return false;
    }

    public boolean canReadField(Field modelField)
    {
        return true;
    }

    public boolean canWriteField(Field modelField)
    {
        return true;
    }

    public EncryptedPayload encryptField(Field modelField,
                                         String value) throws
                                                       Exception
    {
        return null;
    }

    public String decryptField(Field modelField,
                               EncryptedPayload value) throws
                                                       Exception
    {
        return null;
    }
}
