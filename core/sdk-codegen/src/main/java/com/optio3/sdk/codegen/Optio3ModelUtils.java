/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.sdk.codegen;

import java.util.List;
import java.util.Map;

import com.optio3.cloud.client.SwaggerExtensions;
import com.optio3.serialization.Reflection;
import io.swagger.models.ComposedModel;
import io.swagger.models.Model;
import io.swagger.models.RefModel;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;

public class Optio3ModelUtils
{
    public static void setVendorExtensionValue(Model model,
                                               SwaggerExtensions kind,
                                               Object value)
    {
        if (model != null)
        {
            setVendorExtensionValue(model.getVendorExtensions(), kind, value);
        }
    }

    public static void setVendorExtensionValue(Map<String, Object> map,
                                               SwaggerExtensions kind,
                                               Object value)
    {
        if (map != null)
        {
            map.put(kind.getText(), value);
        }
    }

    //--//

    public static <T> T getVendorExtensionValue(Model model,
                                                SwaggerExtensions kind,
                                                Class<T> clz)
    {
        return model != null ? getVendorExtensionValue(model.getVendorExtensions(), kind, clz) : null;
    }

    public static <T> List<T> getVendorExtensionListValue(Model model,
                                                          SwaggerExtensions kind,
                                                          Class<T> clz)
    {
        @SuppressWarnings("unchecked") List<T> res = (List<T>) getVendorExtensionValue(model, kind, List.class);
        return res;
    }

    public static <T> T getVendorExtensionValue(Property p,
                                                SwaggerExtensions kind,
                                                Class<T> clz)
    {
        return p != null ? getVendorExtensionValue(p.getVendorExtensions(), kind, clz) : null;
    }

    public static <T> T getVendorExtensionValue(Parameter p,
                                                SwaggerExtensions kind,
                                                Class<T> clz)
    {
        return p != null ? getVendorExtensionValue(p.getVendorExtensions(), kind, clz) : null;
    }

    public static <T> T getVendorExtensionValue(Map<String, Object> map,
                                                SwaggerExtensions kind,
                                                Class<T> clz)
    {
        if (map == null)
        {
            return null;
        }

        Object val = map.get(kind.getText());
        return clz.cast(val);
    }

    public static String extractOptio3Type(Model model,
                                           SwaggerExtensions selector)
    {
        return getVendorExtensionValue(model, selector, String.class);
    }

    public static String extractOptio3Type(Property p,
                                           SwaggerExtensions selector)
    {
        return getVendorExtensionValue(p, selector, String.class);
    }

    public static String extractOptio3ShortType(Property p,
                                                SwaggerExtensions selector)
    {
        String type = extractOptio3Type(p, selector);
        if (type == null)
        {
            return null;
        }

        int pos = type.lastIndexOf('.');
        return pos < 0 ? type : type.substring(pos + 1);
    }

    public static String extractOptio3Type(Parameter p,
                                           SwaggerExtensions selector)
    {
        return getVendorExtensionValue(p, selector, String.class);
    }

    public static String extractOptio3ShortType(Parameter p,
                                                SwaggerExtensions selector)
    {
        String type = extractOptio3Type(p, selector);
        if (type == null)
        {
            return null;
        }

        int pos = type.lastIndexOf('.');
        return pos < 0 ? type : type.substring(pos + 1);
    }

    //--//

    public static int getInheritanceDepth(Map<String, Model> definitions,
                                          Model model)
    {
        int   inheritanceDepth = 0;
        Model parent           = getParent(definitions, model);

        while (parent != null)
        {
            inheritanceDepth++;
            parent = getParent(definitions, parent);
        }

        return inheritanceDepth;
    }

    public static Model getParent(Map<String, Model> definitions,
                                  Model model)
    {
        String parentName = getParentName(model);
        return parentName != null ? definitions.get(parentName) : null;
    }

    public static String getParentName(Model model)
    {
        ComposedModel model2 = Reflection.as(model, ComposedModel.class);
        if (model2 != null)
        {
            for (Model child : model2.getAllOf())
            {
                RefModel ref = Reflection.as(child, RefModel.class);
                if (ref != null)
                {
                    return ref.getSimpleRef();
                }
            }
        }

        return null;
    }
}
