/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.dataconnector;

import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.google.common.collect.Maps;
import com.optio3.cloud.annotation.Optio3RecurringProcessor;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.logic.RecurringActivityHandler;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithMetadata;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.concurrency.Executors;
import com.optio3.util.TimeUtils;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "DATA_CONNECTION")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "DataConnection", model = BaseModel.class, metamodel = DataConnectionRecord_.class)
public class DataConnectionRecord extends RecordWithMetadata
{
    private static final Map<String, ConnectionState> s_pending = Maps.newHashMap();

    //--//

    @Optio3RecurringProcessor
    public static class PruneUnusedConnections extends RecurringActivityHandler
    {
        private static final Duration s_maxStaleness = Duration.of(30, ChronoUnit.DAYS);

        @Override
        public Duration startupDelay()
        {
            return null;
        }

        @Override
        public CompletableFuture<ZonedDateTime> process(SessionProvider sessionProvider) throws
                                                                                         Exception
        {
            ZonedDateTime now = TimeUtils.now();

            try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
            {
                RecordHelper<DataConnectionRecord> helper = sessionHolder.createHelper(DataConnectionRecord.class);

                ZonedDateTime threshold = now.minus(s_maxStaleness);
                for (DataConnectionRecord rec : helper.listAll())
                {
                    if (TimeUtils.isBeforeOrNull(rec.getUpdatedOn(), threshold))
                    {
                        helper.delete(rec);
                    }
                }

                sessionHolder.commit();
            }

            return wrapAsync(now.plus(1, ChronoUnit.DAYS));
        }

        @Override
        public void shutdown()
        {
            // Nothing to do.
        }
    }

    //--//

    private static class ConnectionState
    {
        private final String      m_connectionId;
        private       MetadataMap m_metadata;
        private       boolean     m_flushed;
        private       boolean     m_modified;

        ConnectionState(SessionProvider sessionProvider,
                        String connectionId)
        {
            m_connectionId = connectionId;

            ensureRecord(sessionProvider, (rec) ->
            {
                m_metadata = rec.getMetadata();
            });

            Executors.scheduleOnDefaultPool(() -> flush(sessionProvider), 5, TimeUnit.MINUTES);
        }

        public ZonedDateTime get(String elementId)
        {
            return m_metadata.getDateTime(elementId);
        }

        void put(String elementId,
                 ZonedDateTime lastSample)
        {
            m_metadata.putDateTime(elementId, lastSample);
            m_modified = true;
        }

        private void ensureRecord(SessionProvider sessionProvider,
                                  Consumer<DataConnectionRecord> callback)
        {
            try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
            {
                RecordHelper<DataConnectionRecord> helper_dc = sessionHolder.createHelper(DataConnectionRecord.class);
                DataConnectionRecord               rec       = helper_dc.getOrNull(m_connectionId);

                if (rec == null)
                {
                    rec = new DataConnectionRecord();
                    rec.setSysId(m_connectionId);
                    helper_dc.persist(rec);
                    helper_dc.flush();
                }

                callback.accept(rec);

                sessionHolder.commit();
            }
        }

        private void flush(SessionProvider sessionProvider)
        {
            synchronized (s_pending)
            {
                if (!m_flushed)
                {
                    if (m_modified)
                    {
                        try
                        {
                            ensureRecord(sessionProvider, (rec) ->
                            {
                                rec.setMetadata(m_metadata);
                            });
                        }
                        catch (Throwable t)
                        {
                            // Ignore failures.
                        }

                        m_modified = false;
                    }

                    m_flushed = true;
                    s_pending.remove(m_connectionId);
                }
            }
        }
    }

    //--//

    public static void setLastSample(SessionProvider sessionProvider,
                                     String connectionId,
                                     String elementId,
                                     ZonedDateTime lastSample)
    {
        try
        {
            synchronized (s_pending)
            {
                ConnectionState state = access(sessionProvider, connectionId);
                state.put(elementId, lastSample);
            }
        }
        catch (Throwable t)
        {
            // Ignore failures, we can always refetch the data.
        }
    }

    public static ZonedDateTime getLastSample(SessionProvider sessionProvider,
                                              String connectionId,
                                              String elementId)
    {
        try
        {
            synchronized (s_pending)
            {
                ConnectionState state = access(sessionProvider, connectionId);
                return state.get(elementId);
            }
        }
        catch (Throwable t)
        {
            // Ignore failures, we can always refetch the data.
            return null;
        }
    }

    //--//

    private static ConnectionState access(SessionProvider sessionProvider,
                                          String connectionId) throws
                                                               Exception
    {
        ConnectionState state = s_pending.get(connectionId);
        if (state == null)
        {
            state = new ConnectionState(sessionProvider, connectionId);

            s_pending.put(connectionId, state);
        }

        return state;
    }
}