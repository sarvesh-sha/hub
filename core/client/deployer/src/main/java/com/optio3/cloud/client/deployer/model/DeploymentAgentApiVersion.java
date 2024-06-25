/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.model;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks methods with associated feature level.
 */
@Documented
@Retention(RUNTIME)
@Target({ METHOD })
public @interface DeploymentAgentApiVersion
{
    DeploymentAgentFeature value();
}
