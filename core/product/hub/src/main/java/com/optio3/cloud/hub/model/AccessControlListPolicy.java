/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model;

import java.util.List;

import com.google.common.collect.Lists;

public class AccessControlListPolicy
{
    public static class Operation
    {
        public String subject;
        public String verb;
    }

    public static class Rule
    {
        public       boolean         deny;
        public       boolean         stopOnMatch;
        public       DeliveryOptions appliesTo;
        public final List<Operation> operations = Lists.newArrayList();
    }

    public final List<Rule> rules = Lists.newArrayList();
}
