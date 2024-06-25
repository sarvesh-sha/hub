/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.model;

public class ValidationResult
{
    public String field;
    public String reason;

    @Override
    public String toString()
    {
        return "ValidationResult{" + "field='" + field + '\'' + ", reason='" + reason + '\'' + '}';
    }
}
