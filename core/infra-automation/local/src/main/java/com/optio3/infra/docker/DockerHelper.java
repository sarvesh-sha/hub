/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.docker;

import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.archive.TarArchiveEntry;
import com.optio3.archive.TarBuilder;
import com.optio3.archive.TarWalker;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.collection.ExpandableArrayOfBytes;
import com.optio3.concurrency.Executors;
import com.optio3.infra.WellKnownDockerImageLabel;
import com.optio3.infra.directory.UserInfo;
import com.optio3.infra.docker.api.ContainerApi;
import com.optio3.infra.docker.api.ImageApi;
import com.optio3.infra.docker.api.NetworkApi;
import com.optio3.infra.docker.api.VolumeApi;
import com.optio3.infra.docker.model.BuildInfo;
import com.optio3.infra.docker.model.ContainerCreateCreatedBody;
import com.optio3.infra.docker.model.ContainerInspection;
import com.optio3.infra.docker.model.ContainerSummary;
import com.optio3.infra.docker.model.HistoryResponseItem;
import com.optio3.infra.docker.model.Image;
import com.optio3.infra.docker.model.ImageDeleteResponseItem;
import com.optio3.infra.docker.model.ImageSummary;
import com.optio3.infra.docker.model.MountPoint;
import com.optio3.infra.docker.model.Network;
import com.optio3.infra.docker.model.NetworkCreateConfig;
import com.optio3.infra.docker.model.NetworkCreateResponse;
import com.optio3.infra.docker.model.NetworksPruneReport;
import com.optio3.infra.docker.model.Volume;
import com.optio3.infra.docker.model.VolumesCreateBody;
import com.optio3.infra.docker.model.VolumesListOKBody;
import com.optio3.infra.registry.ManifestV1Decoded;
import com.optio3.infra.registry.api.RegistryApi;
import com.optio3.infra.registry.model.Catalog;
import com.optio3.infra.registry.model.History;
import com.optio3.infra.registry.model.ManifestV1;
import com.optio3.infra.registry.model.ManifestV2;
import com.optio3.infra.registry.model.Tags;
import com.optio3.interop.UnixSocketAdapter;
import com.optio3.lang.RunnableWithException;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.BoxingUtils;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.FileSystem;
import com.optio3.util.IdGenerator;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.ConsumerWithException;
import com.optio3.util.function.FunctionWithException;
import com.optio3.util.function.PredicateWithException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;

public class DockerHelper implements AutoCloseable
{
    public static final Logger LoggerInstance = new Logger(DockerHelper.class);

    public static final String DOCKER_API_VERSION = "/v1.27";

    public static final String DOCKER_REGISTRY_API_VERSION = "/v2";

    public static final String SELFID = "OPTIO3_DOCKER_SELFID";

    /**
     * Keeps track of the logical vs. physical location of mount points.
     * The logical location is the one seen by the container (guest).
     * The physical location is the one seen by the daemon (host).
     *
     * To spawn a container from inside another container, we need to resolve the indirection.
     */
    static class MountpointMapper
    {
        private static final String BIND_PREFIX = "OPTIO3_DOCKER_BIND_";

        private final Map<String, String> m_guestToHost = Maps.newHashMap();

        MountpointMapper()
        {
            Map<String, String> env = System.getenv();

            ContainerInspection self = s_self.get();
            if (self != null)
            {
                for (MountPoint mount : self.mounts)
                {
                    String name = encodeGuestBinding(mount.destination);
                    m_guestToHost.put(name, mount.source);
                }
            }

            for (String key : env.keySet())
            {
                if (key.startsWith(BIND_PREFIX))
                {
                    m_guestToHost.put(key, env.get(key));
                }
            }
        }

        //--//

        public String mapBindingToHost(String guestPath)
        {
            for (int pos = guestPath.length(); pos-- > 0; )
            {
                if (guestPath.charAt(pos) == '/')
                {
                    String guestDir = guestPath.substring(0, pos);

                    String hostDir = m_guestToHost.get(encodeGuestBinding(guestDir));
                    if (hostDir != null)
                    {
                        String file = guestPath.substring(pos + 1);

                        if (file.length() > 0)
                        {
                            hostDir = hostDir + "/" + file;
                        }

                        return hostDir;
                    }
                }
            }

            return guestPath;
        }

        public String encodeGuestBinding(String path)
        {
            //
            // The shell doesn't like slashes in the name of environment variables.
            // We have to escape a couple of characters:
            //
            //      _   =>   _u_
            //      .   =>   _d_
            //      /   =>   _s_
            //
            path = path.replace("_", "_u_");
            path = path.replace(".", "_d_");
            path = path.replace("/", "_s_");

            return BIND_PREFIX + path;
        }
    }

    private final static Supplier<MountpointMapper> s_mapper = Suppliers.memoize(MountpointMapper::new);

    //--//

    /**
     * Description of image on Docker Registry
     */
    public static class RegistryImage
    {
        public DockerImageIdentifier tag;

        public String imageSha;

        public DockerImageArchitecture architecture;

        public Map<String, String> labels = Maps.newHashMap();
    }

    /**
     * The authentication information has to be serialized as JSON and encoded as Base64.
     */
    public static class AuthConfig
    {
        public String username;
        public String password;
        public String auth;

        // Email is an optional value associated with the username.
        // This field is deprecated and will be removed in a later
        // version of docker.
        public String email;

        public String serveraddress;

        // IdentityToken is used to authenticate the user and get
        // an access token for the registry.
        public String identitytoken;

        // RegistryToken is a bearer token to be sent to a registry
        public String registrytoken;
    }

    /**
     * The registry information has to be serialized as JSON and encoded as Base64.
     */
    public static class RegistryConfig extends HashMap<String, RegistryConfig.User>
    {
        private static final long serialVersionUID = 1L;

        public static class User
        {
            public String username;
            public String password;
        }

        public void add(String registryAddress,
                        UserInfo registryUser)
        {
            User user = new User();
            user.username = registryUser.user;
            user.password = registryUser.getEffectivePassword();

            put(registryAddress, user);
        }
    }

    /**
     * The Docker Engine implements a REST API, but it's implemented on top of a UNIX socket, instead of a TCP one.
     * <p>
     * We use Apache CXF to convert calls to Java interfaces into REST calls. However it doesn't have a simple way to plug-in a new low-level transport.
     * <p>
     * This class bridges the gap.
     * <p>
     * It opens a temporary TCP server on a random port and also connects to Docker using a UNIX socket.<br>
     * Then it copies back and forth all the data, so that Apache CXF is happy.
     */
    static class HttpToUnixSocketConverter
    {
        private final ThreadPoolExecutor m_pool = Executors.getDefaultThreadPool();

        private ServerSocket m_socket;
        private Future<?>    m_worker;

        private ConcurrentMap<Socket, UnixSocketAdapter> m_openConnections = Maps.newConcurrentMap();

        HttpToUnixSocketConverter(int port) throws
                                            IOException
        {
            m_socket = new ServerSocket(port);

            m_worker = m_pool.submit(this::acceptConnections);
        }

        String getAddress()
        {
            return "http://127.0.0.1:" + m_socket.getLocalPort();
        }

        void close() throws
                     IOException,
                     InterruptedException
        {
            m_socket.close();
            try
            {
                m_worker.get();
            }
            catch (ExecutionException e)
            {
                // Ignore exceptions.
            }

            while (!m_openConnections.isEmpty())
            {
                for (Socket s : m_openConnections.keySet())
                {
                    shutdown(s);
                }
            }
        }

        private void shutdown(Socket s)
        {
            UnixSocketAdapter docker = m_openConnections.remove(s);
            if (docker != null)
            {
                docker.close();
            }

            try
            {
                s.close();
            }
            catch (IOException e)
            {
                // We might race with other threads, no problem.
            }
        }

        private void acceptConnections()
        {
            int seq = 0;

            while (!m_socket.isClosed())
            {
                try
                {
                    Socket s = m_socket.accept();

                    UnixSocketAdapter docker = new UnixSocketAdapter("/var/run/docker.sock");

                    m_openConnections.putIfAbsent(s, docker);

                    LoggerInstance.debug("New connection #%d on socket %s", seq, m_socket.getLocalPort());

                    final int seq2 = seq++;
                    m_pool.submit(() -> fromHttpToSocket(seq2, s, docker));
                    m_pool.submit(() -> fromSocketToHttp(seq2, s, docker));
                }
                catch (IOException e)
                {
                    return;
                }
            }
        }

        private void fromHttpToSocket(int seq,
                                      Socket s,
                                      UnixSocketAdapter docker)
        {
            try
            {
                InputStream inStream = s.getInputStream();
                int         offset   = 0;

                byte[] buf = ExpandableArrayOfBytes.getTempBuffer(64 * 1024);

                while (true)
                {
                    int len = inStream.read(buf);
                    if (len < 0)
                    {
                        LoggerInstance.debugVerbose("fromHttpToSocket: end of stream for connection #%d", seq);
                        return;
                    }

                    if (LoggerInstance.isEnabled(Severity.DebugVerbose))
                    {
                        StringBuilder sb = new StringBuilder();

                        for (int i = 0; i < len; i += 32)
                        {
                            LoggerInstance.debugVerbose("fromHttpToSocket: #%d %s", seq, dumpBuffer(sb, offset, buf, i, len - i));
                        }
                    }

                    docker.write(buf, len);
                    offset += len;
                }
            }
            catch (Exception e)
            {
                // Ignore failures, typically due to socket shutdown.
            }
            finally
            {
                shutdown(s);
            }
        }

        private void fromSocketToHttp(int seq,
                                      Socket s,
                                      UnixSocketAdapter docker)
        {
            try
            {
                OutputStream out    = s.getOutputStream();
                int          offset = 0;

                byte[] buf = ExpandableArrayOfBytes.getTempBuffer(64 * 1024);

                while (!s.isClosed())
                {
                    int len = docker.read(buf, 100);
                    if (len < 0)
                    {
                        LoggerInstance.debugVerbose("fromSocketToHttp: end of stream for connection #%d", seq);
                        return;
                    }

                    if (len == 0) // Socket has no ready data.
                    {
                        continue;
                    }

                    if (LoggerInstance.isEnabled(Severity.DebugVerbose))
                    {
                        StringBuilder sb = new StringBuilder();

                        for (int i = 0; i < len; i += 32)
                        {
                            LoggerInstance.debugVerbose("fromSocketToHttp: #%d %s", seq, dumpBuffer(sb, offset, buf, i, len - i));
                        }
                    }

                    out.write(buf, 0, len);
                    offset += len;
                }
            }
            catch (Exception e)
            {
                // Ignore failures, typically due to socket shutdown.
            }
            finally
            {
                shutdown(s);
            }
        }

        private String dumpBuffer(StringBuilder sb,
                                  int address,
                                  byte[] buf,
                                  int offset,
                                  int len)
        {
            sb.setLength(0);
            sb.append(String.format("%04x: ", address + offset));

            for (int i = 0; i < 32; i++)
            {
                if (i < len)
                {
                    sb.append(String.format("%02X ", buf[offset + i]));
                }
                else
                {
                    sb.append("   ");
                }
            }

            sb.append(' ');

            for (int i = 0; i < 32 && i < len; i++)
            {
                char c = (char) buf[offset + i];

                sb.append(c >= 32 && c < 128 ? c : '.');
            }

            return sb.toString();
        }
    }

    //--//

    private static final ObjectMapper        s_objectMapper;
    private static final JacksonJsonProvider s_jsonProvider;

    private final static Supplier<ContainerInspection> s_self = Suppliers.memoize(() ->
                                                                                  {
                                                                                      String selfId = System.getenv(SELFID);
                                                                                      if (selfId != null)
                                                                                      {
                                                                                          try
                                                                                          {
                                                                                              try (DockerHelper helper = new DockerHelper(null))
                                                                                              {
                                                                                                  ContainerApi proxy = helper.createProxy(ContainerApi.class);

                                                                                                  List<ContainerSummary> listSummary = proxy.containerList(null, null, null, null);
                                                                                                  for (ContainerSummary summary : listSummary)
                                                                                                  {
                                                                                                      ContainerInspection inspect = lookForSelfId(selfId, proxy, summary);
                                                                                                      if (inspect != null)
                                                                                                      {
                                                                                                          return inspect;
                                                                                                      }
                                                                                                  }
                                                                                              }
                                                                                          }
                                                                                          catch (Throwable ex)
                                                                                          {
                                                                                              // If we fail for any reason, then we can't be running inside Docker and have access to the daemon.
                                                                                          }
                                                                                      }

                                                                                      return null;
                                                                                  });

    static
    {
        ObjectMapper mapper = new ObjectMapper();

        //
        // Docker defines its models with a capital letter.
        // Swagger generates everything with a lowercase.
        // Make deserializer case-insensitive.
        //
        ObjectMappers.configureCaseInsensitive(mapper);

        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        //
        // As the Docker API evolves, new optional properties could be added to the models.
        // Just ignore what we don't understand.
        //
        ObjectMappers.configureToIgnoreMissingProperties(mapper);

        ObjectMappers.configureEnumAsStrings(mapper, true);

        mapper.registerModules(new JavaTimeModule());

        s_objectMapper = mapper;
        s_jsonProvider = new JacksonJsonProvider(mapper);
    }

    private       long                      m_timeout = 60000;
    private final UserInfo                  m_registryUser;
    private final HttpToUnixSocketConverter m_converter;
    private final String                    m_baseAddress;

    public DockerHelper(UserInfo registryUser) throws
                                               IOException
    {
        this(registryUser, 0);
    }

    public DockerHelper(UserInfo registryUser,
                        int port) throws
                                  IOException
    {
        m_registryUser = registryUser;
        m_converter    = new HttpToUnixSocketConverter(port);
        m_baseAddress  = m_converter.getAddress() + DOCKER_API_VERSION;
    }

    @Override
    public void close()
    {
        try
        {
            m_converter.close();
        }
        catch (Exception e)
        {
        }
    }

    public void setTimeout(long timeout)
    {
        m_timeout = timeout;
    }

    //--//

    public static String getSelfDockerId()
    {
        ContainerInspection self = s_self.get();
        return self != null ? self.id : null;
    }

    public static String mapBindingToHost(String path)
    {
        return s_mapper.get()
                       .mapBindingToHost(path);
    }

    public static String encodeGuestBinding(String guestPath)
    {
        return s_mapper.get()
                       .encodeGuestBinding(guestPath);
    }

    public static String encodeAsJsonBase64(Object target)
    {
        try
        {
            String json = ObjectMappers.SkipNulls.writeValueAsString(target);

            return Base64.getEncoder()
                         .encodeToString(json.getBytes());
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static <T> T decodeJson(byte[] buf,
                                   Class<T> clz) throws
                                                 IOException
    {
        return s_objectMapper.readValue(buf, clz);
    }

    //--//

    public static class LogEntry
    {
        public int           fd;
        public ZonedDateTime timestamp;
        public String        line;
    }

    public static class Utf8ByteArray extends ByteArrayOutputStream
    {
        String readLine(BufferedInputStream in) throws
                                                IOException
        {
            count = 0;

            while (true)
            {
                int c = in.read();
                if (c < 0 || c == '\n')
                {
                    break;
                }

                write(c);
            }

            return count == 0 ? null : new String(buf, 0, count, StandardCharsets.UTF_8);
        }
    }

    public List<LogEntry> getLogs(String containerId,
                                  boolean stdout,
                                  boolean stderr,
                                  ZonedDateTime since,
                                  int limit) throws
                                             IOException
    {
        List<LogEntry> res = Lists.newArrayList();

        Integer sinceInteger = (since != null) ? (int) since.toEpochSecond() : null;

        ContainerApi proxy = createProxy(ContainerApi.class);

        Response            response;
        ContainerInspection details;

        try
        {
            response = proxy.containerLogs(containerId, null, stdout, stderr, sinceInteger, true, null);
            details  = proxy.containerInspect(containerId, null);
        }
        catch (Exception e)
        {
            // Container gone or some other problem, return nothing.
            return res;
        }

        try (InputStream entity = (InputStream) response.getEntity())
        {
            if (details.config.tty == Boolean.TRUE)
            {
                BufferedInputStream stream = new BufferedInputStream(entity);
                Utf8ByteArray       reader = new Utf8ByteArray();
                String              line;

                while ((line = reader.readLine(stream)) != null)
                {
                    addLine(res, since, line, 1);

                    if (limit > 0 && res.size() >= limit)
                    {
                        break;
                    }
                }
            }
            else
            {
                byte[] buf = ExpandableArrayOfBytes.getTempBuffer(64 * 1024);

                while (true)
                {
                    Optional<Integer> optFd  = readInt(entity, true);
                    Optional<Integer> optLen = readInt(entity, false);

                    if (!optFd.isPresent() || !optLen.isPresent())
                    {
                        break;
                    }

                    int len = optLen.get();

                    int safeLen = Math.min(len, buf.length); // Truncate output at 64KB.
                    if (safeLen < len)
                    {
                        LoggerInstance.warn("Encountered really long log when reading from Container %s: %d", containerId, len);
                    }

                    int got = IOUtils.read(entity, buf, 0, safeLen);

                    String chunk = new String(buf, 0, safeLen, StandardCharsets.UTF_8);
                    addLine(res, since, chunk, optFd.get());

                    if (len > safeLen)
                    {
                        got += IOUtils.skip(entity, len - safeLen);
                    }

                    if (got != len)
                    {
                        // For some reason, the whole log did not come through. Just return.
                        LoggerInstance.warn("Failed to skip long log entry from Container %s, only got %d bytes out of %d", containerId, got, len);
                        break;
                    }

                    if (limit > 0 && res.size() >= limit)
                    {
                        break;
                    }
                }
            }
        }

        return res;
    }

    private void addLine(List<LogEntry> res,
                         ZonedDateTime since,
                         String chunk,
                         int fd)
    {
        LogEntry en = new LogEntry();
        en.fd = fd;

        int pos = chunk.indexOf(' ');
        if (pos >= 0)
        {
            en.timestamp = ZonedDateTime.parse(chunk.substring(0, pos));
            en.line      = chunk.substring(pos + 1);

            //
            // Due to the UNIX timestamp resolution of one second, we could get a few entries from the past.
            // This will filter them out.
            //
            if (since != null && !en.timestamp.isAfter(since))
            {
                return;
            }
        }
        else
        {
            en.line = chunk;
        }

        res.add(en);
    }

    private Optional<Integer> readInt(InputStream in,
                                      boolean littleEndian) throws
                                                            IOException
    {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
        {
            return Optional.empty();
        }

        if (littleEndian)
        {
            return Optional.of((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1));
        }
        else
        {
            return Optional.of((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4));
        }
    }

    //--//

    public List<ImageSummary> listImages(boolean all,
                                         String filters,
                                         boolean digests)
    {
        ImageApi proxy = createProxy(ImageApi.class);

        return proxy.imageList(all, filters, digests);
    }

    public Image inspectImage(DockerImageIdentifier image)
    {
        ImageApi proxy = createProxy(ImageApi.class);

        return proxy.imageInspect(image.fullName);
    }

    public Image inspectImageOrNull(DockerImageIdentifier image)
    {
        try
        {
            return inspectImage(image);
        }
        catch (NotFoundException e)
        {
            return null;
        }
    }

    public DockerImageArchitecture inspectImageArchitecture(DockerImageIdentifier image)
    {
        ImageApi proxy = createProxy(ImageApi.class);

        String architecture = null;

        Image details = proxy.imageInspect(image.fullName);

        DockerImageArchitecture arch = WellKnownDockerImageLabel.Architecture.getLabel(details.config.labels);
        if (arch != null)
        {
            return arch;
        }

        for (HistoryResponseItem history : proxy.imageHistory(image.fullName))
        {
            if (StringUtils.equals(history.id, "<missing>"))
            {
                continue;
            }

            try
            {
                Image imageFromHistory = proxy.imageInspect(history.id);
                if (imageFromHistory.architecture != null)
                {
                    architecture = imageFromHistory.architecture;
                }
            }
            catch (Throwable t)
            {
                // Ignore failures.
            }
        }

        return DockerImageArchitecture.parse(architecture);
    }

    public List<ImageDeleteResponseItem> removeImage(DockerImageIdentifier image,
                                                     boolean force)
    {
        ImageApi proxy = createProxy(ImageApi.class);

        try
        {
            return proxy.imageDelete(image.fullName, force, false);
        }
        catch (NotFoundException e)
        {
            return Collections.emptyList();
        }
    }

    public Image pullImage(DockerImageIdentifier image,
                           ConsumerWithException<BuildInfo> progressCallback) throws
                                                                              Exception
    {
        ImageApi proxy = createProxy(ImageApi.class);

        String xRegistryAuth = getRegistryAuth(image);

        Response response = proxy.imageCreate(image.fullName, null, null, null, null, xRegistryAuth);

        try (JsonStreamReader reader = new JsonStreamReader(response))
        {
            readBuildInfos(reader, progressCallback);
        }

        return proxy.imageInspect(image.fullName);
    }

    public String buildImage(Path sourceDirectory,
                             String dockerFile,
                             Map<String, String> buildArgs,
                             String registryAddress,
                             Map<String, String> labels,
                             ZonedDateTime overrideTime,
                             ConsumerWithException<BuildInfo> progressCallback) throws
                                                                                Exception
    {
        ImageApi proxy = createProxy(ImageApi.class);

        String xRegistryConfig = getRegistryConfig(registryAddress);

        try (FileSystem.TmpFileHolder holder = FileSystem.createTempFile())
        {
            try (FileOutputStream stream = new FileOutputStream(holder.get()))
            {
                TarBuilder.packDirectory(stream, sourceDirectory.toFile(), false, overrideTime);
            }

            try (FileInputStream stream = new FileInputStream(holder.get()))
            {
                //
                // Docker can either emit the details of a build or the image hash.
                // So we have to use a temporary tag to get the build output details *and* the image hash.
                // After the image has been built successfully, we inspect it to get its hash.
                //
                String temporaryImageTag = IdGenerator.newGuid();

                String labelsJson = (labels != null && !labels.isEmpty()) ? ObjectMappers.SkipNulls.writeValueAsString(labels) : null;

                // @formatter:off
                Response response = proxy.imageBuild(stream, dockerFile, 
                    /* t           */ temporaryImageTag,
                    /* extrahosts  */ null,
                    /* remote      */ null,
                    /* q           */ null,
                    /* nocache     */ null,
                    /* cachefrom   */ null,
                    /* pull        */ null,
                    /* rm          */ null,
                    /* forcerm     */ true,
                    /* memory      */ null,
                    /* memswap     */ null,
                    /* cpushares   */ null,
                    /* cpusetcpus  */ null,
                    /* cpuperiod   */ null,
                    /* cpuquota    */ null,
                    /* buildargs   */ buildArgs,
                    /* shmsize     */ null,
                    /* squash      */ null,
                    /* labels      */ labelsJson,
                    /* networkmode */ null,
                    /* contentType */ null,
                    xRegistryConfig);
                // @formatter:on

                try (JsonStreamReader reader = new JsonStreamReader(response))
                {
                    readBuildInfos(reader, progressCallback);

                    // Just verify the image is there.
                    @SuppressWarnings("unused") Image image = proxy.imageInspect(temporaryImageTag);
                }

                return temporaryImageTag;
            }
        }
    }

    private void readBuildInfos(JsonStreamReader reader,
                                ConsumerWithException<BuildInfo> progressCallback) throws
                                                                                   Exception
    {
        while (true)
        {
            BuildInfo bi = reader.readAs(BuildInfo.class);
            if (bi == null)
            {
                break;
            }

            if (progressCallback != null)
            {
                progressCallback.accept(bi);
            }
        }
    }

    public void tagImage(DockerImageIdentifier source,
                         DockerImageIdentifier target)
    {
        ImageApi proxy = createProxy(ImageApi.class);

        proxy.imageTag(source.fullName, target.getRepositoryName(), target.tag);
    }

    public String pushImage(DockerImageIdentifier image,
                            ConsumerWithException<BuildInfo> progressCallback) throws
                                                                               Exception
    {
        ImageApi proxy = createProxy(ImageApi.class);

        String xRegistryAuth = getRegistryAuth(image);

        Response response = proxy.imagePush(image.fullName, xRegistryAuth, image.tag);
        String   result   = null;

        try (JsonStreamReader reader = new JsonStreamReader(response))
        {
            while (true)
            {
                BuildInfo bi = reader.readAs(BuildInfo.class);
                if (bi == null)
                {
                    break;
                }

                if (bi.error != null)
                {
                    result = bi.error;
                }

                if (progressCallback != null)
                {
                    progressCallback.accept(bi);
                }
            }
        }

        return result;
    }

    public InputStream exportImage(DockerImageIdentifier image)
    {
        ImageApi proxy = createProxy(ImageApi.class);

        return proxy.imageGet(image.fullName);
    }

    public InputStream exportImages(List<DockerImageIdentifier> images)
    {
        ImageApi proxy = createProxy(ImageApi.class);

        return proxy.imageGetAll(CollectionUtils.transformToList(images, (img) -> img.fullName));
    }

    public void importImage(InputStream tarBall,
                            boolean quiet)
    {
        ImageApi proxy = createProxy(ImageApi.class);

        proxy.imageLoad(tarBall, quiet);
    }

    //--//

    public List<DockerImageIdentifier> listRegistryCatalog(String registryAddress)
    {
        RegistryApi proxy = createProxyForRegistry(RegistryApi.class, registryAddress);

        return listRegistryCatalog(proxy, registryAddress);
    }

    private List<DockerImageIdentifier> listRegistryCatalog(RegistryApi proxy,
                                                            String registryAddress)
    {
        List<DockerImageIdentifier> res = Lists.newArrayList();

        Catalog catalog = proxy.getCatalog(null, null);
        for (String repo : catalog.repositories)
        {
            DockerImageIdentifier image = new DockerImageIdentifier(registryAddress + "/" + repo);
            res.add(image);
        }

        return res;
    }

    public List<DockerImageIdentifier> listRegistryImageTags(DockerImageIdentifier image) throws
                                                                                          Exception
    {
        RegistryApi proxy = createProxyForRegistry(RegistryApi.class, image.getRegistryAddress());

        return listRegistryImageTags(proxy, image);
    }

    private List<DockerImageIdentifier> listRegistryImageTags(RegistryApi proxy,
                                                              DockerImageIdentifier image)
    {
        List<DockerImageIdentifier> res = Lists.newArrayList();

        Tags tags = proxy.getTags(image.getAccountAndName(), null, null);
        for (String tag : tags.tags)
        {
            DockerImageIdentifier imageTagged = new DockerImageIdentifier(image.fullName + ":" + tag);
            res.add(imageTagged);
        }

        return res;
    }

    public ManifestV1 getManifestV1(DockerImageIdentifier image)
    {
        RegistryApi proxy = createProxyForRegistry(RegistryApi.class, image.getRegistryAddress());

        return getManifestV1(proxy, image);
    }

    private ManifestV1 getManifestV1(RegistryApi proxy,
                                     DockerImageIdentifier image)
    {
        return proxy.getManifestV1(image.getAccountAndName(), image.tag);
    }

    public ManifestV2 getManifestV2(DockerImageIdentifier image)
    {
        RegistryApi proxy = createProxyForRegistry(RegistryApi.class, image.getRegistryAddress());

        return proxy.getManifestV2(image.getAccountAndName(), image.tag);
    }

    public String getManifestDigest(DockerImageIdentifier image)
    {
        RegistryApi proxy = createProxyForRegistry(RegistryApi.class, image.getRegistryAddress());

        return getManifestDigestInner(proxy, image);
    }

    private String getManifestDigestInner(RegistryApi proxy,
                                          DockerImageIdentifier image)
    {
        Response response = proxy.getManifestRaw(image.getAccountAndName(), image.tag);
        return response.getHeaderString("Docker-Content-Digest");
    }

    public void deleteManifest(String registryAddress,
                               DockerImageIdentifier image)
    {
        RegistryApi proxy = createProxyForRegistry(RegistryApi.class, registryAddress);

        proxy.deleteManifest(image.getAccountAndName(), getManifestDigestInner(proxy, image));
    }

    public static ManifestV1Decoded decodeManifestHistory(History h) throws
                                                                     JsonParseException,
                                                                     JsonMappingException,
                                                                     IOException
    {
        return s_objectMapper.readValue(h.v1Compatibility, ManifestV1Decoded.class);
    }

    public List<RegistryImage> analyzeRegistry(String registryAddress,
                                               boolean inParallel) throws
                                                                   Exception
    {
        List<RegistryImage> results = Lists.newArrayList();

        RegistryApi proxy = createProxyForRegistry(RegistryApi.class, registryAddress);

        List<Future<Integer>>       promises = Lists.newArrayList();
        List<DockerImageIdentifier> repos    = listRegistryCatalog(proxy, registryAddress);

        if (inParallel)
        {
            //
            // Fetch each repo in parallel.
            //
            for (DockerImageIdentifier repo : repos)
            {
                promises.add(Executors.getDefaultThreadPool()
                                      .submit(() -> listImagesOnRepo(proxy, repo, results)));
            }

            //
            // Wait for all the repos to be done.
            //
            for (Future<Integer> promise : promises)
            {
                promise.get();
            }
        }
        else
        {
            //
            // Fetch each repo in parallel.
            //
            for (DockerImageIdentifier repo : repos)
            {
                listImagesOnRepo(proxy, repo, results);
            }
        }

        return results;
    }

    private int listImagesOnRepo(RegistryApi proxy,
                                 DockerImageIdentifier repo,
                                 List<RegistryImage> results) throws
                                                              Exception
    {
        int count = 0;

        List<DockerImageIdentifier> tags = listRegistryImageTags(proxy, repo);
        for (DockerImageIdentifier tag : tags)
        {
            RegistryImage ri = new RegistryImage();
            ri.tag = tag;

            DockerImageArchitecture archAuthoritative    = null;
            DockerImageArchitecture archNonAuthoritative = null;

            ManifestV1 manifestV1 = getManifestV1(proxy, tag);
            for (History h : manifestV1.history)
            {
                ManifestV1Decoded hDecoded = DockerHelper.decodeManifestHistory(h);

                if (hDecoded.config != null && hDecoded.config.labels != null)
                {
                    ri.labels.putAll(hDecoded.config.labels);

                    DockerImageArchitecture arch = WellKnownDockerImageLabel.Architecture.getLabel(ri.labels);
                    if (arch != null)
                    {
                        archAuthoritative = arch;
                    }
                }

                if (archNonAuthoritative == null)
                {
                    archNonAuthoritative = DockerImageArchitecture.parse(hDecoded.architecture);
                }
            }

            ri.architecture = archAuthoritative != null ? archAuthoritative : archNonAuthoritative;

            ManifestV2 manifestV2 = getManifestV2(tag);
            ri.imageSha = manifestV2.config.digest;

            synchronized (results)
            {
                results.add(ri);
                count++;
            }
        }

        return count;
    }

    //--//

    public List<Volume> listVolumes(Map<String, String> filterByLabels)
    {
        List<Volume> res = Lists.newArrayList();

        VolumeApi         proxy = createProxy(VolumeApi.class);
        VolumesListOKBody body  = proxy.volumeList(null);
        for (Volume vol : body.volumes)
        {
            if (isMatch(filterByLabels, vol.labels))
            {
                res.add(vol);
            }
        }

        return res;
    }

    public Volume inspectVolume(String volumeName)
    {
        VolumeApi proxy = createProxy(VolumeApi.class);

        return proxy.volumeInspect(volumeName);
    }

    public Volume createVolume(VolumesCreateBody config)
    {
        VolumeApi proxy = createProxy(VolumeApi.class);

        return proxy.volumeCreate(config);
    }

    public void deleteVolume(String volumeName,
                             boolean force)
    {
        VolumeApi proxy = createProxy(VolumeApi.class);

        try
        {
            proxy.volumeDelete(volumeName, force);
        }
        catch (NotFoundException e)
        {
            // Not there, we are good!
        }
    }

    //--//

    public List<Network> listNetwork(Map<String, String> filterByLabels)
    {
        List<Network> res = Lists.newArrayList();

        NetworkApi    proxy    = createProxy(NetworkApi.class);
        List<Network> networks = proxy.networkList(null);
        for (Network network : networks)
        {
            if (isMatch(filterByLabels, network.labels))
            {
                res.add(network);
            }
        }

        return res;
    }

    public Network inspectNetwork(String networkName)
    {
        NetworkApi proxy = createProxy(NetworkApi.class);

        return proxy.networkInspect(networkName, false);
    }

    public String createNetworkIfMissing(String networkName,
                                         String driver)
    {
        for (Network network : listNetwork(null))
        {
            if (StringUtils.equals(network.name, networkName))
            {
                return network.id;
            }
        }

        NetworkCreateConfig networkCfg = new NetworkCreateConfig();
        networkCfg.name   = networkName;
        networkCfg.driver = driver;
        return createNetwork(networkCfg);
    }

    public String createNetwork(NetworkCreateConfig config)
    {
        NetworkApi proxy = createProxy(NetworkApi.class);

        NetworkCreateResponse res = proxy.networkCreate(config);
        return res.id;
    }

    public void deleteNetwork(String networkName)
    {
        NetworkApi proxy = createProxy(NetworkApi.class);

        proxy.networkDelete(networkName);
    }

    public NetworksPruneReport pruneNetworks()
    {
        NetworkApi proxy = createProxy(NetworkApi.class);

        return proxy.networkPrune(null);
    }

    //--//

    public List<ContainerSummary> listContainers(boolean all,
                                                 Map<String, String> filterByLabels)
    {
        List<ContainerSummary> res = Lists.newArrayList();

        ContainerApi proxy = createProxy(ContainerApi.class);
        for (ContainerSummary cs : proxy.containerList(all, null, null, null))
        {
            if (isMatch(filterByLabels, cs.labels))
            {
                res.add(cs);
            }
        }

        return res;
    }

    public String createContainer(String name,
                                  ContainerBuilder builder)
    {
        ContainerApi proxy = createProxy(ContainerApi.class);

        ContainerCreateCreatedBody res = proxy.containerCreate(builder.build(), name);
        return res.id;
    }

    public void renameContainer(String containerId,
                                String name)
    {
        ContainerApi proxy = createProxy(ContainerApi.class);

        proxy.containerRename(containerId, name);
    }

    public ContainerInspection inspectContainer(String containerId)
    {
        ContainerApi proxy = createProxy(ContainerApi.class);

        return proxy.containerInspect(containerId, null);
    }

    public ContainerInspection inspectContainerNoThrow(String containerId)
    {
        //
        // Look, Docker can be unreliable at times and report the wrong results....
        // So we retry a few times...
        //
        for (int retries = 0; retries < 3; retries++)
        {
            try
            {
                return inspectContainer(containerId);
            }
            catch (NotFoundException e)
            {
                Executors.safeSleep(10);
            }
        }

        return null;
    }

    public Exception startContainer(String containerId,
                                    Duration timeout)
    {
        try
        {
            ContainerApi proxy = createProxy(ContainerApi.class);

            ContainerInspection container = proxy.containerInspect(containerId, null);
            if (!DockerHelper.isRunning(container))
            {
                proxy.containerStart(containerId, null);

                if (timeout != null)
                {
                    MonotonousTime until = TimeUtils.computeTimeoutExpiration(timeout);
                    while (true)
                    {
                        if (TimeUtils.isTimeoutExpired(until))
                        {
                            return Exceptions.newTimeoutException("Failed to start container '%s', due to timeout", containerId);
                        }

                        container = proxy.containerInspect(containerId, null);
                        if (isRunning(container))
                        {
                            break;
                        }

                        Executors.safeSleep(200);
                    }
                }
            }

            return null;
        }
        catch (Exception e2)
        {
            return e2;
        }
    }

    public Exception signalContainer(String containerId,
                                     String signal)
    {
        try
        {
            ContainerApi proxy = createProxy(ContainerApi.class);

            ContainerInspection container = proxy.containerInspect(containerId, null);
            if (DockerHelper.isRunning(container))
            {
                proxy.containerKill(containerId, signal);
            }

            return null;
        }
        catch (NotFoundException e)
        {
            // Doesn't exist.

            return null;
        }
        catch (Exception e2)
        {
            // Something went wrong...
            return e2;
        }
    }

    public Exception stopContainer(String containerId,
                                   Duration timeout)
    {
        try
        {
            ContainerApi proxy = createProxy(ContainerApi.class);

            ContainerInspection container = proxy.containerInspect(containerId, null);
            if (DockerHelper.isRunning(container))
            {
                proxy.containerStop(containerId, null);

                if (timeout != null)
                {
                    MonotonousTime until = TimeUtils.computeTimeoutExpiration(timeout);
                    while (true)
                    {
                        if (TimeUtils.isTimeoutExpired(until))
                        {
                            return Exceptions.newTimeoutException("Failed to stop container '%s', due to timeout", containerId);
                        }

                        container = proxy.containerInspect(containerId, null);
                        if (!isRunning(container))
                        {
                            break;
                        }

                        Executors.safeSleep(200);
                    }
                }
            }

            return null;
        }
        catch (NotFoundException e)
        {
            // Doesn't exist.

            return null;
        }
        catch (Exception e2)
        {
            // Something went wrong...
            return e2;
        }
    }

    public void deleteContainer(String containerId,
                                boolean volume,
                                boolean force)
    {
        ContainerApi proxy = createProxy(ContainerApi.class);

        proxy.containerDelete(containerId, volume ? true : null, force ? true : null, null);

        try
        {
            pruneNetworks();
        }
        catch (Exception e)
        {
            // Ignore failures.
        }
    }

    //--//

    public boolean ensureDirectory(String containerId,
                                   String containerPath) throws
                                                         Exception
    {
        if (StringUtils.isNotEmpty(containerPath))
        {
            ByteArrayOutputStream dirStream = new ByteArrayOutputStream();
            try (TarBuilder tarBuilder = new TarBuilder(dirStream, false))
            {
                String tarPath = null;

                for (String pathPart : StringUtils.split(containerPath, "/"))
                {
                    if (StringUtils.isEmpty(pathPart))
                    {
                        continue;
                    }

                    tarBuilder.addAsDir(tarPath, pathPart, 0755);

                    if (tarPath == null)
                    {
                        tarPath = pathPart;
                    }
                    else
                    {
                        tarPath += "/" + pathPart;
                    }
                }

                if (tarPath != null) // We added at least one directory to the tar.
                {
                    return writeArchiveToContainer(containerId, "/", new ByteArrayInputStream(dirStream.toByteArray()), false);
                }
            }
        }

        return false;
    }

    public boolean readArchiveFromContainer(String containerId,
                                            String path,
                                            PredicateWithException<TarArchiveEntry> callback) throws
                                                                                              Exception
    {
        ContainerApi proxy = createProxy(ContainerApi.class);

        try
        {
            Response response = proxy.containerArchive(containerId, path, true);

            try (InputStream entity = (InputStream) response.getEntity())
            {
                TarWalker.walk(entity, false, callback);
            }

            return true;
        }
        catch (NotFoundException e)
        {
            return false;
        }
    }

    public boolean readArchiveFromContainer(String containerId,
                                            String containerPath,
                                            File outputFile,
                                            boolean compress) throws
                                                              Exception

    {
        try (FileOutputStream output = new FileOutputStream(outputFile))
        {
            return readArchiveFromContainer(containerId, containerPath, output, compress);
        }
    }

    public boolean readArchiveFromContainer(String containerId,
                                            String containerPath,
                                            OutputStream output,
                                            boolean compress) throws
                                                              Exception

    {
        if (compress)
        {
            try (GZIPOutputStream gzip = new GZIPOutputStream(output))
            {
                return readArchiveFromContainer(containerId, containerPath, gzip);
            }
        }
        else
        {
            return readArchiveFromContainer(containerId, containerPath, output);
        }
    }

    public boolean readArchiveFromContainer(String containerId,
                                            String containerPath,
                                            OutputStream output) throws
                                                                 Exception

    {
        ContainerApi proxy = createProxy(ContainerApi.class);

        try
        {
            Response response = proxy.containerArchive(containerId, containerPath, true);

            try (InputStream entity = (InputStream) response.getEntity())
            {
                //
                // Unfortunately, when packing a directory, Docker returns a TAR file that includes the name of the archived directory.
                // We need to walk through the tar file and remove the prefix.
                //
                if (containerPath.endsWith("/"))
                {
                    // We need to strip the name of the directory.
                    Path path = Paths.get(containerPath);
                    removePrefix(output,
                                 entity,
                                 path.getFileName()
                                     .toString() + "/");
                }
                else
                {
                    // We are looking at a single file, copy as-is.
                    IOUtils.copyLarge(entity, output);
                }
            }

            return true;
        }
        catch (NotFoundException e)
        {
            return false;
        }
    }

    private void removePrefix(OutputStream output,
                              InputStream entity,
                              String prefix) throws
                                             Exception
    {
        try (TarBuilder builder = new TarBuilder(output, false))
        {
            TarWalker.walk(entity, false, (entry) ->
            {
                String name = entry.name;

                if (name.length() > prefix.length() && entry.name.startsWith(prefix))
                {
                    entry.name = entry.name.substring(prefix.length());

                    builder.addAsStream(entry, entry.getStream());
                }

                return true;
            });
        }
    }

    public boolean writeArchiveToContainer(String containerId,
                                           String path,
                                           File inputFile,
                                           boolean decompress) throws
                                                               Exception

    {
        try (FileInputStream stream = new FileInputStream(inputFile))
        {
            return writeArchiveToContainer(containerId, path, stream, decompress);
        }
    }

    public boolean writeArchiveToContainer(String containerId,
                                           String path,
                                           InputStream input,
                                           boolean decompress) throws
                                                               Exception

    {
        if (decompress)
        {
            input = new GZIPInputStream(input);
        }

        ContainerApi proxy = createProxy(ContainerApi.class);
        try
        {
            proxy.putContainerArchive(containerId, path, input, "true");

            return true;
        }
        catch (NotFoundException e)
        {
            return false;
        }
    }

    //--//

    public static <T> CompletableFuture<T> callWithHelper(UserInfo registryUser,
                                                          FunctionWithException<DockerHelper, T> callback)
    {
        try (DockerHelper helper = new DockerHelper(registryUser))
        {
            return wrapAsync(callback.apply(helper));
        }
        catch (Throwable e)
        {
            CompletableFuture<T> res = new CompletableFuture<T>();

            res.completeExceptionally(e);
            return res;
        }
    }

    public static <T> CompletableFuture<T> callWithHelperAndAutoRetry(int maxRetries,
                                                                      UserInfo registryUser,
                                                                      FunctionWithException<DockerHelper, T> callback)
    {
        CompletableFuture<T> res = new CompletableFuture<T>();

        while (true)
        {
            try (DockerHelper helper = new DockerHelper(registryUser))
            {
                T val = callback.apply(helper);
                res.complete(val);
                return res;
            }
            catch (Throwable e)
            {
                if (maxRetries-- > 0)
                {
                    continue;
                }

                LoggerInstance.error("Operation failed with %s", e);

                res.completeExceptionally(e);
                return res;
            }
        }
    }

    public static <T> T callWithFailureFiltering(Callable<T> callback,
                                                 Response.Status... expected) throws
                                                                              Exception
    {
        try
        {
            return callback.call();
        }
        catch (ClientErrorException ex)
        {
            Response.Status res = extractResponseStatus(ex);
            for (Response.Status status : expected)
            {
                if (res == status)
                {
                    return null;
                }
            }

            throw ex;
        }
    }

    public static void callWithFailureFiltering(RunnableWithException callback,
                                                Response.Status... expected) throws
                                                                             Exception
    {
        try
        {
            callback.run();
        }
        catch (ClientErrorException ex)
        {
            Response.Status res = extractResponseStatus(ex);
            for (Response.Status status : expected)
            {
                if (res == status)
                {
                    return;
                }
            }

            throw ex;
        }
    }

    public static Response.Status extractResponseStatus(ClientErrorException e)
    {
        Response response = e.getResponse();
        return Response.Status.fromStatusCode(response.getStatus());
    }

    public static boolean isRunning(ContainerInspection container)
    {
        return container != null && container.state != null && BoxingUtils.get(container.state.running, false);
    }

    public static boolean isPaused(ContainerInspection container)
    {
        return container != null && container.state != null && BoxingUtils.get(container.state.paused, false);
    }

    //--//

    private static ContainerInspection lookForSelfId(String selfId,
                                                     ContainerApi proxy,
                                                     ContainerSummary summary)
    {
        try
        {
            ContainerInspection inspect = proxy.containerInspect(summary.id, null);

            for (String val : inspect.config.env)
            {
                int pos = val.indexOf('=');
                if (pos > 0)
                {
                    String name = val.substring(0, pos);

                    if (name.equals(SELFID))
                    {
                        String value = val.substring(pos + 1);

                        if (value.equals(selfId))
                        {
                            //
                            // We found ourselves inside Docker!
                            //
                            return inspect;
                        }
                    }
                }
            }
        }
        catch (Throwable ex)
        {
            // Skip failed container inspections, it could be the container is gone.
        }

        return null;
    }

    //--//

    public static boolean isMatch(Map<String, String> expected,
                                  Map<String, String> labels)
    {
        if (expected != null && !expected.isEmpty())
        {
            if (labels == null)
            {
                return false;
            }

            for (String key : expected.keySet())
            {
                if (!labels.containsKey(key))
                {
                    return false;
                }

                String expectedValue = expected.get(key);
                if (expectedValue != null && !expectedValue.equals(labels.get(key)))
                {
                    return false;
                }
            }
        }

        return true;
    }

    //--//

    private String getRegistryAuth(DockerImageIdentifier image)
    {
        if (m_registryUser == null)
        {
            return null;
        }

        AuthConfig auth = new AuthConfig();
        auth.username      = m_registryUser.user;
        auth.password      = m_registryUser.getEffectivePassword();
        auth.email         = m_registryUser.getEffectiveEmailAddress();
        auth.serveraddress = image.getRegistryAddress();

        return encodeAsJsonBase64(auth);
    }

    private String getRegistryConfig(String registryAddress)
    {
        if (m_registryUser == null)
        {
            return null;
        }

        RegistryConfig reg = new RegistryConfig();

        reg.add(registryAddress, m_registryUser);

        return encodeAsJsonBase64(reg);
    }

    //--//

    private <P> P createProxy(Class<P> cls)
    {
        return createProxy(cls, m_baseAddress);
    }

    private <P> P createProxyForRegistry(Class<P> cls,
                                         String registryAddress)
    {
        return createProxy(cls, "https://" + registryAddress + DOCKER_REGISTRY_API_VERSION);
    }

    private <P> P createProxy(Class<P> cls,
                              String baseAddress)
    {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(baseAddress);
        bean.setResourceClass(cls);
        bean.setProvider(s_jsonProvider);

        if (m_registryUser != null)
        {
            bean.setUsername(m_registryUser.user);
            bean.setPassword(m_registryUser.getEffectivePassword());
        }

        Client client = bean.createWithValues();

        HTTPConduit conduit = WebClient.getConfig(client)
                                       .getHttpConduit();
        conduit.getClient()
               .setReceiveTimeout(m_timeout);

        return cls.cast(client);
    }
}
