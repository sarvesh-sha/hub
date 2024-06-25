/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.remoting;

import java.util.List;
import java.util.Objects;

import com.google.common.collect.Lists;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;

public class RemoteCallDescriptor implements Comparable<RemoteCallDescriptor>
{
    public String               classId;
    public String               methodName;
    public List<RemoteArgument> parameters;

    //--//

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        RemoteCallDescriptor that = Reflection.as(o, RemoteCallDescriptor.class);
        if (that == null)
        {
            return false;
        }

        return Objects.equals(classId, that.classId) && Objects.equals(methodName, that.methodName) && Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(classId, methodName, parameters);
    }

    @Override
    public int compareTo(RemoteCallDescriptor o)
    {
        int diff = classId.compareTo(o.classId);
        if (diff == 0)
        {
            diff = methodName.compareTo(o.methodName);
            if (diff == 0)
            {
                for (int i = 0; true; i++)
                {
                    RemoteArgument argThis  = CollectionUtils.getNthElement(parameters, i);
                    RemoteArgument argOther = CollectionUtils.getNthElement(o.parameters, i);

                    if (argThis == null)
                    {
                        diff = (argOther == null) ? 0 : -1;
                        break;
                    }
                    else if (argOther == null)
                    {
                        diff = 1;
                        break;
                    }
                    else
                    {
                        diff = argThis.compareTo(argOther);
                        if (diff != 0)
                        {
                            break;
                        }
                    }
                }
            }
        }

        return diff;
    }

    public RemoteCallDescriptor cloneForSignatureMatching()
    {
        RemoteCallDescriptor res = new RemoteCallDescriptor();

        res.classId = classId;
        res.methodName = methodName;
        res.parameters = Lists.newArrayList();
        for (RemoteArgument parameter : parameters)
        {
            res.parameters.add(parameter.cloneForSignatureMatching());
        }

        return res;
    }

    //--//

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        toString(sb, true, false);

        return sb.toString();
    }

    public String toString(boolean includeClass,
                           boolean includeValues)
    {
        StringBuilder sb = new StringBuilder();
        toString(sb, includeClass, includeValues);
        return sb.toString();
    }

    public void toString(StringBuilder sb,
                         boolean includeClass,
                         boolean includeValues)
    {
        if (includeClass)
        {
            sb.append(classId);
            sb.append('.');
        }

        sb.append(methodName);
        sb.append('(');
        for (int i = 0; i < parameters.size(); i++)
        {
            if (i != 0)
            {
                sb.append(", ");
            }

            parameters.get(i)
                      .toString(sb, includeValues);
        }
        sb.append(')');
    }
}
