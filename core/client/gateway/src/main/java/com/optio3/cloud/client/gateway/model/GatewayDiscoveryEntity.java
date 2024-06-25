/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.model;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.common.collect.Lists;
import com.optio3.protocol.model.BaseAssetDescriptor;

public class GatewayDiscoveryEntity extends GatewayEntity
{
    public static final String Protocol_BACnet = "BACNET";
    public static final String Protocol_Ipn    = "IPN";
    public static final String Protocol_Perf   = "PERF";

    /**
     * For hierarchical entities, the next level of the entity identification.
     */
    public List<GatewayDiscoveryEntity> subEntities;

    //--//

    public int count(boolean onlyWithContents)
    {
        int num = 0;

        if (contents != null || !onlyWithContents)
        {
            num++;
        }

        if (subEntities != null)
        {
            for (GatewayDiscoveryEntity child : subEntities)
            {
                num += child.count(onlyWithContents);
            }
        }

        return num;
    }

    //--//

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        toString(sb);

        return sb.toString();
    }

    @Override
    protected void toString(StringBuilder sb)
    {
        super.toString(sb);

        if (subEntities != null)
        {
            boolean first = true;

            for (GatewayDiscoveryEntity sub : subEntities)
            {
                if (first)
                {
                    sb.append(" { ");
                    first = false;
                }
                else
                {
                    sb.append(", ");
                }

                sub.toString(sb);
            }
            if (!first)
            {
                sb.append(" }");
            }
        }
    }

    //--//

    public static Iterable<GatewayDiscoveryEntity> filter(List<GatewayDiscoveryEntity> entities,
                                                          GatewayDiscoveryEntitySelector selector)
    {
        return filter(entities, selector, null);
    }

    public static Iterable<GatewayDiscoveryEntity> filter(List<GatewayDiscoveryEntity> entities,
                                                          GatewayDiscoveryEntitySelector selector,
                                                          String selectorValue)
    {
        class IteratorImpl implements Iterator<GatewayDiscoveryEntity>
        {
            private Iterator<GatewayDiscoveryEntity> m_iterator;
            private boolean                          m_fetched;
            private GatewayDiscoveryEntity           m_next;

            IteratorImpl()
            {
                m_iterator = entities != null ? entities.iterator() : null;
            }

            @Override
            public boolean hasNext()
            {
                if (m_iterator == null)
                {
                    return false;
                }

                while (!m_fetched)
                {
                    if (!m_iterator.hasNext())
                    {
                        return false;
                    }

                    GatewayDiscoveryEntity en = m_iterator.next();

                    if (en.selectorKey != selector)
                    {
                        continue;
                    }

                    if (selectorValue != null && !selectorValue.equals(en.selectorValue))
                    {
                        continue;
                    }

                    m_fetched = true;
                    m_next = en;
                    break;
                }

                return m_fetched;
            }

            @Override
            public GatewayDiscoveryEntity next()
            {
                GatewayDiscoveryEntity en = m_next;
                m_next = null;
                m_fetched = false;

                if (en == null)
                {
                    throw new NoSuchElementException();
                }

                return en;
            }
        }

        class IterableImpl implements Iterable<GatewayDiscoveryEntity>
        {
            @Override
            public Iterator<GatewayDiscoveryEntity> iterator()
            {
                return new IteratorImpl();
            }
        }

        return new IterableImpl();
    }

    public Iterable<GatewayDiscoveryEntity> filter(GatewayDiscoveryEntitySelector selector)
    {
        return filter(selector, null);
    }

    public Iterable<GatewayDiscoveryEntity> filter(GatewayDiscoveryEntitySelector selector,
                                                   String selectorValue)
    {
        return filter(subEntities, selector, selectorValue);
    }

    //--//

    public static GatewayDiscoveryEntity create(GatewayDiscoveryEntitySelector selector,
                                                String selectorValue)
    {
        GatewayDiscoveryEntity res = new GatewayDiscoveryEntity();
        res.selectorKey = selector;
        res.selectorValue = selectorValue;
        return res;
    }

    public GatewayDiscoveryEntity createAsRequest(GatewayDiscoveryEntitySelector selector,
                                                  BaseAssetDescriptor selectorValue)
    {
        GatewayDiscoveryEntity res = new GatewayDiscoveryEntity();
        res.selectorKey = selector;

        res.setSelectorValueAsObject(selectorValue);

        add(res);

        return res;
    }

    public synchronized GatewayDiscoveryEntity createAsRequest(GatewayDiscoveryEntitySelector selector,
                                                               String selectorValue)
    {
        GatewayDiscoveryEntity res = new GatewayDiscoveryEntity();
        res.selectorKey = selector;
        res.selectorValue = selectorValue;

        add(res);

        return res;
    }

    private synchronized void add(GatewayDiscoveryEntity child)
    {
        if (subEntities == null)
        {
            subEntities = Lists.newArrayList();
        }

        subEntities.add(child);
    }
}
