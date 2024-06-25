/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.search;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * When applied to a Hibernate entity, this provides a global context handler during search indexing.
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE })
public @interface Optio3HibernateSearchContext
{
    Class<? extends HibernateIndexingContext> handler();
}
