/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.logic;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.asyncawait.AsyncBackground;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.persistence.RecordForBackgroundActivity;
import com.optio3.cloud.persistence.RecordForBackgroundActivityChunk;
import com.optio3.cloud.persistence.RecordForWorker;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.ILogger;
import com.optio3.logging.Logger;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.BiConsumerWithException;
import com.optio3.util.function.BiFunctionWithException;
import com.optio3.util.function.ConsumerWithException;
import com.optio3.util.function.FunctionWithException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;

public abstract class BackgroundActivityHandler<R extends RecordForBackgroundActivity<R, C, W>, C extends RecordForBackgroundActivityChunk<R, C, W>, W extends RecordForWorker<W>>
{
    public interface ICleanupOnFailure
    {
        void cleanupOnFailure(Throwable t) throws
                                           Exception;
    }

    public interface ICleanupOnFailureWithSession
    {
        void cleanupOnFailure(SessionHolder sessionHolder,
                              Throwable t) throws
                                           Exception;
    }

    public interface ICleanupOnComplete
    {
        void cleanupOnComplete() throws
                                 Exception;
    }

    public interface ICleanupOnCompleteWithSession
    {
        void cleanupOnComplete(SessionHolder sessionHolder) throws
                                                            Exception;
    }

    public interface IPostProcess
    {
        void postProcess(Throwable t) throws
                                      Exception;
    }

    public interface IPostProcessWithSession
    {
        void postProcess(SessionHolder sessionHolder,
                         Throwable t) throws
                                      Exception;
    }

    //--//

    public static final DateTimeFormatter DEFAULT_TIMESTAMP       = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    public static final DateTimeFormatter DEFAULT_TIMESTAMP_MILLI = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private static class Descriptor
    {
        Method                   method;
        BackgroundActivityMethod anno;
    }

    //--//

    public interface VariableResolver
    {
        GenericValue resolve(String id);

        String convertToString();
    }

    public static class SubstitutionContext
    {
        public String             result;
        public List<GenericValue> sources    = Lists.newArrayList(); // Only the values that contributed directly to the text.
        public List<GenericValue> references = Lists.newArrayList(); // All the values traversed to resolve the text.

        //--//

        public <T extends GenericValue> T findSource(Class<T> clz,
                                                     int index)
        {
            List<T> res = findInstances(sources, clz);
            return CollectionUtils.getNthElement(res, index);
        }

        public <T extends GenericValue> T findReference(Class<T> clz,
                                                        int index)
        {
            List<T> res = findInstances(references, clz);
            return CollectionUtils.getNthElement(res, index);
        }

        //--//

        public <T extends GenericValue> List<T> getSources(Class<T> clz)
        {
            return CollectionUtils.asEmptyCollectionIfNull(findInstances(sources, clz));
        }

        public <T extends GenericValue> List<T> getReferences(Class<T> clz)
        {
            return CollectionUtils.asEmptyCollectionIfNull(findInstances(references, clz));
        }

        //--//

        private static <T extends GenericValue> List<T> findInstances(List<GenericValue> lst,
                                                                      Class<T> clz)
        {
            List<T> res = null;

            for (GenericValue v : lst)
            {
                T v2 = Reflection.as(v, clz);
                if (v2 != null)
                {
                    if (res == null)
                    {
                        res = Lists.newArrayList();
                    }

                    res.add(v2);
                }
            }

            return res;
        }
    }

    @JsonTypeInfo(use = Id.CLASS)
    public static abstract class GenericValue
    {
        public SubstitutionContext substituteVariables(String text)
        {
            SubstitutionContext sb = new SubstitutionContext();

            StrSubstitutor sub = new StrSubstitutor(new StrLookup<String>()
            {
                @Override
                public String lookup(String key)
                {
                    GenericValue v = resolveVariable(sb, key);

                    sb.sources.add(v);

                    return v.toText();
                }
            });

            sb.result = sub.replace(text);
            return sb;
        }

        public GenericValue resolveVariable(SubstitutionContext sb,
                                            String key)
        {
            GenericValue context = this;

            int lastPos = 0;
            while (lastPos < key.length())
            {
                sb.references.add(context);

                int dotPos = key.indexOf('.', lastPos);

                String prop;
                if (dotPos < 0)
                {
                    prop    = key.substring(lastPos);
                    lastPos = key.length();
                }
                else
                {
                    prop    = key.substring(lastPos, dotPos);
                    lastPos = dotPos + 1;
                }

                context = context.dereferenceProperty(prop);
            }

            return context;
        }

        public String toText()
        {
            VariableResolver resolver = Reflection.as(this, VariableResolver.class);
            if (resolver != null)
            {
                return resolver.convertToString();
            }

            throw Exceptions.newRuntimeException("Value '%s' does not implement toText() method",
                                                 this.getClass()
                                                     .getName());
        }

        //--//

        private GenericValue dereferenceProperty(String prop)
        {
            VariableResolver resolver = Reflection.as(this, VariableResolver.class);
            if (resolver != null)
            {
                return resolver.resolve(prop);
            }

            try
            {
                Method  m;
                Field   f;
                Object  val;
                boolean got;

                if ((m = Reflection.findGetter(this.getClass(), prop)) != null)
                {
                    val = m.invoke(this);
                    got = true;
                }
                else if ((f = Reflection.findField(this.getClass(), prop)) != null)
                {
                    val = f.get(this);
                    got = true;
                }
                else
                {
                    val = null;
                    got = false;
                }

                if (got)
                {
                    GenericValue genericVal = Reflection.as(val, GenericValue.class);
                    return genericVal != null ? genericVal : new PrimitiveValue(val);
                }
            }
            catch (Exception e)
            {
                throw Exceptions.newRuntimeException("Cannot access property '%s' on value '%s': %s",
                                                     prop,
                                                     this.getClass()
                                                         .getName(),
                                                     e);
            }

            throw Exceptions.newRuntimeException("Value '%s' does not have property '%s'",
                                                 this.getClass()
                                                     .getName(),
                                                 prop);
        }
    }

    public final static class PrimitiveValue extends GenericValue implements VariableResolver
    {
        public Object value;

        @JsonCreator
        public PrimitiveValue(Object v)
        {
            value = v;
        }

        //--//

        @Override
        public GenericValue resolve(String id)
        {
            throw Exceptions.newIllegalArgumentException("Primitive value %s, not suitable for dereferencing property '%s'",
                                                         value != null ? value.getClass()
                                                                              .getName() : "<null>",
                                                         id);
        }

        @Override
        public String convertToString()
        {
            return value != null ? value.toString() : null;
        }
    }

    public static class PropertyBagValue extends GenericValue implements VariableResolver
    {
        public Map<String, GenericValue> map = Maps.newHashMap();

        //--//

        public void put(String key,
                        GenericValue value)
        {
            map.put(key, value);
        }

        public <T extends GenericValue> T get(Class<T> clz,
                                              String key)
        {
            GenericValue v = map.get(key);

            return clz.cast(v);
        }

        public <T extends GenericValue> List<T> list(Class<T> clz)
        {
            List<T> res = Lists.newArrayList();

            for (GenericValue v : map.values())
            {
                T v2 = Reflection.as(v, clz);
                if (v2 != null)
                {
                    res.add(v2);
                }
            }

            return res;
        }

        //--//

        @Override
        public GenericValue resolve(String id)
        {
            if (map.containsKey(id))
            {
                return map.get(id);
            }

            throw Exceptions.newIllegalArgumentException("Can't find property '%s'", id);
        }

        @Override
        public String convertToString()
        {
            throw Exceptions.newRuntimeException("Can't convert a property bag to text");
        }
    }

    //--//

    public static class ChunkedProperty<R2 extends RecordForBackgroundActivity<R2, C2, W2>, C2 extends RecordForBackgroundActivityChunk<R2, C2, W2>, W2 extends RecordForWorker<W2>>
    {
        public RecordLocator<C2> loc;
        public Class<?>          chunkClass;

        void put(SessionProvider sessionProvider,
                 Class<C2> clzChunkRecord,
                 RecordLocator<R2> loc_activity,
                 Object value)
        {
            putImpl(sessionProvider, clzChunkRecord, loc_activity, value.getClass(), (chunk) -> chunk.setState(value));
        }

        void putRaw(SessionProvider sessionProvider,
                    Class<C2> clzChunkRecord,
                    RecordLocator<R2> loc_activity,
                    byte[] buffer,
                    int offset,
                    int length)
        {
            putImpl(sessionProvider, clzChunkRecord, loc_activity, byte[].class, (chunk) -> chunk.setRawState(Arrays.copyOfRange(buffer, offset, offset + length)));
        }

        <T> T get(SessionProvider sessionProvider,
                  Class<T> clz)
        {
            return getImpl(sessionProvider, (chunk) -> chunk.getState(clz));
        }

        byte[] getRaw(SessionProvider sessionProvider)
        {
            return getImpl(sessionProvider, RecordForBackgroundActivityChunk::getRawState);
        }

        //--//

        private void putImpl(SessionProvider sessionProvider,
                             Class<C2> clzChunkRecord,
                             RecordLocator<R2> loc_activity,
                             Class<?> chunkClass,
                             Consumer<C2> callback)
        {
            try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
            {
                C2 chunk = sessionHolder.fromLocator(loc);
                if (chunk == null)
                {
                    R2 rec_activity = sessionHolder.fromLocator(loc_activity);

                    chunk = Reflection.newInstance(clzChunkRecord);
                    chunk.setOwningActivity(rec_activity);

                    callback.accept(chunk);

                    sessionHolder.persistEntity(chunk);

                    loc = sessionHolder.createLocator(chunk);
                }
                else
                {
                    this.chunkClass = chunkClass;

                    callback.accept(chunk);
                }

                sessionHolder.commit();
            }
        }

        <T> T getImpl(SessionProvider sessionProvider,
                      Function<C2, T> callback)
        {
            try (SessionHolder sessionHolder = sessionProvider.newReadOnlySession())
            {
                C2 chunk = sessionHolder.fromLocatorOrNull(loc);
                if (chunk == null)
                {
                    return null;
                }

                return callback.apply(chunk);
            }
        }
    }

    public static class ChunkedPropertyBag<R2 extends RecordForBackgroundActivity<R2, C2, W2>, C2 extends RecordForBackgroundActivityChunk<R2, C2, W2>, W2 extends RecordForWorker<W2>>
    {
        public Map<String, ChunkedProperty<R2, C2, W2>> map         = Maps.newHashMap();
        public Map<String, Integer>                     mapSequence = Maps.newHashMap();

        //--//

        void put(SessionProvider sessionProvider,
                 Class<C2> clzChunkRecord,
                 RecordLocator<R2> loc_activity,
                 String key,
                 Object value)
        {
            putImpl(key, (chunk) -> chunk.put(sessionProvider, clzChunkRecord, loc_activity, value));
        }

        <T> T get(SessionProvider sessionProvider,
                  String key,
                  Class<T> clz)
        {
            return getImpl(key, clz, (chunk) -> chunk.get(sessionProvider, clz));
        }

        void putIncremental(SessionProvider sessionProvider,
                            Class<C2> clzChunkRecord,
                            RecordLocator<R2> loc_activity,
                            String key,
                            Object value)
        {
            Integer sequence = mapSequence.getOrDefault(key, 0);

            putImpl(key + sequence, (chunk) -> chunk.put(sessionProvider, clzChunkRecord, loc_activity, value));

            mapSequence.put(key, sequence + 1);
        }

        <T> T getIncremental(SessionProvider sessionProvider,
                             String key,
                             int sequence,
                             Class<T> clz)
        {
            Integer sequenceMax = mapSequence.getOrDefault(key, 0);

            return sequence < sequenceMax ? getImpl(key + sequence, clz, (chunk) -> chunk.get(sessionProvider, clz)) : null;
        }

        List<String> filter(Class<?> clz)
        {
            List<String> res = Lists.newArrayList();

            for (Map.Entry<String, ChunkedProperty<R2, C2, W2>> pair : map.entrySet())
            {
                ChunkedProperty<R2, C2, W2> chunk = pair.getValue();
                if (Reflection.isSubclassOf(clz, chunk.chunkClass))
                {
                    res.add(pair.getKey());
                }
            }

            return res;
        }

        OutputStream writeAsStream(SessionProvider sessionProvider,
                                   Class<C2> clzChunkRecord,
                                   RecordLocator<R2> loc_activity,
                                   String keyPrefix,
                                   int chunkSize)
        {
            return new OutputStream()
            {
                private final byte[] m_buffer = new byte[chunkSize];
                private int m_offset;
                private int m_sequenceNumber;

                @Override
                public void write(byte[] b,
                                  int off,
                                  int len) throws
                                           IOException
                {
                    while (len > 0)
                    {
                        int chunk = Math.min(len, m_buffer.length - m_offset);

                        System.arraycopy(b, off, m_buffer, m_offset, chunk);

                        off += chunk;
                        len -= chunk;
                        m_offset += chunk;

                        flushIfNeeded();
                    }
                }

                @Override
                public void flush() throws
                                    IOException
                {
                    // Noop, we only flush at the end.
                }

                @Override
                public void close() throws
                                    IOException
                {
                    reallyFlush();
                }

                @Override
                public void write(int b) throws
                                         IOException
                {
                    m_buffer[m_offset++] = (byte) b;

                    flushIfNeeded();
                }

                private void flushIfNeeded() throws
                                             IOException
                {
                    if (m_offset == m_buffer.length)
                    {
                        reallyFlush();
                    }
                }

                private void reallyFlush() throws
                                           IOException
                {
                    if (m_offset > 0)
                    {
                        putImpl(keyPrefix + m_sequenceNumber++, (chunk) -> chunk.putRaw(sessionProvider, clzChunkRecord, loc_activity, m_buffer, 0, m_offset));

                        m_offset = 0;
                    }
                }
            };
        }

        InputStream readAsStream(SessionProvider sessionProvider,
                                 String keyPrefix)
        {
            return new InputStream()
            {
                private byte[] m_buffer;
                private int m_offset;
                private int m_sequenceNumber;

                @Override
                public int read(byte[] b,
                                int off,
                                int len)
                {
                    int read = 0;

                    while (len > 0 && isAvailable())
                    {
                        int chunk = Math.min(len, m_buffer.length - m_offset);
                        System.arraycopy(m_buffer, m_offset, b, off, chunk);

                        off += chunk;
                        len -= chunk;
                        read += chunk;
                        m_offset += chunk;
                    }

                    return read > 0 ? read : -1;
                }

                @Override
                public long skip(long n)
                {
                    long skipped = 0;

                    while (n > 0 && isAvailable())
                    {
                        long chunk = Math.min(n, m_buffer.length - m_offset);
                        m_offset += chunk;
                        n -= chunk;
                        skipped += chunk;
                    }

                    return skipped;
                }

                @Override
                public void close() throws
                                    IOException
                {
                    m_sequenceNumber = -2; // An invalid value.
                    m_buffer         = null;
                }

                @Override
                public synchronized void reset()
                {
                    m_buffer         = null;
                    m_offset         = 0;
                    m_sequenceNumber = 0;
                }

                @Override
                public int read() throws
                                  IOException
                {
                    return isAvailable() ? m_buffer[m_offset++] & 0xFF : -1;
                }

                private boolean isAvailable()
                {
                    if (m_buffer == null || m_offset >= m_buffer.length)
                    {
                        m_buffer = null;
                        m_offset = 0;

                        try (SessionHolder subSessionHolder = sessionProvider.newSessionWithoutTransaction())
                        {
                            m_buffer = getImpl(keyPrefix + m_sequenceNumber, byte[].class, (chunk) -> chunk.getRaw(sessionProvider));
                            if (m_buffer != null)
                            {
                                m_sequenceNumber++;
                            }
                        }
                    }

                    return m_buffer != null;
                }
            };
        }

        //--//

        private void putImpl(String key,
                             Consumer<ChunkedProperty<R2, C2, W2>> callback)
        {
            ChunkedProperty<R2, C2, W2> chunk = map.get(key);
            if (chunk == null)
            {
                chunk = new ChunkedProperty<>();
                map.put(key, chunk);
            }

            callback.accept(chunk);
        }

        private <T> T getImpl(String key,
                              Class<T> clz,
                              Function<ChunkedProperty<R2, C2, W2>, T> callback)
        {
            ChunkedProperty<R2, C2, W2> chunk = map.get(key);

            return chunk != null ? callback.apply(chunk) : null;
        }
    }

    //--//

    private static final Map<Class<?>, Map<String, Descriptor>> s_methodsPerClass    = Maps.newHashMap();
    private static final Logger                                 s_baseLoggerInstance = new Logger(BackgroundActivityHandler.class);

    private SessionProvider                                 m_sessionProvider;
    private RecordLocator<R>                                m_activity;
    private List<BiConsumerWithException<SessionHolder, R>> m_postActions;
    private boolean                                         m_stopProcessingPostActions;

    @JsonIgnore
    protected ILogger loggerInstance;

    @JsonIgnore
    public String displayName;

    //--//

    public String        stateMachine;
    public ZonedDateTime timeoutExpiration;
    public ZonedDateTime firstFailedAttempt;
    public Integer       maxAttempts;
    public int           nextAttempt;
    public ZonedDateTime nextHeaderReport;
    public String        lastStateMachine;
    public int           noPostActionsCounter;

    public PropertyBagValue            state        = new PropertyBagValue();
    public ChunkedPropertyBag<R, C, W> stateChunked = new ChunkedPropertyBag<>();

    protected BackgroundActivityHandler()
    {
        loggerInstance = s_baseLoggerInstance.createSubLogger(getClass());
    }

    protected void initializeTimeout(int amount,
                                     TimeUnit unit)
    {
        timeoutExpiration = TimeUtils.future(amount, unit);
    }

    protected void initializeTimeout(Duration timeout)
    {
        timeoutExpiration = TimeUtils.now()
                                     .plus(timeout);
    }

    //--//

    private Map<String, Descriptor> resolveMethods()
    {
        Class<?> clz = getClass();

        Map<String, Descriptor> map;

        synchronized (s_methodsPerClass)
        {
            map = s_methodsPerClass.get(clz);
        }

        if (map == null)
        {
            List<Descriptor> candidates = Lists.newArrayList();

            for (Method m : Reflection.collectMethods(clz)
                                      .values())
            {
                var anno = m.getAnnotation(BackgroundActivityMethod.class);
                if (anno != null)
                {
                    if (Reflection.isStaticMethod(m))
                    {
                        throw Exceptions.newRuntimeException("Method '%s' cannot be static", m);
                    }

                    Class<?> stateClass = anno.stateClass();
                    if (stateClass != void.class && !stateClass.isEnum())
                    {
                        throw Exceptions.newRuntimeException("State class on method '%s' is not an enum: %s", m, stateClass.getName());
                    }

                    Type[] args = m.getGenericParameterTypes();

                    if (anno.needsSession())
                    {
                        if (args.length != 1 || !Reflection.isSubclassOf(SessionHolder.class, args[0]))
                        {
                            throw Exceptions.newRuntimeException("Method '%s' needs a session but doesn't have the correct signature", m);
                        }

                        if (m.getReturnType() != void.class)
                        {
                            throw Exceptions.newRuntimeException("Method '%s' must return void", m);
                        }
                    }
                    else
                    {
                        if (args.length != 0)
                        {
                            throw Exceptions.newRuntimeException("Method '%s' cannot receive any parameters", m);
                        }

                        if (!Reflection.isMethodReturningAPromise(Void.class, m))
                        {
                            throw Exceptions.newRuntimeException("Method '%s' must return CompletableFuture<Void>", m);
                        }
                    }

                    Descriptor desc = new Descriptor();
                    desc.method = m;
                    desc.anno   = anno;

                    candidates.add(desc);
                }
            }

            if (candidates.isEmpty())
            {
                throw Exceptions.newRuntimeException("No methods annotated with @BackgroundActivityMethod in %s", clz.getName());
            }

            Descriptor               initial    = null;
            Class<? extends Enum<?>> stateClass = null;

            for (Descriptor desc : candidates)
            {
                @SuppressWarnings("unchecked") Class<? extends Enum<?>> descStateClass = (Class<? extends Enum<?>>) desc.anno.stateClass();

                if (stateClass == null)
                {
                    stateClass = descStateClass;
                }
                else if (stateClass != descStateClass)
                {
                    throw Exceptions.newRuntimeException("Methods in '%s' refer to different state classes:", clz.getName(), stateClass.getName(), descStateClass.getName());
                }

                if (desc.anno.initial())
                {
                    if (initial == null)
                    {
                        initial = desc;
                    }
                    else
                    {
                        throw Exceptions.newRuntimeException("Methods '%s' and '%s' both marked as initial", initial.method, desc.method);
                    }
                }
            }

            map = Maps.newHashMap();

            if (candidates.size() > 1)
            {
                Set<String> stateValues = Sets.newHashSet();

                for (Enum<?> enumConstant : stateClass.getEnumConstants())
                {
                    stateValues.add(enumConstant.name());
                }

                for (Descriptor desc : candidates)
                {
                    String stateValue = StringUtils.removeStart(desc.method.getName(), "state_");

                    if (!stateValues.contains(stateValue))
                    {
                        throw Exceptions.newRuntimeException("Method '%s' refers to state '%s', not in '%s'", desc.method, stateValue, stateClass.getName());
                    }

                    map.put(stateValue, desc);

                    if (desc.anno.initial())
                    {
                        map.put(null, desc);
                    }
                }
            }
            else
            {
                map.put(null, candidates.get(0));
            }

            synchronized (s_methodsPerClass)
            {
                s_methodsPerClass.put(clz, map);
            }
        }

        return map;
    }

    public static <R extends RecordForBackgroundActivity<R, C, W>, C extends RecordForBackgroundActivityChunk<R, C, W>, T extends BackgroundActivityHandler<R, C, W>, W extends RecordForWorker<W>> T allocate(Class<T> clz)
    {
        return Reflection.newInstance(clz);
    }

    @JsonIgnore
    public SessionProvider getSessionProvider()
    {
        return m_sessionProvider;
    }

    @JsonIgnore
    public void setSessionProvider(SessionProvider sessionProvider)
    {
        m_sessionProvider = sessionProvider;
    }

    @JsonIgnore
    public RecordLocator<R> getActivity()
    {
        return m_activity;
    }

    @JsonIgnore
    public void setActivity(RecordLocator<R> activity)
    {
        m_activity = activity;
    }

    //--//

    protected void flushStateToDatabase() throws
                                          Exception
    {
        callUnderTransaction(this::flushStateToDatabase);
    }

    protected void flushStateToDatabase(SessionHolder sessionHolder)
    {
        R rec = sessionHolder.fromLocator(m_activity);
        rec.setHandler(this);

        sessionHolder.commitAndBeginNewTransaction();
    }

    protected boolean shouldStopProcessing()
    {
        try
        {
            return computeInReadOnlySession(sessionHolder ->
                                            {
                                                R rec = sessionHolder.fromLocatorOrNull(m_activity);
                                                if (rec == null)
                                                {
                                                    return true;
                                                }

                                                BackgroundActivityStatus status = rec.getStatus();
                                                return status.isCancelling() || status.isDone();
                                            });
        }
        catch (Exception e)
        {
            return true;
        }
    }

    //--//

    protected <T> T computeUnderTransaction(FunctionWithException<SessionHolder, T> callback) throws
                                                                                              Exception
    {
        T res;

        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            res = callback.apply(sessionHolder);

            sessionHolder.commit();
        }

        return res;
    }

    protected void callUnderTransaction(ConsumerWithException<SessionHolder> callback) throws
                                                                                       Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            callback.accept(sessionHolder);

            sessionHolder.commit();
        }
    }

    protected <T> T computeInReadOnlySession(FunctionWithException<SessionHolder, T> callback) throws
                                                                                               Exception
    {
        T res;

        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            res = callback.apply(sessionHolder);
        }

        return res;
    }

    protected void callInReadOnlySession(ConsumerWithException<SessionHolder> callback) throws
                                                                                        Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            callback.accept(sessionHolder);
        }
    }

    //--//

    protected <R> void withLocatorReadonly(RecordLocator<R> loc,
                                           BiConsumerWithException<SessionHolder, R> callback) throws
                                                                                               Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            R rec = sessionHolder.fromLocator(loc);

            callback.accept(sessionHolder, rec);
        }
    }

    protected <R> void withLocatorReadonlyOrNull(RecordLocator<R> loc,
                                                 BiConsumerWithException<SessionHolder, R> callback) throws
                                                                                                     Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            R rec = sessionHolder.fromLocatorOrNull(loc);

            callback.accept(sessionHolder, rec);
        }
    }

    protected <T, R> T withLocatorReadonly(RecordLocator<R> loc,
                                           BiFunctionWithException<SessionHolder, R, T> callback) throws
                                                                                                  Exception
    {
        T res;

        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            R rec = sessionHolder.fromLocator(loc);

            res = callback.apply(sessionHolder, rec);
        }

        return res;
    }

    protected <T, R> T withLocatorReadonlyOrNull(RecordLocator<R> loc,
                                                 BiFunctionWithException<SessionHolder, R, T> callback) throws
                                                                                                        Exception
    {
        T res;

        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            R rec = sessionHolder.fromLocatorOrNull(loc);

            res = callback.apply(sessionHolder, rec);
        }

        return res;
    }

    //--//

    protected <R> void withLocator(RecordLocator<R> loc,
                                   BiConsumerWithException<SessionHolder, R> callback) throws
                                                                                       Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            R rec = sessionHolder.fromLocator(loc);

            callback.accept(sessionHolder, rec);

            sessionHolder.commit();
        }
    }

    protected <R> void withLocatorOrNull(RecordLocator<R> loc,
                                         BiConsumerWithException<SessionHolder, R> callback) throws
                                                                                             Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            R rec = sessionHolder.fromLocatorOrNull(loc);

            callback.accept(sessionHolder, rec);

            sessionHolder.commit();
        }
    }

    protected <T, R> T withLocator(RecordLocator<R> loc,
                                   BiFunctionWithException<SessionHolder, R, T> callback) throws
                                                                                          Exception
    {
        T res;

        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            R rec = sessionHolder.fromLocator(loc);

            res = callback.apply(sessionHolder, rec);

            sessionHolder.commit();
        }

        return res;
    }

    protected <T, R> T withLocatorOrNull(RecordLocator<R> loc,
                                         BiFunctionWithException<SessionHolder, R, T> callback) throws
                                                                                                Exception
    {
        T res;

        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            R rec = sessionHolder.fromLocatorOrNull(loc);

            res = callback.apply(sessionHolder, rec);

            sessionHolder.commit();
        }

        return res;
    }

    //--//

    protected <R> void lockedWithLocator(RecordLocator<R> loc,
                                         long timeout,
                                         TimeUnit unit,
                                         BiConsumerWithException<SessionHolder, RecordLocked<R>> callback) throws
                                                                                                           Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<R> lock = sessionHolder.fromLocatorWithLock(loc, timeout, unit);

            callback.accept(sessionHolder, lock);

            sessionHolder.commit();
        }
    }

    protected <R> void lockedWithLocatorOrNull(RecordLocator<R> loc,
                                               long timeout,
                                               TimeUnit unit,
                                               BiConsumerWithException<SessionHolder, RecordLocked<R>> callback) throws
                                                                                                                 Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<R> lock = sessionHolder.fromLocatorWithLockOrNull(loc, timeout, unit);

            callback.accept(sessionHolder, lock);

            sessionHolder.commit();
        }
    }

    protected <T, R> T lockedWithLocator(RecordLocator<R> loc,
                                         long timeout,
                                         TimeUnit unit,
                                         BiFunctionWithException<SessionHolder, RecordLocked<R>, T> callback) throws
                                                                                                              Exception
    {
        T res;

        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<R> lock = sessionHolder.fromLocatorWithLock(loc, timeout, unit);

            res = callback.apply(sessionHolder, lock);

            sessionHolder.commit();
        }

        return res;
    }

    protected <T, R> T lockedWithLocatorOrNull(RecordLocator<R> loc,
                                               long timeout,
                                               TimeUnit unit,
                                               BiFunctionWithException<SessionHolder, RecordLocked<R>, T> callback) throws
                                                                                                                    Exception
    {
        T res;

        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<R> lock = sessionHolder.fromLocatorWithLockOrNull(loc, timeout, unit);

            res = callback.apply(sessionHolder, lock);

            sessionHolder.commit();
        }

        return res;
    }

    //--//

    public void putChunk(String key,
                         Object value)
    {
        stateChunked.put(m_sessionProvider, getChunkClass(), m_activity, key, value);
    }

    public <T> T getChunk(String key,
                          Class<T> clz)
    {
        return stateChunked.get(m_sessionProvider, key, clz);
    }

    public void addChunkToSequence(String key,
                                   Object value)
    {
        stateChunked.putIncremental(m_sessionProvider, getChunkClass(), m_activity, key, value);
    }

    public <T> T getChunkFromSequence(String key,
                                      int sequence,
                                      Class<T> clz)
    {
        return stateChunked.getIncremental(m_sessionProvider, key, sequence, clz);
    }

    public <T> void forEachChunkInSequence(String key,
                                           Class<T> clz,
                                           BiConsumer<Integer, T> callback)
    {
        for (int sequence = 0; true; sequence++)
        {
            T chunk = getChunkFromSequence(key, sequence, clz);
            if (chunk == null)
            {
                break;
            }

            callback.accept(sequence, chunk);
        }
    }

    public <T> T ensureChunk(String key,
                             Class<T> clz,
                             Callable<T> generator) throws
                                                    Exception
    {
        T res = getChunk(key, clz);
        if (res == null)
        {
            res = generator.call();
            putChunk(key, res);
        }

        return res;
    }

    public <T> T ensureChunkNoThrow(String key,
                                    Class<T> clz,
                                    Callable<T> generator)
    {
        try
        {
            return ensureChunk(key, clz, generator);
        }
        catch (Exception e)
        {
            return Reflection.newInstance(clz);
        }
    }

    public List<String> filterChunks(Class<?> clz)
    {
        return stateChunked.filter(clz);
    }

    public InputStream readAsStream(String keyPrefix)
    {
        return stateChunked.readAsStream(m_sessionProvider, keyPrefix);
    }

    public OutputStream writeAsStream(String keyPrefix,
                                      int chunkSize)
    {
        final int maxSize = 1024 * 1024;
        if (chunkSize <= 0 || chunkSize > maxSize)
        {
            chunkSize = maxSize;
        }

        return stateChunked.writeAsStream(m_sessionProvider, getChunkClass(), m_activity, keyPrefix, chunkSize);
    }

    protected abstract Class<C> getChunkClass();

    //--//

    /**
     * After handler creation and/or restore, setup context, like extra info for logger.
     */
    public abstract void configureContext();

    /**
     * @return A text for the UI
     */
    @JsonIgnore
    public abstract String getTitle();

    /**
     * @return A context that can be used to identify an instance of the activity.
     */
    @JsonIgnore
    public abstract RecordLocator<? extends RecordWithCommonFields> getContext();

    //--//

    public void validateStates()
    {
        resolveMethods();
    }

    class AsyncWorker
    {
        @AsyncBackground
        final CompletableFuture<Void> process() throws
                                                Exception
        {
            Descriptor desc = null;

            try
            {
                if (timeoutExpiration != null && TimeUtils.isTimeoutExpired(timeoutExpiration))
                {
                    throw new TimeoutException(getTitle());
                }
                else
                {
                    var map = resolveMethods();
                    desc = map.get(stateMachine);
                    if (desc == null)
                    {
                        throw Exceptions.newIllegalArgumentException("Unexpected state '%s'", stateMachine);
                    }
                    else
                    {
                        if (maxAttempts == null)
                        {
                            maxAttempts = desc.anno.maxRetries();
                        }

                        if (!StringUtils.equals(stateMachine, lastStateMachine))
                        {
                            nextHeaderReport = null;
                            lastStateMachine = stateMachine;

                            maxAttempts = desc.anno.maxRetries();
                        }

                        if (TimeUtils.isTimeoutExpired(nextHeaderReport))
                        {
                            loggerInstance.info("%s %s", getTitle(), getStateMachineDisplayName());

                            nextHeaderReport = TimeUtils.future(10, TimeUnit.MINUTES);
                        }

                        if (desc.anno.needsSession())
                        {
                            try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
                            {
                                desc.method.invoke(BackgroundActivityHandler.this, sessionHolder);

                                sessionHolder.commit();
                            }
                        }
                        else
                        {
                            @SuppressWarnings("unchecked") CompletableFuture<Void> future = (CompletableFuture<Void>) desc.method.invoke(BackgroundActivityHandler.this);

                            await(future);
                        }

                        // Reset on success.
                        maxAttempts = desc.anno.maxRetries();
                    }
                }
            }
            catch (Throwable t)
            {
                t = Exceptions.unwrapException(t);

                if (desc != null && desc.anno.autoRetry())
                {
                    if (firstFailedAttempt == null)
                    {
                        firstFailedAttempt = TimeUtils.now();

                        Throwable t2 = Exceptions.unwrapException(t);

                        if (t2 instanceof TimeoutException)
                        {
                            loggerInstance.warn("%s stalled at %s...", getTitle(), getStateMachineDisplayName());
                        }
                        else
                        {
                            loggerInstance.warn("%s stalled at %s, due to %s", getTitle(), getStateMachineDisplayName(), t);
                        }
                    }

                    if (--maxAttempts <= 0)
                    {
                        await(markAsFailed(t));
                    }

                    nextAttempt = Math.min(60, nextAttempt + 5);

                    await(rescheduleDelayed(nextAttempt, TimeUnit.SECONDS));
                }
                else
                {
                    await(markAsFailed(t));
                }
            }

            if (m_postActions == null)
            {
                noPostActionsCounter++;
                loggerInstance.debug("%s stalled at %s with no post actions for %d rounds...", getTitle(), stateMachine, noPostActionsCounter);

                if (noPostActionsCounter >= 1000)
                {
                    if ((noPostActionsCounter % 100) == 0)
                    {
                        loggerInstance.warn("%s stalled at %s with no post actions for %d rounds...", getTitle(), stateMachine, noPostActionsCounter);
                    }

                    await(rescheduleDelayed(1, TimeUnit.SECONDS));
                }
                else
                {
                    await(rescheduleDelayed(10, TimeUnit.MILLISECONDS));
                }
            }
            else
            {
                noPostActionsCounter = 0;
            }

            drainPostActions();

            return wrapAsync(null);
        }
    }

    String getStateMachineDisplayName()
    {
        return stateMachine != null ? String.format("(%s)", stateMachine) : "<start>";
    }

    final CompletableFuture<Void> process() throws
                                            Exception
    {
        AsyncWorker worker = new AsyncWorker();
        return worker.process();
    }

    void handleCallbacksOnComplete(SessionHolder sessionHolder) throws
                                                                Exception
    {
        ICleanupOnComplete itfCleanupOnComplete = Reflection.as(this, ICleanupOnComplete.class);
        if (itfCleanupOnComplete != null)
        {
            itfCleanupOnComplete.cleanupOnComplete();
        }

        ICleanupOnCompleteWithSession itfCleanupOnFailureWithSession = Reflection.as(this, ICleanupOnCompleteWithSession.class);
        if (itfCleanupOnFailureWithSession != null)
        {
            itfCleanupOnFailureWithSession.cleanupOnComplete(sessionHolder);
        }
    }

    void handleCallbacksOnFailure(SessionHolder sessionHolder,
                                  Throwable t) throws
                                               Exception
    {
        ICleanupOnFailure itfCleanupOnFailure = Reflection.as(this, ICleanupOnFailure.class);
        if (itfCleanupOnFailure != null)
        {
            itfCleanupOnFailure.cleanupOnFailure(t);
        }

        ICleanupOnFailureWithSession itfCleanupOnFailureWithSession = Reflection.as(this, ICleanupOnFailureWithSession.class);
        if (itfCleanupOnFailureWithSession != null)
        {
            itfCleanupOnFailureWithSession.cleanupOnFailure(sessionHolder, t);
        }
    }

    void handlePostprocessCallbacks(SessionHolder sessionHolder,
                                    Throwable t) throws
                                                 Exception
    {
        IPostProcess itfPostProcess = Reflection.as(this, IPostProcess.class);
        if (itfPostProcess != null)
        {
            itfPostProcess.postProcess(t);
        }

        IPostProcessWithSession itfPostProcessWithSession = Reflection.as(this, IPostProcessWithSession.class);
        if (itfPostProcessWithSession != null)
        {
            itfPostProcessWithSession.postProcess(sessionHolder, t);
        }
    }

    //--//

    protected CompletableFuture<Void> continueAtState(Enum<?> state) throws
                                                                     Exception
    {
        return continueAtState(state, 0, null);
    }

    protected CompletableFuture<Void> continueAtState(Enum<?> state,
                                                      int delay,
                                                      TimeUnit unit) throws
                                                                     Exception
    {
        var        map  = resolveMethods();
        Descriptor desc = map.get(state.name());
        if (desc == null)
        {
            return markAsFailed(Exceptions.newIllegalArgumentException("Unexpected state '%s'", state));
        }

        stateMachine = state.name();

        return rescheduleDelayed(delay, unit);
    }

    protected CompletableFuture<Void> rescheduleDelayed(int delay,
                                                        TimeUnit unit)
    {
        ZonedDateTime when = unit != null ? TimeUtils.future(delay, unit) : TimeUtils.now();

        return queuePostAction((sessionHolder, rec_activity) ->
                               {
                                   loggerInstance.debug("Rescheduling activity %s for %s", rec_activity.getDisplayName(), when.toLocalDateTime());

                                   rec_activity.transitionToActive(when);
                               });
    }

    protected CompletableFuture<Void> rescheduleSleeping(int delay,
                                                         TimeUnit unit)
    {
        ZonedDateTime when = TimeUtils.future(delay, unit);

        return queuePostAction((sessionHolder, rec_activity) ->
                               {
                                   loggerInstance.debug("Sleeping activity '%s' for %s", rec_activity.getDisplayName(), when.toLocalDateTime());

                                   rec_activity.transitionToSleeping(when);
                               });
    }

    protected <T extends RecordForBackgroundActivity<T, ?, ?>> CompletableFuture<Void> waitForSubActivity(RecordLocator<T> subActivity,
                                                                                                          ZonedDateTime forcedWakeup)
    {
        return queuePostAction((sessionHolder, rec_activity) ->
                               {
                                   T rec_subActivity = sessionHolder.fromLocator(subActivity);
                                   loggerInstance.debug("Activity '%s' waiting on sub activity '%s'", rec_subActivity.getDisplayName(), rec_subActivity.getDisplayName());

                                   @SuppressWarnings("unchecked") RecordForBackgroundActivity<T, ?, ?> rec2 = (RecordForBackgroundActivity<T, ?, ?>) rec_activity;
                                   rec2.transitionToWaiting(rec_subActivity, forcedWakeup);
                               });
    }

    protected <T extends RecordForBackgroundActivity<T, ?, ?>> void waitForSubActivities(Collection<RecordLocator<T>> subActivities,
                                                                                         ZonedDateTime forcedWakeup)
    {
        queuePostAction((sessionHolder, rec_activity) ->
                        {
                            List<T> rec_subActivities = CollectionUtils.transformToList(subActivities, sessionHolder::fromLocator);

                            loggerInstance.debug("Activity '%s' waiting on sub activities '%s'",
                                                 rec_activity.getDisplayName(),
                                                 CollectionUtils.transformToList(rec_subActivities, RecordForBackgroundActivity::getDisplayName));

                            @SuppressWarnings("unchecked") RecordForBackgroundActivity<T, ?, ?> rec2 = (RecordForBackgroundActivity<T, ?, ?>) rec_activity;
                            rec2.transitionToWaiting(rec_subActivities, forcedWakeup);
                        });
    }

    protected CompletableFuture<Void> markAsFailed(String failureMessage,
                                                   Object... args)
    {
        return markAsFailed(String.format(failureMessage, args));
    }

    protected CompletableFuture<Void> markAsFailed(String failureMessage)
    {
        return markAsFailed(new RuntimeException(failureMessage));
    }

    protected CompletableFuture<Void> markAsFailed(Throwable t)
    {
        Throwable finalT = Exceptions.unwrapException(t);

        return queuePostAction((sessionHolder, rec_activity) ->
                               {
                                   m_stopProcessingPostActions = true;

                                   try
                                   {
                                       handleCallbacksOnFailure(sessionHolder, finalT);
                                       handlePostprocessCallbacks(sessionHolder, finalT);

                                       if (finalT instanceof CancellationException)
                                       {
                                           // Cancellation is not an error.
                                       }
                                       else if (finalT instanceof TimeoutException)
                                       {
                                           loggerInstance.warn("Activity '%s' failed with timeout", rec_activity.getDisplayName());
                                       }
                                       else
                                       {
                                           loggerInstance.error("Activity '%s' failed with exception %s", rec_activity.getDisplayName(), finalT);
                                       }
                                   }
                                   catch (Throwable innerT)
                                   {
                                       loggerInstance.error("Activity '%s' crashed during cleanup, with exception %s", rec_activity.getDisplayName(), innerT);
                                   }

                                   rec_activity.setResult(sessionHolder, finalT);
                               });
    }

    protected CompletableFuture<Void> markAsCompleted()
    {
        return queuePostAction((sessionHolder, rec_activity) ->
                               {
                                   loggerInstance.debug("Activity '%s' completed successfully", rec_activity.getDisplayName());

                                   if (nextHeaderReport != null)
                                   {
                                       loggerInstance.info("%s <completed>", getTitle());
                                   }

                                   m_stopProcessingPostActions = true;

                                   try
                                   {
                                       handleCallbacksOnComplete(sessionHolder);
                                       handlePostprocessCallbacks(sessionHolder, null);
                                   }
                                   catch (Throwable innerT)
                                   {
                                       loggerInstance.error("Activity '%s' crashed during cleanup, with exception %s", rec_activity.getDisplayName(), innerT);
                                   }

                                   rec_activity.setResult(sessionHolder, null);
                               });
    }

    //--//

    CompletableFuture<Void> queuePostAction(BiConsumerWithException<SessionHolder, R> callback)
    {
        if (m_postActions == null)
        {
            m_postActions = Lists.newArrayList();
        }

        m_postActions.add(callback);
        return AsyncRuntime.NullResult;
    }

    void drainPostActions() throws
                            Exception
    {
        int retries = 5;

        while (true)
        {
            try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
            {
                RecordLocked<R> lock_activity = sessionHolder.fromLocatorWithLockOrNull(m_activity, 10, TimeUnit.SECONDS);
                if (lock_activity == null)
                {
                    if (retries-- <= 0)
                    {
                        loggerInstance.debug("Failed to flush state for '%s', record deleted...", getTitle());
                        break;
                    }

                    continue;
                }

                drainPostActions(sessionHolder, lock_activity.get());

                sessionHolder.commit();

                return;
            }
            catch (Throwable t)
            {
                if (retries-- <= 0)
                {
                    throw t;
                }

                loggerInstance.debug("Failed to flush state for '%s', due to %s, retrying...", getTitle(), t.getMessage());
            }
        }
    }

    void drainPostActions(SessionHolder sessionHolder,
                          R rec_activity) throws
                                          Exception
    {
        while (m_postActions != null)
        {
            List<BiConsumerWithException<SessionHolder, R>> postActions = m_postActions;
            m_postActions = null;

            for (BiConsumerWithException<SessionHolder, R> postAction : postActions)
            {
                if (m_stopProcessingPostActions)
                {
                    break;
                }

                postAction.accept(sessionHolder, rec_activity);
            }
        }

        rec_activity.setHandler(this);
    }

    //--//

    public <S> S getService(Class<S> serviceClass)
    {
        return m_sessionProvider.getService(serviceClass);
    }

    @Nonnull
    public <S> S getServiceNonNull(Class<S> serviceClass)
    {
        return m_sessionProvider.getServiceNonNull(serviceClass);
    }

    //--//

    protected void putStateValue(String key,
                                 GenericValue value)
    {
        state.put(key, value);
    }

    protected <T extends GenericValue> List<T> getStateValues(Class<T> clz)
    {
        return state.list(clz);
    }

    protected <T extends GenericValue> T getStateValue(Class<T> clz,
                                                       String key)
    {
        return state.get(clz, key);
    }

    protected SubstitutionContext resolveVariables(String value)
    {
        return state.substituteVariables(value);
    }

    protected String resolveVariablesToText(String value)
    {
        return resolveVariables(value).result;
    }
}
