/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.model;

import java.util.List;

import com.google.common.collect.Lists;

public class SortCriteria
{
    public String column;

    public boolean ascending;

    //--//

    public static List<SortCriteria> build(String column,
                                           boolean ascending)
    {
        SortCriteria sc = new SortCriteria();
        sc.column = column;
        sc.ascending = ascending;

        List<SortCriteria> lst = Lists.newArrayList();
        lst.add(sc);

        return lst;
    }
}
