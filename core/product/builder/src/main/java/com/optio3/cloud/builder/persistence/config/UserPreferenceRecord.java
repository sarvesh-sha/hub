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
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3Cascade.Flavor;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.model.config.UserPreference;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.serialization.ObjectMappers;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "USER_PREFERENCE")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "UserPreference", model = UserPreference.class, metamodel = UserPreferenceRecord_.class)
public class UserPreferenceRecord extends RecordWithCommonFields implements ModelMapperTarget<UserPreference, UserPreferenceRecord_>
{
    static class TreeContext
    {
        final UserRecord                         rec_user;
        final RecordHelper<UserPreferenceRecord> helper;

        TreeContext(UserRecord rec_user,
                    RecordHelper<UserPreferenceRecord> helper)
        {
            this.rec_user = rec_user;
            this.helper   = helper;
        }
    }

    public static class Tree
    {
        private final TreeContext                       m_context;
        public final  Map<String, UserPreferenceRecord> values  = Maps.newHashMap();
        public final  Map<String, Tree>                 subKeys = Maps.newHashMap();

        public Tree(TreeContext context)
        {
            m_context = context;
        }

        //--//

        static Tree build(RecordHelper<UserPreferenceRecord> helper,
                          UserRecord rec_user,
                          List<UserPreferenceRecord> lst)
        {
            Tree root = new Tree(new TreeContext(rec_user, helper));

            for (UserPreferenceRecord rec : lst)
            {
                String path = rec.getPath();
                String name = rec.getName();

                Tree node = root.getNode(path, true);
                node.values.put(name, rec);
            }

            return root;
        }

        public UserPreferenceRecord getValue(@Nonnull UserPreferenceTypedValue en)
        {
            Tree node = getNode(en.getPath(), false);
            return node != null ? node.values.get(en.getName()) : null;
        }

        public boolean checkValueFormat(String path,
                                        String name,
                                        String value)
        {
            try
            {
                final UserPreferenceTypedValue en = UserPreferenceTypedValue.find(path, name);
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

        public UserPreferenceRecord setValue(UserPreferenceTypedValue en,
                                             Object value) throws
                                                           IOException
        {
            UserPreferenceRecord rec_pref = ensureRecord(en.getPath(), en.getName());

            setValueInner(rec_pref, en, ObjectMappers.RestDefaults.writeValueAsString(value));

            return rec_pref;
        }

        public UserPreferenceRecord setValue(String path,
                                             String name,
                                             String value) throws
                                                           IOException
        {
            UserPreferenceRecord rec_pref = ensureRecord(path, name);

            setValueInner(rec_pref, UserPreferenceTypedValue.find(path, name), value);

            return rec_pref;
        }

        private void setValueInner(UserPreferenceRecord rec_pref,
                                   UserPreferenceTypedValue en,
                                   String value) throws
                                                 IOException
        {
            if (rec_pref.setValue(value))
            {
                if (en != null)
                {
                    en.onUpdate(m_context.helper, rec_pref);
                }
            }
        }

        private UserPreferenceRecord ensureRecord(String path,
                                                  String name)
        {
            Tree node = getNode(path, true);

            UserPreferenceRecord rec_pref = node.values.get(name);
            if (rec_pref == null)
            {
                rec_pref = newInstance(m_context.rec_user, path, name);
                m_context.helper.persist(rec_pref);

                node.values.put(name, rec_pref);
            }

            return rec_pref;
        }

        //--//

        public List<UserPreferenceRecord> extractValues(@Nonnull UserPreferenceTypedValue kind)
        {
            List<UserPreferenceRecord> lst = Lists.newArrayList();
            extractValues(lst, kind);
            return lst;
        }

        private void extractValues(List<UserPreferenceRecord> lst,
                                   @Nonnull UserPreferenceTypedValue kind)
        {
            for (UserPreferenceRecord value : values.values())
            {
                UserPreferenceTypedValue en = UserPreferenceTypedValue.find(value.getPath(), value.getName());
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
                node = new Tree(m_context);

                parent.subKeys.put(path, node);
            }

            return node;
        }

        public void remove()
        {
            removeChildren();

            for (UserPreferenceRecord rec : values.values())
            {
                m_context.helper.delete(rec);
                rec.user.flushPreferences();
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
            UserPreferenceRecord rec = values.remove(key);
            if (rec != null)
            {
                m_context.helper.delete(rec);
                rec.user.flushPreferences();
                return true;
            }

            return false;
        }
    }

    //--//

    @Optio3ControlNotifications(reason = "Notify user of changes", direct = Notify.ALWAYS, reverse = Notify.NEVER, getter = "getUser")
    @Optio3Cascade(mode = Flavor.DELETE, getter = "getUser")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "user", nullable = false, foreignKey = @ForeignKey(name = "USER__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private UserRecord user;

    @Column(name = "keyId", nullable = false) // key is a reserved keyword in MySQL/MariaDB...
    private String key;

    @Lob
    @Column(name = "value")
    @Basic(fetch = FetchType.LAZY)
    private String value;

    @Transient
    private boolean valueChanged;

    //--//

    public UserPreferenceRecord()
    {
    }

    public static UserPreferenceRecord newInstance(UserRecord rec_user,
                                                   String path,
                                                   String name)
    {
        UserPreferenceRecord rec_pref = new UserPreferenceRecord();
        rec_pref.setUser(rec_user);
        rec_pref.key = buildKey(path, name);

        return rec_pref;
    }

    //--//

    public UserRecord getUser()
    {
        return user;
    }

    public boolean setUser(UserRecord user)
    {
        // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
        if (SessionHolder.sameEntity(this.user, user))
        {
            return false; // Nothing changed.
        }

        this.user = user;
        return true;
    }

    public String getKey()
    {
        return key;
    }

    //--//

    public String getValue() throws
                             IOException
    {
        UserPreferenceTypedValue en = UserPreferenceTypedValue.find(getPath(), getName());
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

        UserPreferenceTypedValue en = UserPreferenceTypedValue.find(getPath(), getName());
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
        UserPreferenceTypedValue en = UserPreferenceTypedValue.find(getPath(), getName());
        if (en == null)
        {
            return null;
        }

        return en.decode(clz, value);
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

    public static String getNodeParent(String v)
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

    public static String getNodeName(String v)
    {
        int pos = v.lastIndexOf('/');

        return pos < 0 ? v : v.substring(pos + 1);
    }

    public static String buildKey(String path,
                                  String name)
    {
        return StringUtils.isNotBlank(path) ? path + "/" + name : name;
    }
}
