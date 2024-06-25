/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import com.optio3.serialization.Reflection;

public abstract class ModelSanitizerHandler
{
    public static class Target
    {
        public static final Target Null = new ModelSanitizerHandler.Target(null);

        public Object  value;
        public boolean remove;
        public boolean replace;

        public Target(Object value)
        {
            this.value = value;
        }

        public void copyFrom(Target other)
        {
            value   = other.value;
            remove  = other.remove;
            replace = other.replace;
        }

        public <T> T castTo(Class<T> clz)
        {
            return clz.cast(value);
        }

        public <T> T as(Class<T> clz)
        {
            return Reflection.as(value, clz);
        }
    }

    //--//

    public abstract void visit(ModelSanitizerContext context,
                               Target target);

    public abstract Class<?> getEntityClassHint();
}
