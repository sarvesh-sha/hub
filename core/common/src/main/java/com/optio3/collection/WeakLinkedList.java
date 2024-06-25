/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.collection;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.common.collect.Lists;

public class WeakLinkedList<E> implements Iterable<E>
{
    private static class WeakIterator<E> implements Iterator<E>
    {
        private final Iterator<WeakReference<E>> m_iterator;

        private E m_peeked;

        WeakIterator(Iterator<WeakReference<E>> iterator)
        {
            m_iterator = iterator;
        }

        @Override
        public boolean hasNext()
        {
            if (m_peeked != null)
            {
                return true;
            }

            while (m_iterator.hasNext())
            {
                WeakReference<E> ref = m_iterator.next();

                m_peeked = ref.get();
                if (m_peeked != null)
                {
                    return true;
                }

                m_iterator.remove();
            }

            return false;
        }

        @Override
        public E next()
        {
            if (!hasNext())
            {
                throw new NoSuchElementException();
            }

            E next = m_peeked;
            m_peeked = null;
            return next;
        }

        @Override
        public void remove()
        {
            m_iterator.remove();
        }
    }

    //--//

    private final LinkedList<WeakReference<E>> m_list = Lists.newLinkedList();

    public void add(E obj)
    {
        m_list.add(new WeakReference<>(obj));
    }

    public void remove(E obj)
    {
        for (Iterator<E> it = iterator(); it.hasNext(); )
        {
            E objOld = it.next();
            if (objOld == obj)
            {
                it.remove();
                return;
            }
        }
    }

    public List<E> toList()
    {
        return Lists.newArrayList(this);
    }

    @Override
    public Iterator<E> iterator()
    {
        return new WeakIterator<E>(m_list.iterator());
    }
}
