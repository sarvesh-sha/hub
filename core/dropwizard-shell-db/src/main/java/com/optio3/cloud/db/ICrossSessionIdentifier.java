/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.db;

import java.io.Serializable;

import org.hibernate.Session;

public interface ICrossSessionIdentifier extends Serializable
{
    Serializable remapToSession(Session session);
}
