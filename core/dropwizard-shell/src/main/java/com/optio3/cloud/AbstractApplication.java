/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud;

import static com.optio3.util.Exceptions.getAndUnwrapException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletRegistration;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.optio3.asyncawait.CompileTime;
import com.optio3.cloud.annotation.Optio3IncludeInApiDefinitions;
import com.optio3.cloud.annotation.Optio3MessageBusChannel;
import com.optio3.cloud.annotation.Optio3NoAuthenticationNeeded;
import com.optio3.cloud.annotation.Optio3Principal;
import com.optio3.cloud.annotation.Optio3RemotableEndpoint;
import com.optio3.cloud.annotation.Optio3RemoteOrigin;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.annotation.Optio3WebSocketEndpoint;
import com.optio3.cloud.authentication.jwt.CookieAuthBundle;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.authentication.jwt.CookiePrincipalProvider;
import com.optio3.cloud.authentication.jwt.CookiePrincipalRoleResolver;
import com.optio3.cloud.exception.DetailedApplicationException;
import com.optio3.cloud.messagebus.ChannelSecurityPolicy;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.MessageBusChannelProvider;
import com.optio3.cloud.messagebus.PeeringProvider;
import com.optio3.cloud.messagebus.channel.RemoteOriginProvider;
import com.optio3.cloud.messagebus.channel.RpcClient;
import com.optio3.cloud.messagebus.channel.RpcWorker;
import com.optio3.cloud.messagebus.transport.StableIdentity;
import com.optio3.cloud.remoting.CallMarshaller;
import com.optio3.collection.Memoizer;
import com.optio3.concurrency.CompletableFutureWithSafeTimeout;
import com.optio3.lang.RunnableWithException;
import com.optio3.logging.Logger;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.service.IServiceProvider;
import com.optio3.text.TextSubstitution;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.FileSystem;
import com.optio3.util.Resources;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.ConsumerWithException;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.jersey.errors.ErrorMessage;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.jersey.setup.JerseyServletContainer;
import io.dropwizard.jetty.GzipHandlerFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentAction;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Subparser;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.component.LifeCycle;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InjectionResolver;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.inject.InstanceBinding;
import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;

public abstract class AbstractApplication<T extends AbstractConfiguration> extends Application<T> implements IServiceProvider
{
    static
    {
        //
        // The MessageBus in the Shell uses our async/await framework.
        // Because of that, we force the initialization here.
        //
        CompileTime.bootstrap();
    }

    public interface IConfigurationInspector
    {
        void beforeInitialize(Bootstrap<?> bootstrap);

        void beforeRun(AbstractConfiguration configuration,
                       Environment environment) throws
                                                Exception;
    }

    //--//

    @Provider
    public static class MapperForExceptionClass implements ExceptionMapper<Exception>
    {
        @Override
        public Response toResponse(Exception exception)
        {
            return fromExceptionToResponse(exception);
        }
    }

    @Provider
    public static class MapperForErrorClass implements ExceptionMapper<Error>
    {
        @Override
        public Response toResponse(Error exception)
        {
            return fromExceptionToResponse(exception);
        }
    }

    private static Response fromExceptionToResponse(Throwable exception)
    {
        // If we're dealing with a web exception, we can service certain types of request (like
        // redirection or server errors) better and also propagate properties of the inner response.
        if (exception instanceof WebApplicationException)
        {
            final Response response = ((WebApplicationException) exception).getResponse();
            Response.Status.Family family = response.getStatusInfo()
                                                    .getFamily();
            if (family.equals(Response.Status.Family.REDIRECTION))
            {
                return response;
            }

            if (family.equals(Response.Status.Family.SERVER_ERROR))
            {
                LoggerInstance.error("There was an error processing a request: %s", exception);
            }

            return Response.fromResponse(response)
                           .type(MediaType.APPLICATION_JSON_TYPE)
                           .entity(new ErrorMessage(response.getStatus(), exception.getLocalizedMessage()))
                           .build();
        }

        LoggerInstance.error("There was an error processing a request: %s", exception);

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                       .type(MediaType.APPLICATION_JSON_TYPE)
                       .entity(new ErrorMessage("There was an error processing your request"))
                       .build();
    }

    //--//

    public static final Logger LoggerInstance = new Logger(AbstractApplication.class);

    //--//

    protected     Bootstrap<T>               m_bootstrap;
    protected     T                          m_configuration;
    protected     Environment                m_environment;
    private       InjectionManager           m_injectionManager;
    private final Supplier<MessageBusBroker> m_messageBusBrokerSupplier = Suppliers.memoize(MessageBusBroker::new);

    private       Server                                   m_server;
    private final Thread                                   m_shutdownThread;
    private       CompletableFuture<Void>                  m_shutdownStarted;
    private final ConcurrentMap<String, RequestStatistics> m_requestStats = Maps.newConcurrentMap();

    private String m_appVersion;

    /**
     * Set to true to enable variable substitutions in the configuration file, using ${env.VAR>}, ${sys.PROP} or application-specific values.
     */
    protected boolean enableVariableSubstition;

    private final TextSubstitution m_substitution;

    private CookieAuthBundle<T> m_authBundle;

    private final Map<Class<?>, Callable<?>>      m_registeredServices = Maps.newHashMap();
    private final Map<String, Optio3AssetServlet> m_assets             = Maps.newHashMap();

    protected Set<Class<?>> m_extraModels = Sets.newHashSet();

    private FilterRegistration.Dynamic m_cors;

    private ProxyFactory m_proxyFactory;

    private CompletableFuture<RpcClient>                m_rpc     = new CompletableFuture<>();
    private CompletableFutureWithSafeTimeout<RpcClient> m_rpcSafe = new CompletableFutureWithSafeTimeout<>(m_rpc);
    private RpcWorker.BaseHeartbeat<?, T>               m_rpcHeartbeat;
    private CompletableFuture<Void>                     m_rpcHeartbeatFlush;

    private final CallMarshaller m_marshaller = new CallMarshaller();

    private final Supplier<Memoizer> m_memoizerSupplier = Suppliers.memoize(Memoizer::new);

    //--//

    protected AbstractApplication()
    {
        m_shutdownThread = new Thread(this::onServerStoppingTrigger);

        //
        // When running inside Docker, the system has a GMT timezone.
        // This looks up an environment variable and uses it to change the timezone of this process.
        //
        String timeZone = System.getenv("OPTIO3_TIMEZONE");
        if (timeZone != null)
        {
            TimeUtils.setDefault(TimeZone.getTimeZone(timeZone));
        }

        //
        // A temporary Service Locator, so we can partially inject early in the boot process.
        //
        setInjectionManager(Injections.createInjectionManager());

        m_substitution = new TextSubstitution();
        m_substitution.addHandler("sys.", System::getProperty);

        //--//

        registerService((Class<AbstractApplication<T>>) getClass(), () -> this);
        registerService(AbstractApplication.class, () -> this);

        registerService(getConfigurationClass(), () -> m_configuration);
        registerService(AbstractConfiguration.class, () -> m_configuration);

        //--//

        registerService(Bootstrap.class, () -> m_bootstrap);
        registerService(Environment.class, () -> m_environment);
        registerService(InjectionManager.class, () -> m_injectionManager);
        registerService(MessageBusBroker.class, m_messageBusBrokerSupplier::get);
        registerService(CallMarshaller.class, () -> m_marshaller);
        registerService(ObjectMapper.class, () -> m_environment.getObjectMapper());
        registerService(Memoizer.class, m_memoizerSupplier::get);
    }

    @Override
    public final void initialize(Bootstrap<T> bootstrap)
    {
        IConfigurationInspector inspector = getService(IConfigurationInspector.class);
        if (inspector != null)
        {
            inspector.beforeInitialize(bootstrap);
        }

        LegacyLogging.configureLevels();

        if (enableVariableSubstition)
        {
            ConfigurationSourceProvider prov = bootstrap.getConfigurationSourceProvider();

            ConfigurationSourceProvider provNew = new ConfigurationSourceProvider()
            {
                @Override
                public InputStream open(String path) throws
                                                     IOException
                {
                    try (final InputStream in = prov.open(path))
                    {
                        final String config      = new String(ByteStreams.toByteArray(in), StandardCharsets.UTF_8);
                        final String substituted = m_substitution.transform(config);

                        return new ByteArrayInputStream(substituted.getBytes(StandardCharsets.UTF_8));
                    }
                }
            };

            bootstrap.setConfigurationSourceProvider(provNew);
        }

        m_bootstrap = bootstrap;

        try
        {
            initialize();
        }
        catch (Exception e)
        {
            e.printStackTrace();

            if (e instanceof RuntimeException)
            {
                throw (RuntimeException) e;
            }

            throw new RuntimeException(e);
        }
    }

    protected abstract void initialize();

    @Override
    public final void run(T configuration,
                          Environment environment) throws
                                                   Exception
    {
        if (configuration.scratchDirectory != null)
        {
            FileSystem.setTempDirectory(new File(configuration.scratchDirectory));
        }

        //
        // Configure the Gzip filter to work on POST/GET and various textual formats.
        //
        DefaultServerFactory server = Reflection.as(configuration.getServerFactory(), DefaultServerFactory.class);
        if (server != null)
        {
            GzipHandlerFactory gzip = server.getGzipFilterFactory();
            if (gzip != null)
            {
                gzip.setIncludedMethods(Sets.newHashSet("POST", "GET"));
                gzip.setCompressedMimeTypes(Sets.newHashSet("application/cbor", "application/json", "application/javascript", "application/csv", "text/plain"));
            }
        }

        IConfigurationInspector inspector = getService(IConfigurationInspector.class);
        if (inspector != null)
        {
            inspector.beforeRun(configuration, environment);
        }

        LegacyLogging.redirect();

        m_configuration = configuration;
        m_environment   = environment;

        registerForServerLifecycleNotifications();

        if (enablePeeringProtocol())
        {
            //
            // Initialize the Peering protocol for the MessageBus
            //
            PeeringProvider.init(this);
        }

        //--//

        ObjectMapper objectMapper = m_environment.getObjectMapper();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        ObjectMappers.RestDefaults = objectMapper;

        m_proxyFactory = new ProxyFactory(objectMapper);

        JerseyEnvironment jersey = m_environment.jersey();
        registerWithJersey(jersey);

        //--//

        try
        {
            run();
        }
        catch (Exception e)
        {
            e.printStackTrace();

            throw e;
        }
    }

    protected void registerWithJersey(JerseyEnvironment jersey) throws
                                                                Exception
    {
        jersey.register(new DetailedApplicationException.Mapper());

        jersey.register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bind(new MapperForExceptionClass()).to(ExceptionMapper.class);
                bind(new MapperForErrorClass()).to(ExceptionMapper.class);
            }
        });

        //--//

        if (m_authBundle != null)
        {
            jersey.register(new AbstractBinder()
            {
                @Override
                protected void configure()
                {
                    bind(new CookiePrincipalProvider(AbstractApplication.this)).to(new GenericType<InjectionResolver<Optio3Principal>>()
                    {
                    });
                }
            });
        }

        //--//

        //
        // Register handlers that associate state with requests.
        //
        jersey.register(new Optio3RequestLogFactory.RequestLogDynamicFeature(m_requestStats));

        //--//

        jersey.register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bind(new RemoteOriginProvider()).to(new GenericType<InjectionResolver<Optio3RemoteOrigin>>()
                {
                });
            }
        });
    }

    private void registerForServerLifecycleNotifications()
    {
        Runtime.getRuntime()
               .addShutdownHook(m_shutdownThread);

        m_environment.lifecycle()
                     .addServerLifecycleListener(new ServerLifecycleListener()
                     {
                         @Override
                         public void serverStarted(Server server)
                         {
                             m_server = server;

                             server.addLifeCycleListener(new LifeCycle.Listener()
                             {
                                 @Override
                                 public void lifeCycleStopping(LifeCycle event)
                                 {
                                     if (event instanceof Server)
                                     {
                                         try
                                         {
                                             onServerStoppingTrigger().get(1, TimeUnit.MINUTES);
                                         }
                                         catch (Exception e)
                                         {
                                             LoggerInstance.error("Stopping failed with %s", e);
                                         }
                                     }
                                 }

                                 @Override
                                 public void lifeCycleStopped(LifeCycle event)
                                 {
                                     if (event instanceof Server)
                                     {
                                         onServerStopped((Server) event);
                                     }
                                 }

                                 @Override
                                 public void lifeCycleStarting(LifeCycle event)
                                 {
                                 }

                                 @Override
                                 public void lifeCycleStarted(LifeCycle event)
                                 {
                                 }

                                 @Override
                                 public void lifeCycleFailure(LifeCycle event,
                                                              Throwable cause)
                                 {
                                 }
                             });

                             JerseyServletContainer container = (JerseyServletContainer) m_environment.getJerseyServletContainer();

                             setInjectionManager(container.getApplicationHandler()
                                                          .getInjectionManager());

                             onServerStarted();
                         }
                     });
    }

    protected abstract boolean enablePeeringProtocol();

    protected abstract void run() throws
                                  Exception;

    protected void onServerStarted()
    {
    }

    private CompletableFuture<Void> onServerStoppingTrigger()
    {
        synchronized (m_shutdownThread)
        {
            if (m_shutdownStarted != null)
            {
                return m_shutdownStarted;
            }

            m_shutdownStarted = new CompletableFuture<>();
        }

        try
        {
            Runtime.getRuntime()
                   .removeShutdownHook(m_shutdownThread);
        }
        catch (Throwable t)
        {
            // Ignore failure, it happens if shutdown has already started.
        }

        try
        {
            onServerStopping();
        }
        catch (Throwable t)
        {
            // Ignore failure, it happens if shutdown has already started.
        }

        m_shutdownStarted.complete(null);
        return m_shutdownStarted;
    }

    protected void onServerStopping()
    {
        try
        {
            cleanupOnShutdown(2, TimeUnit.MINUTES);
        }
        catch (Exception e)
        {
            LoggerInstance.error("Caught exception during shutdown: %s", e);
        }
    }

    protected void onServerStopped(Server server)
    {
    }

    public final void shutdown() throws
                                 Exception
    {
        if (m_server != null)
        {
            m_server.stop();
        }
    }

    public void cleanupOnShutdown(long timeout,
                                  TimeUnit unit) throws
                                                 Exception
    {
    }

    //--//

    public <S> void registerService(Class<S> clz,
                                    Callable<S> generator)
    {
        m_registeredServices.put(clz, generator);
    }

    @Override
    public final <S> S getService(Class<S> serviceClass)
    {
        try
        {
            Callable<?> callback = m_registeredServices.get(serviceClass);
            return callback != null ? serviceClass.cast(callback.call()) : null;
        }
        catch (Exception e)
        {
            LoggerInstance.error("Failed to generate service '%s': %s", serviceClass.getName(), e);

            return null;
        }
    }

    //--//

    public String getAppVersion()
    {
        return m_appVersion;
    }

    protected HttpConnectorFactory getApplicationConnector()
    {
        AbstractConfiguration cfg = getServiceNonNull(AbstractConfiguration.class);

        DefaultServerFactory serverFactory = (DefaultServerFactory) cfg.getServerFactory();
        return (HttpConnectorFactory) CollectionUtils.firstElement(serverFactory.getApplicationConnectors());
    }

    protected void setInjectionManager(InjectionManager injectionManager)
    {
        m_injectionManager = injectionManager;

        //
        // Make the application available through "@Inject AbstractApplication<T> var".
        //
        bindNewInstanceToInjectionManager(this);
        bindNewInstanceToInjectionManager(this, AbstractApplication.class);

        if (m_environment != null)
        {
            //
            // Make the ObjectMapper available through "@Inject ObjectMapper var".
            //
            bindNewInstanceToInjectionManager(m_environment.getObjectMapper(), ObjectMapper.class);
        }

        if (m_configuration != null)
        {
            //
            // Make the configuration available through "@Inject AbstractConfiguration var".
            //
            bindNewInstanceToInjectionManager(m_configuration, getConfigurationClass());
        }
    }

    private <T> void bindNewInstanceToInjectionManager(T instance)
    {
        @SuppressWarnings("unchecked") Class<T> clz = (Class<T>) instance.getClass();

        bindNewInstanceToInjectionManager(instance, clz);
    }

    private <T> void bindNewInstanceToInjectionManager(T instance,
                                                       Class<T> clz)
    {
        InstanceBinding<T> binder = Bindings.service(instance)
                                            .to(clz);

        m_injectionManager.register(binder);
    }

    //--//

    protected void enableAuthentication(CookiePrincipalRoleResolver resolver)
    {
        m_authBundle = new CookieAuthBundle<>(null, resolver);
        m_bootstrap.addBundle(m_authBundle);
    }

    public @NotNull CookiePrincipal buildPrincipal(String subject)
    {
        return m_authBundle != null ? m_authBundle.buildPrincipal(subject) : CookiePrincipal.createEmpty();
    }

    //--//

    protected void stopServer()
    {
        try
        {
            m_environment.getApplicationContext()
                         .getServer()
                         .stop();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    //--//

    protected void serveAssets(String resourcePath,
                               String uriPath,
                               String indexFile,
                               String assetsName,
                               boolean useIndexHashAsAppVersion,
                               Function<String, Boolean> allowAssetAccess)
    {
        if (!resourcePath.endsWith("/"))
        {
            resourcePath += '/';
        }

        if (!uriPath.endsWith("/"))
        {
            uriPath += '/';
        }

        synchronized (m_assets)
        {
            if (!m_assets.containsKey(uriPath))
            {
                if (useIndexHashAsAppVersion)
                {
                    try
                    {
                        for (String line : Resources.loadResourceAsLines(getClass(), resourcePath.substring(1) + indexFile, false))
                        {
                            final String marker = "OPTIO3_VERSION=\"";

                            int pos = line.indexOf(marker);
                            if (pos > 0)
                            {
                                pos += marker.length();

                                int posEnd = line.indexOf('"', pos);
                                if (posEnd > 0)
                                {
                                    m_appVersion = line.substring(pos, posEnd);
                                    break;
                                }
                            }
                        }
                    }
                    catch (Throwable t)
                    {
                        LoggerInstance.error("Failed to checksum the App Version, due to %s", t);
                    }
                }

                Optio3AssetServlet servlet = new Optio3AssetServlet(resourcePath, uriPath, indexFile, StandardCharsets.UTF_8, allowAssetAccess);

                m_environment.servlets()
                             .addServlet(assetsName, servlet)
                             .addMapping(uriPath + '*');

                m_assets.put(uriPath, servlet);
            }
        }
    }

    //--//

    protected void discoverWebSockets(String jerseyRootPath,
                                      String packagePrefix)
    {
        Reflections reflections = new Reflections(packagePrefix, new TypeAnnotationsScanner());

        for (Class<?> t : reflections.getTypesAnnotatedWith(Optio3WebSocketEndpoint.class, true))
        {
            Optio3WebSocketEndpoint anno = t.getAnnotation(Optio3WebSocketEndpoint.class);

            //
            // As we discover WebSocket, keep tab of the JsonWebSockets, to add their models to the Swagger specification.
            //
            if (JsonWebSocket.class.isAssignableFrom(t))
            {
                Class<?> tArg = Reflection.searchTypeArgument(JsonWebSocket.class, t, 0);
                if (tArg == null)
                {
                    throw Exceptions.newRuntimeException("Can't get type argument for type '%s'", t);
                }

                addNonTrivialModel(tArg);
            }

            AuthFilter<?, ?> authFilter = null;

            if (!t.isAnnotationPresent(Optio3NoAuthenticationNeeded.class))
            {
                if (m_authBundle != null)
                {
                    authFilter = m_authBundle.getAuthRequestFilter();
                }
            }

            @SuppressWarnings("serial") WebSocketWrapper wrapper = new WebSocketWrapper(anno.timeout(), authFilter, t, anno.urlPatterns()[0], m_requestStats)
            {
                @Override
                protected Object createNewInstance()
                {
                    return Injections.getOrCreate(m_injectionManager, t);
                }
            };

            ServletRegistration.Dynamic reg = m_environment.servlets()
                                                           .addServlet(anno.name(), wrapper);

            for (String mapping : anno.urlPatterns())
            {
                reg.addMapping(jerseyRootPath + mapping);
            }
        }
    }

    protected void discoverExtraModels(String packagePrefix)
    {
        Reflections reflections = new Reflections(packagePrefix, new TypeAnnotationsScanner());

        for (Class<?> t : reflections.getTypesAnnotatedWith(Optio3IncludeInApiDefinitions.class, true))
        {
            addNonTrivialModel(t);
        }

        for (Class<?> t : reflections.getTypesAnnotatedWith(JsonTypeName.class, true))
        {
            if (!isPresentInParent(t))
            {
                throw Exceptions.newRuntimeException("Type '%s' has @JsonTypeName but it's not listed in its parent", t.getName());
            }
        }
    }

    private static boolean isPresentInParent(Class<?> t)
    {
        JsonTypeInfo annoDef = t.getAnnotation(JsonTypeInfo.class);
        if (annoDef != null)
        {
            // Root of the hierarchy.
            return true;
        }

        Class<?> tSuper = t.getSuperclass();
        if (tSuper == null)
        {
            return true;
        }

        JsonSubTypes anno = tSuper.getAnnotation(JsonSubTypes.class);
        if (anno == null)
        {
            return false;
        }

        for (JsonSubTypes.Type type : anno.value())
        {
            Class<?> subClz = type.value();

            if (subClz == t)
            {
                return true;
            }
        }

        return false;
    }

    protected void discoverMessageBusChannels(String packagePrefix)
    {
        Reflections reflections = new Reflections(packagePrefix, new TypeAnnotationsScanner());

        for (Class<?> t : reflections.getTypesAnnotatedWith(Optio3MessageBusChannel.class, true))
        {
            if (Reflection.canAssignTo(MessageBusChannelProvider.class, t))
            {
                @SuppressWarnings("unchecked") Class<MessageBusChannelProvider<?, ?>> clz = (Class<MessageBusChannelProvider<?, ?>>) t;

                addMessageBusChannel(clz);
            }
            else
            {
                throw Exceptions.newRuntimeException("@Optio3MessageBusChannel applied to type not extending from MessageBusChannelSubscriber: '%s'", t);
            }
        }
    }

    protected <C extends MessageBusChannelProvider<?, ?>> C addMessageBusChannel(Class<C> t)
    {
        MessageBusBroker        broker = getServiceNonNull(MessageBusBroker.class);
        Optio3MessageBusChannel anno   = t.getAnnotation(Optio3MessageBusChannel.class);
        if (anno == null)
        {
            throw Exceptions.newRuntimeException("Missing @Optio3MessageBusChannel on type '%s'", t);
        }

        Constructor<C> constructor;

        try
        {
            constructor = t.getConstructor(IServiceProvider.class, String.class);
            constructor.setAccessible(true);
        }
        catch (Exception e)
        {
            throw Exceptions.newRuntimeException("Invalid MessageBus Channel Provider constructor for type '%s': %s", t, e);
        }

        try
        {
            C                     provider = constructor.newInstance(this, anno.name());
            ChannelSecurityPolicy policy   = Reflection.as(provider, ChannelSecurityPolicy.class);

            broker.registerChannelProvider(null, (MessageBusChannelProvider<?, ?>) provider, policy);

            addNonTrivialModel(Reflection.searchTypeArgument(MessageBusChannelProvider.class, t, 0));
            addNonTrivialModel(Reflection.searchTypeArgument(MessageBusChannelProvider.class, t, 1));

            return provider;
        }
        catch (Exception e)
        {
            throw Exceptions.newRuntimeException("Failed to instantiate MessageBus Channel provider from type '%s': %s", t, e);
        }
    }

    private void addNonTrivialModel(Class<?> t)
    {
        if (t == null)
        {
            return;
        }

        if (t.isPrimitive())
        {
            return;
        }

        if (t == String.class || t == Object.class)
        {
            return;
        }

        m_extraModels.add(t);
    }

    //--//

    protected void discoverResources(String packagePrefix)
    {
        Reflections reflections = new Reflections(packagePrefix, new TypeAnnotationsScanner());

        for (Class<?> t : reflections.getTypesAnnotatedWith(Optio3RestEndpoint.class, true))
        {
            //
            // Make sure we have well-formatted @Path annotations.
            //
            for (Method m : Reflection.collectMethods(t)
                                      .values())
            {
                Path annoPath = m.getAnnotation(Path.class);
                if (annoPath != null)
                {
                    for (Parameter p : m.getParameters())
                    {
                        PathParam annoPathParam = p.getAnnotation(PathParam.class);
                        if (annoPathParam != null)
                        {
                            if (!annoPath.value()
                                         .contains("{" + annoPathParam.value() + "}"))
                            {
                                throw Exceptions.newRuntimeException("The REST path for %s does not contain the path parameter for %s", m, p);
                            }
                        }
                    }

                    if (!m.isAnnotationPresent(Produces.class))
                    {
                        Class<?> returnType = m.getReturnType();

                        if (returnType == void.class || returnType == String.class || Number.class.isAssignableFrom(returnType))
                        {
                            // These are fine without a Media Type annotation.
                        }
                        else
                        {
                            throw Exceptions.newRuntimeException("The REST path for %s does not specific any media type", m);
                        }
                    }
                }
            }

            try
            {
                Objects.requireNonNull(t.getConstructor());

                m_environment.jersey()
                             .register(t);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    //--//

    public Argument addActionToCommand(Subparser subparser,
                                       String name,
                                       String help,
                                       boolean required,
                                       RunnableWithException callback)
    {
        Argument arg = subparser.addArgument(name);
        arg.help(help);
        arg.required(required);

        arg.action(new ArgumentAction()
        {
            @Override
            public void run(ArgumentParser parser,
                            Argument arg,
                            Map<String, Object> attrs,
                            String flag,
                            Object value) throws
                                          ArgumentParserException
            {
                try
                {
                    callback.run();
                }
                catch (Exception e)
                {
                    throw new ArgumentParserException(e, parser);
                }
            }

            @Override
            public void onAttach(Argument arg)
            {
            }

            @Override
            public boolean consumeArgument()
            {
                return false;
            }
        });

        return arg;
    }

    public Argument addArgumentToCommand(Subparser subparser,
                                         String name,
                                         String help,
                                         boolean required,
                                         ConsumerWithException<String> callback)
    {
        Argument arg = subparser.addArgument(name);
        arg.help(help);
        arg.required(required);

        arg.action(new ArgumentAction()
        {

            @Override
            public void run(ArgumentParser parser,
                            Argument arg,
                            Map<String, Object> attrs,
                            String flag,
                            Object value) throws
                                          ArgumentParserException
            {
                try
                {
                    callback.accept((String) value);
                }
                catch (Exception e)
                {
                    throw new ArgumentParserException(e, parser);
                }
            }

            @Override
            public void onAttach(Argument arg)
            {
            }

            @Override
            public boolean consumeArgument()
            {
                return true;
            }
        });

        return arg;
    }

    //--//

    protected void discoverRemotableEndpoints(String packagePrefix)
    {
        Reflections reflections = new Reflections(packagePrefix, new TypeAnnotationsScanner());

        for (Class<?> t : reflections.getTypesAnnotatedWith(Optio3RemotableEndpoint.class, true))
        {
            registerRemotableEndpoint(t);
        }
    }

    public void registerRemotableEndpoint(Class<?> t)
    {
        m_marshaller.addRemotableEndpoint(t);
    }

    //--//

    protected void enableCORS(String... headers)
    {
        if (m_cors == null)
        {
            m_cors = m_environment.servlets()
                                  .addFilter("CORS", CrossOriginFilter.class);

            m_cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
            m_cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "HEAD,GET,POST,DELETE,PUT,PATCH,OPTIONS");

            // Add URL mapping
            m_cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
        }

        Set<String> params = Sets.newHashSet();
        String      args   = m_cors.getInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM);
        if (args != null)
        {
            for (String arg : args.split(","))
            {
                if (arg.length() > 0)
                {
                    params.add(arg);
                }
            }
        }

        Collections.addAll(params, headers);

        m_cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, String.join(",", params));
    }

    //--//

    public ConcurrentMap<String, RequestStatistics> getRequestStatistics()
    {
        return m_requestStats;
    }

    public Map<String, StableIdentity> getMessageBusStatistics()
    {
        MessageBusBroker messageBusBroker = getServiceNonNull(MessageBusBroker.class);
        return messageBusBroker.getIdentities();
    }

    //--//

    public RpcClient checkIfOnline(String rpcId,
                                   int timeout,
                                   TimeUnit unit) throws
                                                  Exception
    {
        if (rpcId == null)
        {
            return null;
        }

        RpcClient client = getAndUnwrapException(getRpcClient(timeout, unit));

        if (!getAndUnwrapException(client.waitForDestination(rpcId, timeout, unit)))
        {
            return null;
        }

        return client;
    }

    public CompletableFuture<RpcClient> getRpcClient(long timeout,
                                                     TimeUnit unit)
    {
        return m_rpcSafe.waitForCompletion(timeout, unit);
    }

    protected void setRpcClient(RpcClient rpc)
    {
        CompletableFuture<RpcClient> oldRpc;

        synchronized (this)
        {
            if (rpc == null)
            {
                oldRpc = m_rpc;

                m_rpc     = new CompletableFuture<>();
                m_rpcSafe = new CompletableFutureWithSafeTimeout<>(m_rpc);
            }
            else
            {
                oldRpc = null;

                m_rpc.complete(rpc);
            }
        }

        if (oldRpc != null)
        {
            oldRpc.completeExceptionally(new TimeoutException());
        }
    }

    protected void setRpcHeartbeat(RpcWorker.BaseHeartbeat<?, T> hb)
    {
        m_rpcHeartbeat = hb;
    }

    public synchronized void flushHeartbeat(boolean force)
    {
        try
        {
            if (m_rpcHeartbeatFlush != null && m_rpcHeartbeatFlush.isDone())
            {
                m_rpcHeartbeatFlush = null;
            }

            if (m_rpcHeartbeatFlush == null)
            {
                RpcWorker.BaseHeartbeat<?, T> hb = m_rpcHeartbeat;
                if (hb != null)
                {
                    m_rpcHeartbeatFlush = hb.sendCheckin(force);
                }
            }
        }
        catch (Exception e)
        {
            // Ignore failures.
        }
    }

    //--//

    public <P> P createProxy(String baseAddress,
                             Class<P> cls,
                             Object... varValues)
    {
        return m_proxyFactory.createProxy(baseAddress, cls, varValues);
    }

    public <P> P createProxyWithPrincipal(String baseAddress,
                                          Class<P> cls,
                                          @NotNull CookiePrincipal principal,
                                          Object... varValues)
    {
        NewCookie cookie = generateCookie(principal);

        return m_proxyFactory.createProxyWithCookie(baseAddress, cls, cookie, varValues);
    }

    public <P> P createProxyWithCredentials(String baseAddress,
                                            Class<P> cls,
                                            String userName,
                                            String password,
                                            Object... varValues)
    {
        return m_proxyFactory.createProxyWithCredentials(baseAddress, cls, userName, password, varValues);
    }

    public NewCookie generateCookie(@NotNull CookiePrincipal principal)
    {
        return m_authBundle.generateCookie(principal);
    }
}
