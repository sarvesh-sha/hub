/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Sets;

public class MetadataTagsMap
{
    private static class Entry
    {
        public final TreeSet<String> values = new TreeSet<>();
    }

    @JsonProperty("state")
    private final TreeMap<String, Entry> m_state = new TreeMap<>();

    @JsonIgnore
    public boolean isEmpty()
    {
        return m_state.isEmpty();
    }

    public boolean hasTag(String tag)
    {
        return m_state.containsKey(tag);
    }

    public boolean removeTag(String tag)
    {
        return m_state.remove(tag) != null;
    }

    public boolean addTag(String tag,
                          boolean lowercase)
    {
        if (tag == null)
        {
            return false;
        }

        if (lowercase)
        {
            tag = tag.toLowerCase();
        }

        if (hasTag(tag))
        {
            return false;
        }

        accessTag(tag, true);
        return true;
    }

    public boolean addTags(Collection<String> tags,
                           boolean lowercase)
    {
        boolean modified = false;

        if (tags != null)
        {
            for (String tag : tags)
            {
                modified |= addTag(tag, lowercase);
            }
        }

        return modified;
    }

    public void clear()
    {
        m_state.clear();
    }

    public Set<String> listTags()
    {
        return Sets.newHashSet(m_state.keySet());
    }

    public boolean addValueToTag(String tag,
                                 String value)
    {
        Entry entry = accessTag(tag, true);

        return entry.values.add(value);
    }

    public boolean removeValueFromTag(String tag,
                                      String value)
    {
        Entry entry = accessTag(tag, false);
        if (entry == null)
        {
            return false;
        }

        return entry.values.remove(value);
    }

    public Set<String> getValuesForTag(String tag)
    {
        Entry entry = accessTag(tag, false);
        return entry == null ? Collections.emptySet() : Sets.newHashSet(entry.values);
    }

    public Set<String> setValuesForTag(String tag,
                                       Set<String> set)
    {
        Entry       entry  = accessTag(tag, true);
        Set<String> oldSet = Sets.newHashSet(entry.values);

        entry.values.clear();
        if (set != null)
        {
            entry.values.addAll(set);
        }

        return oldSet;
    }

    //--//

    private Entry accessTag(String tag,
                            boolean createIfMissing)
    {
        Entry entry = m_state.get(tag);
        if (entry == null && createIfMissing)
        {
            entry = new Entry();
            m_state.put(tag, entry);
        }

        return entry;
    }
}
