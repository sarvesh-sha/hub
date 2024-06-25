/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.authentication.jwt;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import javax.validation.constraints.NotNull;
import javax.ws.rs.container.ContainerRequestContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.optio3.cloud.authentication.WellKnownRole;
import com.optio3.cloud.exception.NotAuthenticatedException;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import io.dropwizard.jackson.Jackson;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class CookiePrincipal implements Principal
{
    private static final ObjectMapper s_objectMapper;

    static
    {
        final ObjectMapper mapper = Jackson.newObjectMapper();
        ObjectMappers.configureToSkipNulls(mapper);

        s_objectMapper = mapper;
    }

    //--//

    private final static String PERSISTENT = "pst"; // long-term token == remember me

    private final static String PAYLOAD = "pld";

    private final static String NOAUTOREFRESH = "nar";

    private final static String ROLES = "rls";

    private final static CookiePrincipal s_machine = new CookiePrincipal(WellKnownRole.Machine);

    private final CookiePrincipalRoleResolver m_resolver;
    private final Claims                      m_claims;
    private       boolean                     m_isAuthenticated;
    private       boolean                     m_loggedOut;

    private CookiePrincipal(WellKnownRole... roles)
    {
        m_resolver = null;
        m_claims = Jwts.claims();

        setEmbeddedRolesEx(roles);

        if (isInRole(WellKnownRole.Machine))
        {
            m_claims.setSubject("<machine account>");
            m_isAuthenticated = true;
        }
        else
        {
            m_claims.setSubject("<no subject>");
        }
    }

    private CookiePrincipal(CookiePrincipalRoleResolver resolver,
                            Claims claims)
    {
        m_resolver = Objects.requireNonNull(resolver);
        m_claims = Objects.requireNonNull(claims);
        m_isAuthenticated = true;
    }

    public static CookiePrincipal createEmpty()
    {
        return new CookiePrincipal();
    }

    public static CookiePrincipal getMachine()
    {
        return s_machine;
    }

    @Override
    public String getName()
    {
        return m_claims.getSubject();
    }

    public Claims getClaims()
    {
        return m_claims;
    }

    public boolean isInRole(WellKnownRole role)
    {
        return isInRole(role.getId());
    }

    public boolean isInRole(String role)
    {
        Set<String> roles = getEmbeddedRoles();
        if (roles != null && roles.contains(role))
        {
            return true;
        }

        return m_resolver != null && m_resolver.hasRole(this, role);
    }

    public void setEmbeddedRolesEx(WellKnownRole... roles)
    {
        Set<String> roles2;

        if (roles == null || roles.length == 0)
        {
            roles2 = null;
        }
        else
        {
            roles2 = Sets.newHashSet();
            for (WellKnownRole role : roles)
            {
                roles2.add(role.getId());
            }
        }

        setEmbeddedRoles(roles2);
    }

    public void setEmbeddedRoles(Set<String> roles)
    {
        String[] rolesArray;

        if (roles == null)
        {
            rolesArray = null;
        }
        else
        {
            rolesArray = new String[roles.size()];
            roles.toArray(rolesArray);
        }

        setClaim(ROLES, rolesArray);
    }

    public Set<String> getEmbeddedRoles()
    {
        String[] roles = getClaim(ROLES, String[].class);
        return roles != null ? Sets.newHashSet(roles) : null;
    }

    public Set<String> getRoles()
    {
        Set<String> roles = getEmbeddedRoles();
        if (roles != null)
        {
            return roles;
        }

        return m_resolver != null ? m_resolver.getRoles(this) : Collections.emptySet();
    }

    public void disableAutoRefresh()
    {
        setClaim(NOAUTOREFRESH);
    }

    public Date getExpirationDate()
    {
        return m_claims.getExpiration();
    }

    public void setExpirationDate(Duration expiresIn)
    {
        m_claims.setExpiration(Date.from(Instant.now()
                                                .plus(expiresIn)));
    }

    public boolean shouldRefreshToken(Duration validity)
    {
        if (isClaimSet(NOAUTOREFRESH))
        {
            return false;
        }

        Date exp = m_claims.getExpiration();
        if (exp == null)
        {
            return true;
        }

        Date nowPlusValidity = Date.from(Instant.now()
                                                .plus(validity));

        return exp.before(nowPlusValidity);
    }

    public boolean isPersistent()
    {
        return isClaimSet(PERSISTENT);
    }

    public void setPersistent(boolean value)
    {
        setClaim(PERSISTENT);
    }

    public <T> T getPayload(Class<T> clz)
    {
        return getClaim(PAYLOAD, clz);
    }

    public void setPayload(Object value)
    {
        setClaim(PAYLOAD, value);
    }

    //--//

    public void setClaim(String name)
    {
        m_claims.put(name, Boolean.TRUE);
    }

    public boolean isClaimSet(String name)
    {
        return m_claims.get(name) == Boolean.TRUE;
    }

    public <T> T getClaim(String name,
                          Class<T> clz)
    {
        String json = m_claims.get(name, String.class);
        if (json == null)
        {
            return null;
        }

        try
        {
            return s_objectMapper.readValue(json, clz);
        }
        catch (Exception e)
        {
            // Bad format, discard.
            return null;
        }
    }

    public void setClaim(String name,
                         Object value)
    {
        String json;

        try
        {
            json = s_objectMapper.writeValueAsString(value);
        }
        catch (Exception e)
        {
            json = null;
        }

        m_claims.put(name, json);
    }

    //--//

    public static @NotNull CookiePrincipal newInstance(CookiePrincipalRoleResolver resolver,
                                                       String subject)
    {
        return new CookiePrincipal(resolver,
                                   Jwts.claims()
                                       .setSubject(subject));
    }

    public static @NotNull CookiePrincipal decode(CookiePrincipalRoleResolver resolver,
                                                  Key key,
                                                  String token)
    {
        return new CookiePrincipal(resolver, fromJwt(key, token));
    }

    public static @NotNull CookiePrincipal getFromContext(ContainerRequestContext context)
    {
        return getFromContext(context, false);
    }

    static @NotNull CookiePrincipal getFromContext(ContainerRequestContext context,
                                                   boolean inResponse)
    {
        CookiePrincipal principal;

        CookieSecurityContext secCtx = Reflection.as(context.getSecurityContext(), CookieSecurityContext.class);
        if (secCtx != null)
        {
            principal = (CookiePrincipal) secCtx.getUserPrincipal();
        }
        else
        {
            principal = CookiePrincipal.createEmpty();

            if (!inResponse) // Can't set context in response phase
            {
                principal.addInContext(context);
            }
        }

        return principal;
    }

    public void addInContext(ContainerRequestContext context)
    {
        context.setSecurityContext(new CookieSecurityContext(this, context));
    }

    public @NotNull CookiePrincipal impersonate(String subject)
    {
        return newInstance(m_resolver, subject);
    }

    public void ensureAuthenticated()
    {
        if (!isAuthenticated())
        {
            throw new NotAuthenticatedException(null);
        }
    }

    public void ensureInAnyRole(WellKnownRole... roles)
    {
        for (WellKnownRole role : roles)
        {
            if (isInRole(role))
            {
                return;
            }
        }

        throw new NotAuthenticatedException(String.format("Must have any of these roles: %s", String.join(", ", printRoles(roles))));
    }

    public void ensureInAllRoles(WellKnownRole... roles)
    {
        for (WellKnownRole role : roles)
        {
            if (!isInRole(role))
            {
                throw new NotAuthenticatedException(String.format("Must have all these roles: %s", printRoles(roles)));
            }
        }
    }

    public boolean isAuthenticated()
    {
        return m_isAuthenticated;
    }

    public boolean isLoggedIn()
    {
        return isAuthenticated() && !wasLoggedOut();
    }

    public void markAsLoggedOut()
    {
        m_loggedOut = true;
    }

    public boolean wasLoggedOut()
    {
        return m_loggedOut;
    }

    private static String printRoles(WellKnownRole... roles)
    {
        StringBuilder sb = new StringBuilder();

        for (WellKnownRole role : roles)
        {
            if (sb.length() > 0)
            {
                sb.append(", ");
            }

            sb.append(role.getDisplayName());
        }

        return sb.toString();
    }

    //--//

    public static Claims fromJwt(Key key,
                                 String token)
    {
        return Jwts.parser()
                   .setSigningKey(key)
                   .parseClaimsJws(token)
                   .getBody();
    }

    public String toJwt(Key signingKey)
    {
        JwtBuilder builder = Jwts.builder();

        builder.signWith(SignatureAlgorithm.HS256, signingKey);
        builder.setClaims(m_claims);

        return builder.compact();
    }

    public static Key generateKey(String secretSeed)
    {
        try
        {
            String algo = SignatureAlgorithm.HS256.getJcaName();

            if (secretSeed != null)
            {
                byte[] hash = Hashing.sha256()
                                     .newHasher()
                                     .putString(secretSeed, StandardCharsets.UTF_8)
                                     .hash()
                                     .asBytes();
                return new SecretKeySpec(hash, algo);
            }
            else
            {
                return KeyGenerator.getInstance(algo)
                                   .generateKey();
            }
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new SecurityException(e);
        }
    }
}
