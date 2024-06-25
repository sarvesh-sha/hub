/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.optio3.cloud.persistence.MetadataMap;

/**
 * Control the mapping between Hibernate tables and RecordIdentity table IDs.
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE })
public @interface Optio3TableInfo
{
    interface IMetadataDigest
    {
    }

    class NoMetadata implements IMetadataDigest
    {
    }

    /**
     * @return The id to use when identifying a record through RecordIdentity
     */
    String externalId();

    /**
     * @return The class used to represent the entity in REST calls
     */
    Class<?> model();

    /**
     * @return The metamodel class from Hibernate
     */
    Class<?> metamodel();

    /**
     * @return Set to true to have the table optimized on boot.
     */
    boolean defragmentOnBoot() default false;

    /**
     * @return The class that describes Metafields.
     */
    Class<? extends IMetadataDigest> metadata() default NoMetadata.class;
}
