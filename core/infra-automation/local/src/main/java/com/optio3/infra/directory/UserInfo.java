/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.directory;

import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.optio3.infra.LdapHelper;

public class UserInfo
{
    public String user;
    public String emailAddress;
    public String password;

    public List<RoleType> roles = Lists.newArrayList();

    public String  ldapDn;
    public boolean referToLdap;

    @JsonIgnore
    public String site;

    @JsonIgnore
    private UserInfo m_effectiveCredentials;

    //--//

    @JsonIgnore
    void setEffectiveCredentials(UserInfo ui)
    {
        m_effectiveCredentials = ui;
    }

    @JsonIgnore
    public UserInfo getEffectiveCredentials()
    {
        return m_effectiveCredentials != null ? m_effectiveCredentials : this;
    }

    @JsonIgnore
    public String getEffectiveEmailAddress()
    {
        if (emailAddress != null)
        {
            return emailAddress;
        }

        return m_effectiveCredentials != null ? m_effectiveCredentials.emailAddress : null;
    }

    @JsonIgnore
    public String getEffectivePassword()
    {
        return getEffectiveCredentials().password;
    }

    public boolean hasRoles(RoleType... args)
    {
        for (RoleType role : args)
        {
            if (!roles.contains(role))
            {
                return false;
            }
        }

        return true;
    }

    public DirContext loginToLdap() throws
                                    NamingException
    {
        return LdapHelper.loginToLdap(site, ldapDn, password);
    }
}
