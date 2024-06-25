/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.exception;

import java.lang.reflect.Constructor;

import javax.validation.constraints.NotNull;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;

public abstract class DetailedApplicationException extends WebApplicationException
{
    private static final long serialVersionUID = 1L;

    @Provider
    public static class Mapper implements ExceptionMapper<DetailedApplicationException>
    {
        @Override
        public Response toResponse(DetailedApplicationException ex)
        {
            return Response.status(Status.PRECONDITION_FAILED)
                           .entity(ex.getDetails())
                           .type(MediaType.APPLICATION_JSON)
                           .build();
        }
    }

    public enum Code
    {
        ALREADY_EXISTS(AlreadyExistsException.class),
        NOT_AUTHENTICATED(NotAuthenticatedException.class),
        NOT_AUTHORIZED(NotAuthorizedException.class),
        NOT_FOUND(NotFoundException.class),
        NOT_IMPLEMENTED(NotImplementedException.class),
        INVALID_ARGUMENT(InvalidArgumentException.class),
        INVALID_STATE(InvalidStateException.class);

        private final Class<? extends DetailedApplicationException> m_cls;

        Code(Class<? extends DetailedApplicationException> cls)
        {
            m_cls = cls;
        }

        public static Code getCode(DetailedApplicationException ex)
        {
            Class<? extends DetailedApplicationException> target = ex.getClass();

            for (Code value : values())
            {
                if (value.m_cls == target)
                {
                    return value;
                }
            }

            throw Exceptions.newIllegalArgumentException("'%s' is not a registered DetailedApplicationException", target.getSimpleName());
        }

        public DetailedApplicationException newInstance(String message,
                                                        String exceptionTrace)
        {
            try
            {
                Constructor<? extends DetailedApplicationException> init = m_cls.getConstructor(String.class, String.class);

                return init.newInstance(message, exceptionTrace);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public static class ErrorDetails
    {
        @NotNull
        public final Code code;

        @NotNull
        public final String message;

        public final String exceptionTrace;

        public final ValidationResults validationErrors;

        @JsonCreator
        public ErrorDetails(@JsonProperty("code") Code code,
                            @JsonProperty("message") String message,
                            @JsonProperty("exceptionTrace") String exceptionTrace,
                            @JsonProperty("validationErrors") ValidationResults validation)
        {
            this.code = code;
            this.message = message;
            this.exceptionTrace = exceptionTrace;
            this.validationErrors = validation;
        }
    }

    private final String m_exceptionTrace;

    protected DetailedApplicationException(String message)
    {
        this(message, (String) null);
    }

    protected DetailedApplicationException(String message,
                                           String exceptionTrace)
    {
        super(message, null, Status.PRECONDITION_FAILED);

        m_exceptionTrace = exceptionTrace;
    }

    protected DetailedApplicationException(String message,
                                           Throwable t)
    {
        super(message, t, Status.PRECONDITION_FAILED);

        if (t != null)
        {
            m_exceptionTrace = Exceptions.convertStackTraceToString(t);
        }
        else
        {
            m_exceptionTrace = null;
        }
    }

    protected void postAllocation(ErrorDetails details)
    {
    }

    public ErrorDetails getDetails()
    {
        return getDetails(null);
    }

    protected ErrorDetails getDetails(ValidationResults errors)
    {
        return new ErrorDetails(Code.getCode(this), getMessage(), m_exceptionTrace, errors);
    }

    public static Throwable tryAndDecode(Throwable t)
    {
        WebApplicationException webEx = Reflection.as(t, WebApplicationException.class);
        if (webEx != null)
        {
            Response response = webEx.getResponse();

            if (response.getStatus() == 412)
            {
                try
                {
                    ErrorDetails details = response.readEntity(ErrorDetails.class);

                    if (details.code != null)
                    {
                        final DetailedApplicationException ex = details.code.newInstance(details.message, details.exceptionTrace);

                        ex.postAllocation(details);
                        return ex;
                    }
                }
                catch (Exception e)
                {
                    // Ignore failures.
                }
            }
        }

        return t;
    }
}
