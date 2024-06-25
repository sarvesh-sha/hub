/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineComputation;
import com.optio3.cloud.hub.engine.EngineVariableReference;

@JsonTypeName("EngineProcedureDeclaration")
public class EngineProcedureDeclaration extends EngineComputation
{
    public String functionId;

    public String name;

    // TODO: UPGRADE PATCH: Legacy fixup to add functionId field
    public void setName(String name)
    {
        this.name = name;
        if (functionId == null)
        {
            functionId = name;
        }
    }

    public List<EngineVariableReference> arguments;
}
