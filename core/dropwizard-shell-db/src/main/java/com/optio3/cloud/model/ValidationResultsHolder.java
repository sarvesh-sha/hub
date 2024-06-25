/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.model;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.optio3.cloud.Optio3RequestLogFactory;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.service.IServiceProvider;
import com.optio3.util.BoxingUtils;

public class ValidationResultsHolder implements AutoCloseable,
                                                IServiceProvider
{
    public final  SessionHolder sessionHolder;
    private final boolean       m_releaseSessionHolder;

    private final ValidationResults m_validationResults = new ValidationResults();

    private final boolean m_dryRun;

    private boolean m_force;

    private final Set<Object> m_pendingDelete = Sets.newHashSet();

    public ValidationResultsHolder(SessionHolder sessionHolder,
                                   Boolean dryRun,
                                   boolean force)
    {
        this(sessionHolder, false, dryRun, force);
    }

    public ValidationResultsHolder(SessionProvider sessionProvider,
                                   Boolean dryRun,
                                   boolean force)
    {
        this(sessionProvider.newSessionWithTransaction(), true, dryRun, force);
    }

    private ValidationResultsHolder(SessionHolder sessionHolder,
                                    boolean releaseSessionHolder,
                                    Boolean dryRun,
                                    boolean force)
    {
        m_dryRun = BoxingUtils.get(dryRun);
        m_force = force;

        this.sessionHolder = sessionHolder;
        m_releaseSessionHolder = releaseSessionHolder;

        if (m_dryRun)
        {
            Optio3RequestLogFactory.dontLog();
        }
    }

    @Override
    public void close()
    {
        if (!(m_dryRun || m_force))
        {
            if (!m_validationResults.entries.isEmpty())
            {
                final InvalidStateException ex = new InvalidStateException("Validation Failed");
                ex.validationErrors = m_validationResults;
                throw ex;
            }
        }

        if (m_releaseSessionHolder)
        {
            try
            {
                if (canProceed())
                {
                    sessionHolder.commit();
                }
            }
            finally
            {
                sessionHolder.close();
            }
        }
    }

    @Override
    public <S> S getService(Class<S> serviceClass)
    {
        return sessionHolder.getService(serviceClass);
    }

    @Nonnull
    @Override
    public <S> S getServiceNonNull(Class<S> serviceClass)
    {
        return sessionHolder.getServiceNonNull(serviceClass);
    }

    //--//

    public ValidationResults getResults()
    {
        return m_validationResults;
    }

    //--//

    public boolean isForced()
    {
        return m_force;
    }

    public boolean canProceed()
    {
        if (m_dryRun)
        {
            return false;
        }

        if (m_force)
        {
            return true;
        }

        return m_validationResults.entries.isEmpty();
    }

    public void checkRole(CookiePrincipalAccessor principalAccessor,
                          String role)
    {
        checkRole(CookiePrincipalAccessor.get(principalAccessor), role);
    }

    public void checkRole(@NotNull CookiePrincipal principal,
                          String role)
    {
        if (principal != null && principal.isInRole(role))
        {
            return;
        }

        addFailure("identity", "Not authorized, must have role '%s'", role);
        m_force = false; // Authorization failures reset the force flag.
    }

    public void checkAnyRoles(CookiePrincipalAccessor principalAccessor,
                              String... roles)
    {
        checkAnyRoles(CookiePrincipalAccessor.get(principalAccessor), roles);
    }

    public void checkAnyRoles(@NotNull CookiePrincipal principal,
                              String... roles)
    {
        if (principal != null)
        {
            for (String role : roles)
            {
                if (principal.isInRole(role))
                {
                    return;
                }
            }
        }

        addFailure("identity", "Not authorized, must have one of the roles '%s'", Lists.newArrayList(roles));
        m_force = false; // Authorization failures reset the force flag.
    }

    public void addFailure(String field,
                           String fmt,
                           Object... args)
    {
        ValidationResult failure = new ValidationResult();
        failure.field = field;
        failure.reason = String.format(fmt, args);
        m_validationResults.entries.add(failure);
    }

    //--//

    public void markAsPendingDelete(Object obj)
    {
        m_pendingDelete.add(obj);
    }

    public boolean isDeletePending(Object obj)
    {
        return m_pendingDelete.contains(obj);
    }

    //--//

    @Override
    public String toString()
    {
        return "ValidationResultsHolder{" + "m_validationResults=" + m_validationResults + ", m_dryRun=" + m_dryRun + ", m_force=" + m_force + '}';
    }
}
