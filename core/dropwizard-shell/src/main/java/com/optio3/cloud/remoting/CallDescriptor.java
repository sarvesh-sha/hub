/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.remoting;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;

class CallDescriptor
{
    static class Param
    {
        JavaType jsonType;
        boolean  isCallback;

        //--//

        @Override
        public String toString()
        {
            return String.format("%s%s", isCallback ? "CALLBACK for " : "", jsonType);
        }
    }

    final Param[] parameters;

    final Param returnValue;

    CallDescriptor(TypeFactory typeFactory,
                   Type type,
                   Method m)
    {
        Parameter[] localParams = m.getParameters();

        parameters = new Param[localParams.length];

        for (int i = 0; i < localParams.length; i++)
        {
            Parameter localParam     = localParams[i];
            Type      localParamType = localParam.getParameterizedType();

            Param remoteParam = new Param();
            Type  javaType    = Reflection.resolveGenericType(type, localParamType);
            remoteParam.jsonType = typeFactory.constructType(javaType);

            Method callback = Reflection.findMethodOfFunctionalInterface(javaType);
            if (callback != null)
            {
                if (!Reflection.isMethodReturningAPromise(javaType, callback))
                {
                    throw Exceptions.newIllegalArgumentException("Parameter %s in %s is not compatible with Futures", localParam.getName(), m);
                }

                remoteParam.isCallback = true;
            }

            parameters[i] = remoteParam;
        }

        returnValue = new Param();
        Type returnValueJavaType = Reflection.resolveGenericType(type, m.getGenericReturnType());
        returnValue.jsonType = typeFactory.constructType(returnValueJavaType);
    }

    boolean sameSignature(JavaType[] otherParameters)
    {
        if (parameters.length != otherParameters.length)
        {
            return false;
        }

        for (int i = 0; i < parameters.length; i++)
        {
            if (!parameters[i].jsonType.equals(otherParameters[i]))
            {
                return false;
            }
        }

        return true;
    }

    //--//

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("(");
        for (int i = 0; i < parameters.length; i++)
        {
            if (i != 0)
            {
                sb.append(", ");
            }

            sb.append(parameters[i]);
        }

        return sb.toString();
    }
}
