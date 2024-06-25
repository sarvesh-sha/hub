/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.baja.sys;

import java.io.PrintStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import com.google.common.collect.Lists;
import com.optio3.logging.ILoggerAppender;
import com.optio3.product.importers.niagara.EncodedString;
import com.optio3.product.importers.niagara.ParsedType;
import com.optio3.serialization.Reflection;
import com.optio3.util.function.BiConsumerWithException;
import com.optio3.util.function.ConsumerWithException;

public abstract class BValue
{
    public BValue        parent;
    public ParsedType    typeDesc;
    public EncodedString name;
    public EncodedString flags;
    public EncodedString handle;
    public EncodedString facets;
    public EncodedString value;

    public List<BValue> children = Lists.newArrayList();

    public String getDisplayValue()
    {
        return value == null ? "<null>" : "'" + value.getDecodedValue()
                                                     .replaceAll("\n", "\\n") + "'";
    }

    public String getPath()
    {
        return parent == null ? "" : parent.getPath() + "/" + name;
    }

    public <T extends BValue> T findByType(Class<T> clz)
    {
        T res = Reflection.as(this, clz);
        if (res != null)
        {
            return res;
        }

        for (BValue child : children)
        {
            T match = child.findByType(clz);
            if (match != null)
            {
                return match;
            }
        }

        return null;
    }

    public BValue findByType(ParsedType type)
    {
        if (typeDesc != null)
        {
            if (typeDesc.equals(type))
            {
                return this;
            }
        }

        for (BValue child : children)
        {
            BValue match = child.findByType(type);
            if (match != null)
            {
                return match;
            }
        }

        return null;
    }

    public void collectTypes(Set<ParsedType> types)
    {
        if (typeDesc != null)
        {
            types.add(typeDesc);
        }

        for (BValue child : children)
        {
            child.collectTypes(types);
        }
    }

    //--//

    public <T extends BValue> void enumerate(Class<T> clz,
                                             ConsumerWithException<T> callback) throws
                                                                                Exception
    {
        for (BValue child : children)
        {
            T child2 = Reflection.as(child, clz);
            if (child2 != null)
            {
                callback.accept(child2);
            }
        }
    }

    public <T extends BValue, F extends BFolder> void enumerateWithFolders(Class<T> clz,
                                                                           Class<F> folderClz,
                                                                           List<F> path,
                                                                           BiConsumerWithException<T, List<F>> callback) throws
                                                                                                                         Exception
    {
        enumerate(clz, (child) -> callback.accept(child, path));

        enumerate(folderClz, (folder) ->
        {
            List<F> path2 = Lists.newArrayList(path);

            path2.add(folder);
            folder.enumerateWithFolders(clz, folderClz, path2, callback);
        });
    }

    //--//

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(this.getClass()
                      .getSimpleName());

        sb.append("{");
        sb.append("name='");
        sb.append(name);
        sb.append("'");

        sb.append(", typeDesc='");
        sb.append(typeDesc);
        sb.append("'");

        if (value != null)
        {
            sb.append(", value='");
            sb.append(value);
            sb.append("'");
        }
        sb.append("}");

        return sb.toString();
    }

    public void dump(PrintStream out,
                     boolean showIndentation)
    {
        AtomicInteger maxNameLen   = new AtomicInteger(0);
        AtomicInteger maxHandleLen = new AtomicInteger(0);
        AtomicInteger maxTypeLen   = new AtomicInteger(0);
        AtomicInteger maxClassLen  = new AtomicInteger(0);

        enumerate(0, (v, indent) ->
        {
            updateMax(maxNameLen, v.getPath());
            updateMax(maxHandleLen, v.handle);
            updateMax(maxTypeLen, v.typeDesc);
            updateMax(maxClassLen,
                      v.getClass()
                       .getSimpleName());
        });

        ILoggerAppender.ColumnFormat nameColumn   = new ILoggerAppender.ColumnFormat(maxNameLen.get(), maxNameLen.get());
        ILoggerAppender.ColumnFormat handleColumn = new ILoggerAppender.ColumnFormat(maxHandleLen.get(), maxHandleLen.get());
        ILoggerAppender.ColumnFormat typeColumn   = new ILoggerAppender.ColumnFormat(maxTypeLen.get(), maxTypeLen.get());
        ILoggerAppender.ColumnFormat classColumn  = new ILoggerAppender.ColumnFormat(maxClassLen.get(), maxClassLen.get());

        enumerate(0, (v, indent) ->
        {
            if (showIndentation)
            {
                for (int i = 0; i < indent; i++)
                {
                    out.print("  ");
                }
            }

            out.print("Name=");
            out.print(nameColumn.format(v.getPath()));
            out.print(" Handle=");
            out.print(handleColumn.format(v.handle != null ? v.handle : ""));
            out.print(" Type=");
            out.print(typeColumn.format(v.typeDesc != null ? v.typeDesc : ""));
            out.print(" Class=");
            out.print(classColumn.format(v.getClass()
                                          .getSimpleName()));
            out.print(" Value=");
            out.print(v.getDisplayValue());
            out.println();
        });
    }

    private void updateMax(AtomicInteger target,
                           Object value)
    {
        String text = value != null ? value.toString() : "<null>";
        int    len  = Math.max(target.get(), text.length());
        target.set(len);
    }

    private void enumerate(int indent,
                           BiConsumer<BValue, Integer> callback)
    {
        callback.accept(this, indent);

        for (BValue child : children)
        {
            child.enumerate(indent + 1, callback);
        }
    }
}

