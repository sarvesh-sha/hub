/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.workflow;

import com.optio3.cloud.persistence.SessionHolder;

public interface IWorkflowHandlerForCRE
{
    boolean postWorkflowCreationForCRE(SessionHolder sessionHolder);
}
