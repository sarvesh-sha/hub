/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.authentication;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.exception.NotAuthorizedException;
import com.optio3.cloud.persistence.HashedPassword;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.FunctionWithException;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.lang3.StringUtils;

public abstract class IdentityAdapter<U, R>
{
    public static class EntryWithTimeout<T>
    {
        private final MonotonousTime m_timestamp = TimeUtils.computeTimeoutExpiration(30, TimeUnit.MINUTES);

        public final T value;

        EntryWithTimeout(T value)
        {
            this.value = value;
        }

        boolean isExpired()
        {
            return TimeUtils.isTimeoutExpired(m_timestamp);
        }
    }

    static class Caches
    {
        final LRUMap<String, EntryWithTimeout<HashedPassword>> sysIdToPwd   = new LRUMap<>(1000);
        final LRUMap<String, String>                           emailToSysId = new LRUMap<>(1000);
    }

    private final Caches m_caches = new Caches();

    public abstract void initialize();

    public boolean authenticate(SessionHolder sessionHolder,
                                @NotNull CookiePrincipal principal,
                                String password,
                                boolean useCachedInfo)
    {
        return authenticate(sessionHolder, principal.getName(), password, useCachedInfo);
    }

    public abstract boolean authenticate(SessionHolder sessionHolder,
                                         String principal,
                                         String password,
                                         boolean useCachedInfo);

    public abstract String createUser(SessionHolder sessionHolder,
                                      String principal,
                                      String firstName,
                                      String lastName,
                                      String password);

    public abstract boolean changePassword(SessionHolder sessionHolder,
                                           String principal,
                                           String newPassword);

    public abstract boolean deleteUser(SessionHolder sessionHolder,
                                       U rec_user);

    public abstract boolean addUserToGroup(SessionHolder sessionHolder,
                                           String emailAddress,
                                           R role);

    public abstract boolean removeUserFromGroup(SessionHolder sessionHolder,
                                                String emailAddress,
                                                R role);

    //--//

    public abstract List<U> listUsers(SessionHolder sessionHolder) throws
                                                                   Exception;

    public U findUser(SessionHolder sessionHolder,
                      @NotNull CookiePrincipal principal,
                      boolean useCachedInfo)
    {
        return findUser(sessionHolder, principal.getName(), useCachedInfo);
    }

    public abstract U findUser(SessionHolder sessionHolder,
                               String principal,
                               boolean useCachedInfo);

    public abstract String getUserId(SessionHolder sessionHolder,
                                     U user) throws
                                             Exception;

    public abstract U getUser(SessionHolder sessionHolder,
                              String sysId) throws
                                            Exception;

    public U getUserWithAuthentication(SessionHolder sessionHolder,
                                       String sysId,
                                       CookiePrincipalAccessor principalAccessor,
                                       FunctionWithException<U, Boolean> overrideCallback) throws
                                                                                           Exception
    {
        CookiePrincipal principal = CookiePrincipalAccessor.get(principalAccessor);
        principal.ensureAuthenticated();

        U user_principal = findUser(sessionHolder, principal.getName(), true);

        if (sysId == null)
        {
            return user_principal;
        }

        U user = getUser(sessionHolder, sysId);

        if (user != user_principal)
        {
            if (overrideCallback == null || overrideCallback.apply(user_principal) != true)
            {
                throw new NotAuthorizedException(null);
            }
        }

        return user;
    }

    public abstract List<R> getRoles(SessionHolder sessionHolder,
                                     U user);

    //--//

    public abstract List<R> listRoles(SessionHolder sessionHolder);

    public abstract String getRoleId(SessionHolder sessionHolder,
                                     R role) throws
                                             Exception;

    public abstract R getRole(SessionHolder sessionHolder,
                              String id);

    public abstract boolean hasRoles(U user,
                                     @SuppressWarnings("unchecked") R... roles);

    //--//

    public void invalidateCaches()
    {
        synchronized (m_caches)
        {
            m_caches.emailToSysId.clear();
            m_caches.sysIdToPwd.clear();
        }
    }

    protected String lookupCachedEmail(String email)
    {
        synchronized (m_caches)
        {
            return m_caches.emailToSysId.get(StringUtils.trim(email));
        }
    }

    protected Boolean authenticateWithCachedPassword(String sysId,
                                                     String password)
    {
        HashedPassword pwd;

        synchronized (m_caches)
        {
            EntryWithTimeout<HashedPassword> entry = m_caches.sysIdToPwd.get(sysId);
            if (entry != null)
            {
                if (entry.isExpired())
                {
                    m_caches.sysIdToPwd.remove(sysId);
                    pwd = null;
                }
                else
                {
                    pwd = entry.value;
                }
            }
            else
            {
                pwd = null;
            }
        }

        if (pwd != null)
        {
            return pwd.authenticate(password);
        }

        return null;
    }

    protected void trackEmailToSysId(String email,
                                     String sysId)
    {
        synchronized (m_caches)
        {
            m_caches.emailToSysId.put(email, sysId);
        }
    }

    protected void cacheHashOfValidPassword(String sysId,
                                            String password)
    {
        synchronized (m_caches)
        {
            m_caches.sysIdToPwd.put(sysId, new EntryWithTimeout<>(new HashedPassword(password)));
        }
    }
}
