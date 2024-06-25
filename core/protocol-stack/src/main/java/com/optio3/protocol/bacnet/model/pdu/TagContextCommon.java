/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.lang.Unsigned32;
import com.optio3.logging.Logger;
import com.optio3.protocol.bacnet.model.enums.ApplicationTag;
import com.optio3.protocol.model.bacnet.Choice;
import com.optio3.protocol.model.bacnet.Constructed;
import com.optio3.protocol.model.bacnet.PropertyType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.serialization.BACnetSerializationTag;
import com.optio3.serialization.SerializablePiece;
import com.optio3.serialization.SerializationSlotToFields;
import com.optio3.serialization.SerializationTag;

public abstract class TagContextCommon
{
    public static final Logger LoggerInstance = new Logger(TagContextForEncoding.class);

    //--//

    public static class PrimitiveWrapper extends Constructed
    {
        @SerializationTag(number = 0)
        public Object value;
    }

    public static class CustomWrapper extends Constructed
    {
        public final List<CustomField> fields = Lists.newArrayList();
    }

    public static class CustomField
    {
        public ApplicationTag applicationTag;
        public int            contextTag;

        @SerializationTag(number = 0)
        public Object value;
    }

    public static class ContextType
    {
        Class<?> type;

        boolean isArray;

        boolean isList;
    }

    //--//

    private final   TagContextCommon m_outer;
    protected final Object           m_target;

    private final String               m_defaultChoiceSet;
    private       Map<String, Boolean> m_choiceSets;

    protected BACnetObjectTypeOrUnknown         m_objectContext;
    protected BACnetPropertyIdentifierOrUnknown m_propertyContext;
    protected Unsigned32                        m_propertyIndexContext;

    protected TagContextCommon(TagContextCommon outer,
                               Object target)
    {
        m_outer  = outer;
        m_target = target;

        if (target instanceof Choice)
        {
            m_defaultChoiceSet = "<Default>";
            ensureChoiceSet(m_defaultChoiceSet);
        }
        else
        {
            m_defaultChoiceSet = null;
        }
    }

    //--//

    protected BACnetSerializationTag getExtensionTag(SerializablePiece piece)
    {
        return piece.getFieldAnnotation(BACnetSerializationTag.class);
    }

    protected BACnetSerializationTag getExtensionTag(SerializationSlotToFields slot)
    {
        if (slot.normalPiece != null)
        {
            return slot.normalPiece.getFieldAnnotation(BACnetSerializationTag.class);
        }

        for (SerializablePiece piece : slot.bitfieldPieces)
        {
            BACnetSerializationTag tag = piece.getFieldAnnotation(BACnetSerializationTag.class);
            if (tag != null)
            {
                return null;
            }
        }

        return null;
    }

    protected boolean isUntagged(SerializablePiece piece)
    {
        BACnetSerializationTag tag = getExtensionTag(piece);

        return tag != null ? tag.untagged() : false;
    }

    protected boolean isPropertyIndex(SerializablePiece piece)
    {
        BACnetSerializationTag tag = getExtensionTag(piece);

        return tag != null ? tag.propertyIndex() : false;
    }

    private String getChoiceSet(SerializationSlotToFields slot)
    {
        BACnetSerializationTag tag = getExtensionTag(slot);
        if (tag != null)
        {
            String res = tag.choiceSet();
            if (res != null && res.length() > 0)
            {
                return res;
            }
        }

        return null;
    }

    //--//

    protected void ensureChoiceSet(String choiceSet)
    {
        if (m_choiceSets == null)
        {
            m_choiceSets = Maps.newHashMap();
        }

        m_choiceSets.putIfAbsent(choiceSet, null);
    }

    protected void registerChoiceSet(String choiceSet)
    {
        if (choiceSet == null)
        {
            return;
        }

        ensureChoiceSet(choiceSet);

        m_choiceSets.put(choiceSet, Boolean.TRUE);
    }

    protected String selectChoiceSet(SerializationSlotToFields slot)
    {
        String choiceSet = getChoiceSet(slot);
        if (choiceSet != null)
        {
            ensureChoiceSet(choiceSet);
        }
        else
        {
            choiceSet = m_defaultChoiceSet;
        }

        return choiceSet;
    }

    protected String verifyChoiceSets()
    {
        if (m_choiceSets != null)
        {
            for (String key : m_choiceSets.keySet())
            {
                Boolean value = m_choiceSets.get(key);
                if (value == null)
                {
                    return key;
                }
            }
        }

        return null;
    }

    protected boolean checkChoiceSet(String choiceSet)
    {
        if (choiceSet == null)
        {
            return true;
        }

        if (m_choiceSets != null)
        {
            Boolean value = m_choiceSets.get(choiceSet);
            if (value != null)
            {
                return true;
            }
        }

        return false;
    }

    //--//

    protected TagContextCommon getNonChoiceContext()
    {
        TagContextCommon ctx = this;
        while (true)
        {
            if (ctx.m_defaultChoiceSet == null)
            {
                return ctx;
            }

            if (ctx.m_outer == null)
            {
                return ctx;
            }

            ctx = ctx.m_outer;
        }
    }

    protected BACnetObjectTypeOrUnknown getObjectTypeContext()
    {
        for (TagContextCommon ctx = this; ctx != null; ctx = ctx.m_outer)
        {
            if (ctx.m_objectContext != null)
            {
                return ctx.m_objectContext;
            }
        }

        return null;
    }

    protected BACnetPropertyIdentifierOrUnknown getPropertyContext()
    {
        for (TagContextCommon ctx = this; ctx != null; ctx = ctx.m_outer)
        {
            if (ctx.m_propertyContext != null)
            {
                return ctx.m_propertyContext;
            }
        }

        return null;
    }

    protected Unsigned32 getPropertyIndexContext()
    {
        for (TagContextCommon ctx = this; ctx != null; ctx = ctx.m_outer)
        {
            if (ctx.m_propertyIndexContext != null)
            {
                return ctx.m_propertyIndexContext;
            }
        }

        return null;
    }

    ContextType inferTypeOfValueFromContext()
    {
        BACnetObjectTypeOrUnknown objectType = getObjectTypeContext();
        if (objectType == null)
        {
            return null;
        }

        BACnetPropertyIdentifierOrUnknown prop = getPropertyContext();
        if (prop == null)
        {
            return null;
        }

        ContextType ct = new ContextType();

        if (objectType.isUnknown() || prop.isUnknown())
        {
            ct.type    = CustomWrapper.class;
            ct.isArray = false;
            ct.isList  = false;
        }
        else
        {
            PropertyType pt = objectType.value.propertyTypes()
                                              .get(prop.value);
            if (pt == null)
            {
                return null;
            }

            ct.type    = pt.type();
            ct.isArray = pt.isArray();
            ct.isList  = pt.isList();
        }

        return ct;
    }
}