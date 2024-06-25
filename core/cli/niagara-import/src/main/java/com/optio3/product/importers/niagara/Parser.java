/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara;

import java.lang.reflect.Field;
import java.util.Map;

import com.google.common.collect.Maps;
import com.optio3.product.importers.niagara.baja.sys.BSimple;
import com.optio3.product.importers.niagara.baja.sys.BValue;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Parser
{
    private static final Map<ParsedType, Class<? extends BValue>> s_unknownTypes   = Maps.newHashMap();
    private final        Map<String, Class<? extends BValue>>     m_moduleHandlers = Maps.newHashMap();
    private final        Map<ParsedType, Class<? extends BValue>> m_typeHandlers   = Maps.newHashMap();
    private final        Map<String, String>                      m_modules        = Maps.newHashMap();

    public Parser()
    {
        Reflections reflections = new Reflections("com.optio3.product.importers.niagara.", new TypeAnnotationsScanner());

        for (Class<?> t : reflections.getTypesAnnotatedWith(ModuleTypeAnnotation.class, true))
        {
            ModuleTypeAnnotation anno = t.getAnnotation(ModuleTypeAnnotation.class);
            processAnnotation(t, anno);
        }

        for (Class<?> t : reflections.getTypesAnnotatedWith(ModuleTypeAnnotationContainer.class, true))
        {
            ModuleTypeAnnotationContainer annoContainer = t.getAnnotation(ModuleTypeAnnotationContainer.class);
            for (ModuleTypeAnnotation anno : annoContainer.value())
            {
                processAnnotation(t, anno);
            }
        }
    }

    private void processAnnotation(Class<?> t,
                                   ModuleTypeAnnotation anno)
    {
        String     module = anno.module();
        String     type   = anno.type();
        ParsedType pt     = new ParsedType(module, type);
        if (Reflection.isAbstractClass(t))
        {
            throw Exceptions.newRuntimeException("Annotation '%s': %s is abstract", pt, t.getSimpleName());
        }

        @SuppressWarnings("unchecked") Class<? extends BValue> t2 = (Class<? extends BValue>) t;

        if (StringUtils.isEmpty(type))
        {
            Class<? extends BValue> t3 = m_moduleHandlers.get(module);

            if (t3 != null && t3 != t2)
            {
                throw Exceptions.newRuntimeException("Two modules with same annotation '%s': %s and %s", module, t3.getSimpleName(), t2.getSimpleName());
            }

            m_moduleHandlers.put(module, t2);
        }
        else
        {
            Class<? extends BValue> t3 = m_typeHandlers.get(pt);

            if (t3 != null && t3 != t2)
            {
                throw Exceptions.newRuntimeException("Two types with same annotation '%s': %s and %s", pt, t3.getSimpleName(), t2.getSimpleName());
            }

            m_typeHandlers.put(pt, t2);
        }
    }

    public BValue load(BValue parent,
                       Element el) throws
                                   IllegalAccessException
    {
        EncodedString name     = null;
        ParsedType    typeDesc = null;
        EncodedString flags    = null;
        EncodedString handle   = null;
        EncodedString facets   = null;
        EncodedString value    = null;

        NamedNodeMap attrsList = el.getAttributes();
        for (int i = 0; i < attrsList.getLength(); i++)
        {
            Node node = attrsList.item(i);
            if (node instanceof Attr)
            {
                Attr   attr = (Attr) node;
                String v    = attr.getValue();

                switch (attr.getName())
                {
                    case "n":
                        name = new EncodedString(v);
                        break;

                    case "m":
                    {
                        String[] parts = StringUtils.split(v, '=');

                        m_modules.put(parts[0], parts[1]);
                        break;
                    }

                    case "t":
                        typeDesc = getParsedType(v);
                        break;

                    case "f":
                        flags = new EncodedString(v);
                        break;

                    case "h":
                        handle = new EncodedString(v);
                        break;

                    case "x":
                        facets = new EncodedString(v);
                        break;

                    case "v":
                        value = new EncodedString(v);
                        break;
                }
            }
        }

        Class<? extends BValue> t;

        if (typeDesc == null)
        {
            t = GenericValue.class;

            if (parent != null && name != null)
            {
                Field f = Reflection.findField(parent.getClass(), name.getDecodedValue());
                if (f != null)
                {
                    final Class<?> fieldClass = f.getType();
                    if (Reflection.isSubclassOf(BValue.class, fieldClass) && !Reflection.isAbstractClass(fieldClass))
                    {
                        @SuppressWarnings("unchecked") Class<? extends BValue> t2 = (Class<? extends BValue>) fieldClass;

                        t = t2;
                    }
                }
            }
        }
        else
        {
            t = m_typeHandlers.get(typeDesc);

            if (t == null)
            {
                t = m_moduleHandlers.get(typeDesc.module);
            }

            if (t == null)
            {
                t = s_unknownTypes.get(typeDesc);
            }

            if (t == null)
            {
                System.err.printf("Unknown type: %s%n", typeDesc);

                t = GenericValue.class;
                s_unknownTypes.put(typeDesc, t);
            }
        }

        BValue parsedValue;

        try
        {
            parsedValue = Reflection.newInstance(t);
        }
        catch (Throwable e)
        {
            throw Exceptions.newRuntimeException("Failed to instantiate type %s for field %s", t.getSimpleName(), name);
        }

        parsedValue.parent = parent;
        parsedValue.name = name;
        parsedValue.typeDesc = typeDesc;
        parsedValue.flags = flags;
        parsedValue.handle = handle;
        parsedValue.facets = facets;
        parsedValue.value = value;

        NodeList childList = el.getChildNodes();
        for (int i = 0; i < childList.getLength(); i++)
        {
            Node node = childList.item(i);
            if (node instanceof Element)
            {
                Element elChild = (Element) node;

                switch (elChild.getTagName())
                {
                    case "p":
                    {
                        BValue parsedChild = load(parsedValue, elChild);
                        parsedValue.children.add(parsedChild);

                        if (parsedChild.name != null)
                        {
                            Field f = Reflection.findField(parsedValue.getClass(), parsedChild.name.getDecodedValue());
                            if (f != null && Reflection.isSubclassOf(BValue.class, f.getType()))
                            {
                                try
                                {
                                    f.set(parsedValue, parsedChild);
                                }
                                catch (Exception e)
                                {
                                    System.out.println(e.getMessage());
                                }
                            }
                        }

                        break;
                    }
                }
            }
        }

        if (parsedValue instanceof BSimple)
        {
            BSimple simple = (BSimple) parsedValue;
            simple.configureAfterParsing(this);
        }

        return parsedValue;
    }

    public ParsedType getParsedType(String v)
    {
        String[] parts = StringUtils.split(v, ':');

        String moduleId = parts[0];
        String type     = parts[1];

        return new ParsedType(m_modules.getOrDefault(moduleId, moduleId), type);
    }
}

