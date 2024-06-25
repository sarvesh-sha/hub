/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.hub.engine.EngineStatement;

@JsonSubTypes({ @JsonSubTypes.Type(value = EngineStatementBreak.class),
                @JsonSubTypes.Type(value = EngineStatementComment.class),
                @JsonSubTypes.Type(value = EngineStatementCommentSection.class),
                @JsonSubTypes.Type(value = EngineStatementContinue.class),
                @JsonSubTypes.Type(value = EngineStatementForEach.class),
                @JsonSubTypes.Type(value = EngineStatementFunctionReturn.class),
                @JsonSubTypes.Type(value = EngineStatementFunctionReturnValue.class),
                @JsonSubTypes.Type(value = EngineStatementLog.class),
                @JsonSubTypes.Type(value = EngineStatementLogicIf.class),
                @JsonSubTypes.Type(value = EngineStatementLookupTablePut.class),
                @JsonSubTypes.Type(value = EngineStatementProcedureCall.class),
                @JsonSubTypes.Type(value = EngineStatementRepeatWhile.class),
                @JsonSubTypes.Type(value = EngineStatementThreadEnd.class),
                @JsonSubTypes.Type(value = EngineVariableAssignment.class) })
public abstract class EngineStatementFromCore extends EngineStatement
{
}
