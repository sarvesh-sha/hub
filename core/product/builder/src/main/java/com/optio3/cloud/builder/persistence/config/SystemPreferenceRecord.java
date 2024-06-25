/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.config;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.NoResultException;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.model.config.SystemPreference;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "SYSTEM_PREFERENCE")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "SystemPreference", model = SystemPreference.class, metamodel = SystemPreferenceRecord_.class)
public class SystemPreferenceRecord extends RecordWithCommonFields implements ModelMapperTarget<SystemPreference, SystemPreferenceRecord_>
{
    public static class Tree
    {
        private final RecordHelper<SystemPreferenceRecord> m_helper;
        public final  Map<String, SystemPreferenceRecord>  values  = Maps.newHashMap();
        public final  Map<String, Tree>                    subKeys = Maps.newHashMap();

        public Tree(RecordHelper<SystemPreferenceRecord> helper)
        {
            m_helper = helper;
        }

        //--//

        static Tree build(RecordHelper<SystemPreferenceRecord> helper)
        {
            Tree root = new Tree(helper);

            for (SystemPreferenceRecord rec : list(helper))
            {
                String path = rec.getPath();
                String name = rec.getName();

                Tree node = root.getNode(path, true);
                node.values.put(name, rec);
            }

            return root;
        }

        public SystemPreferenceRecord getValue(@Nonnull SystemPreferenceTypedValue en)
        {
            Tree node = getNode(en.getPath(), false);
            return node != null ? node.values.get(en.getName()) : null;
        }

        public SystemPreferenceRecord setValue(SystemPreferenceTypedValue en,
                                               Object value) throws
                                                             Exception
        {
            SystemPreferenceRecord rec_pref = ensureRecord(en.getPath(), en.getName());

            setValueInner(rec_pref, en, ObjectMappers.RestDefaults.writeValueAsString(value));

            return rec_pref;
        }

        public SystemPreferenceRecord setValue(String path,
                                               String name,
                                               String value) throws
                                                             Exception
        {
            SystemPreferenceRecord rec_pref = ensureRecord(path, name);

            setValueInner(rec_pref, SystemPreferenceTypedValue.find(path, name), value);

            return rec_pref;
        }

        public boolean checkValueFormat(String path,
                                        String name,
                                        String value)
        {
            try
            {
                final SystemPreferenceTypedValue en = SystemPreferenceTypedValue.find(path, name);
                if (en != null)
                {
                    en.roundtrip(value);
                }

                return true;
            }
            catch (Throwable t)
            {
                return false;
            }
        }

        private void setValueInner(SystemPreferenceRecord rec_pref,
                                   SystemPreferenceTypedValue en,
                                   String value) throws
                                                 Exception
        {
            if (rec_pref.setValue(value))
            {
                if (en != null)
                {
                    en.onUpdate(m_helper, rec_pref);
                }
            }
        }

        private SystemPreferenceRecord ensureRecord(String path,
                                                    String name)
        {
            Tree node = getNode(path, true);

            SystemPreferenceRecord rec_pref = node.values.get(name);
            if (rec_pref == null)
            {
                rec_pref = newInstance(path, name);
                m_helper.persist(rec_pref);

                node.values.put(name, rec_pref);
            }

            return rec_pref;
        }

        //--//

        public List<SystemPreferenceRecord> extractValues(@Nonnull SystemPreferenceTypedValue kind)
        {
            List<SystemPreferenceRecord> lst = Lists.newArrayList();
            extractValues(lst, kind);
            return lst;
        }

        private void extractValues(List<SystemPreferenceRecord> lst,
                                   @Nonnull SystemPreferenceTypedValue kind)
        {
            for (SystemPreferenceRecord value : values.values())
            {
                SystemPreferenceTypedValue en = SystemPreferenceTypedValue.find(value.getPath(), value.getName());
                if (en == kind)
                {
                    lst.add(value);
                }
            }

            for (Tree subTree : subKeys.values())
            {
                subTree.extractValues(lst, kind);
            }
        }

        //--//

        public Tree getNode(String path,
                            boolean createIfMissing)
        {
            if (path == null)
            {
                return this;
            }

            Tree parent = getNode(getNodeParent(path), createIfMissing);
            if (parent == null)
            {
                return null;
            }

            Tree node = parent.subKeys.get(path);
            if (node == null && createIfMissing)
            {
                node = new Tree(m_helper);

                parent.subKeys.put(path, node);
            }

            return node;
        }

        public void remove()
        {
            removeChildren();

            for (SystemPreferenceRecord rec : values.values())
            {
                m_helper.delete(rec);
            }

            values.clear();
        }

        public void removeChildren()
        {
            for (Tree value : subKeys.values())
            {
                value.remove();
            }

            subKeys.clear();
        }

        public boolean removeNode(String key)
        {
            SystemPreferenceRecord rec = values.remove(key);
            if (rec != null)
            {
                m_helper.delete(rec);
                return true;
            }

            return false;
        }
    }

    //--//

    @Column(name = "keyId", nullable = false) // key is a reserved keyword in MySQL/MariaDB...
    private String key;

    @Lob
    @Column(name = "value")
    @Basic(fetch = FetchType.LAZY)
    private String value;

    @Transient
    private boolean valueChanged;

    //--//

    public SystemPreferenceRecord()
    {
    }

    public static SystemPreferenceRecord newInstance(SystemPreferenceTypedValue en)
    {
        return newInstance(en.getPath(), en.getName());
    }

    public static SystemPreferenceRecord newInstance(String path,
                                                     String name)
    {
        SystemPreferenceRecord rec_pref = new SystemPreferenceRecord();
        rec_pref.key = buildKey(path, name);

        return rec_pref;
    }

    public static Tree getPreferencesTree(SessionHolder sessionHolder)
    {
        return Tree.build(sessionHolder.createHelper(SystemPreferenceRecord.class));
    }

    public static <T> T getTypedValue(SessionHolder sessionHolder,
                                      SystemPreferenceTypedValue en,
                                      Class<T> clz) throws
                                                    IOException
    {
        Tree                   tree = getPreferencesTree(sessionHolder);
        SystemPreferenceRecord rec  = tree.getValue(en);
        return rec != null ? rec.getTypedValue(clz) : null;
    }

    public static <T> T setTypedValue(SessionHolder sessionHolder,
                                      SystemPreferenceTypedValue en,
                                      T val) throws
                                             Exception
    {
        Tree                   tree = getPreferencesTree(sessionHolder);
        SystemPreferenceRecord rec  = tree.setValue(en, val);

        @SuppressWarnings("unchecked") Class<T> clz = (Class<T>) val.getClass();

        return rec.getTypedValue(clz);
    }

    //--//

    public String getKey()
    {
        return key;
    }

    public String getValue() throws
                             IOException
    {
        SystemPreferenceTypedValue en = SystemPreferenceTypedValue.find(getPath(), getName());
        if (en != null)
        {
            // Make sure it has the correct format.
            return en.roundtrip(this.value);
        }

        return this.value;
    }

    private boolean setValue(String value) throws
                                           IOException
    {
        if (StringUtils.equals(this.value, value))
        {
            return false;
        }

        SystemPreferenceTypedValue en = SystemPreferenceTypedValue.find(getPath(), getName());
        if (en != null)
        {
            // Make sure it has the correct format.
            value = en.roundtrip(value);
        }

        this.value        = value;
        this.valueChanged = true;
        return true;
    }

    public <T> T getTypedValue(Class<T> clz) throws
                                             IOException
    {
        SystemPreferenceTypedValue en = SystemPreferenceTypedValue.find(getPath(), getName());
        if (en == null)
        {
            return null;
        }

        if (StringUtils.isEmpty(value))
        {
            return null;
        }

        if (!Reflection.isSubclassOf(en.getTypeInfo(), clz))
        {
            throw Exceptions.newGenericException(ClassCastException.class, "Can't cast '%s' to '%s'", en.getTypeInfo(), clz);
        }

        return clz.cast(ObjectMappers.RestDefaults.readValue(value, en.getTypeInfo()));
    }

    //--//

    public String getPath()
    {
        return getNodeParent(key);
    }

    public String getName()
    {
        return getNodeName(key);
    }

    private static String getNodeParent(String v)
    {
        if (v == null)
        {
            return null;
        }

        int pos = v.lastIndexOf('/');
        if (pos < 0)
        {
            return null;
        }

        return v.substring(0, pos);
    }

    private static String getNodeName(String v)
    {
        int pos = v.lastIndexOf('/');

        return pos < 0 ? v : v.substring(pos + 1);
    }

    private static String buildKey(String path,
                                   String name)
    {
        return StringUtils.isNotBlank(path) ? path + "/" + name : name;
    }

    //--//

    public static List<SystemPreferenceRecord> list(RecordHelper<SystemPreferenceRecord> helper) throws
                                                                                                 NoResultException
    {
        return QueryHelperWithCommonFields.filter(helper, null);
    }

    public static SystemPreferenceRecord find(RecordHelper<SystemPreferenceRecord> helper,
                                              String key) throws
                                                          NoResultException
    {
        return QueryHelperWithCommonFields.getFirstMatch(helper, (jh) ->
        {
            jh.addWhereClauseWithEqual(jh.root, SystemPreferenceRecord_.key, key);
        });
    }
}
