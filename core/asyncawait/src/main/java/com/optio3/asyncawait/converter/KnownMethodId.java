/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.asyncawait.converter;

public enum KnownMethodId
{
    AsyncOrigin_type,
    AsyncOrigin_method,
    AsyncOrigin_signature,
    CompileTime_bootstrap,
    CompileTime_wasComputationCancelled,
    CompileTime_await,
    CompileTime_awaitNoUnwrap,
    CompileTime_awaitTimeout,
    CompileTime_awaitNoUnwrapTimeout,
    CompileTime_sleep,
    CompileTime_wrapAsync,
    AsyncComputation_init,
    AsyncComputation_start,
    AsyncComputation_startDelayed,
    AsyncComputation_sleep,
    AsyncComputation_advanceInner,
    AsyncComputation_forwardFuture,
    AsyncComputation_invalidState,
    ContinuationState_init_foreground,
    ContinuationState_init_background,
    ContinuationState_queue,
    ContinuationState_queueTimeout,
    ContinuationState_getFuture,
    ContinuationState_getAndUnwrapException
}
