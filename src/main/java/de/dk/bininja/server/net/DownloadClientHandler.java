package de.dk.bininja.server.net;

import java.io.IOException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dk.bininja.net.Base64Connection;
import de.dk.bininja.net.ConnectionMetadata;
import de.dk.bininja.net.ConnectionType;
import de.dk.bininja.net.DownloadManager;
import de.dk.bininja.net.packet.download.DownloadPacket;
import de.dk.bininja.server.controller.ClientHandler;
import de.dk.util.channel.Channel;
import de.dk.util.channel.ChannelDeclinedException;
import de.dk.util.channel.ChannelHandler;
import de.dk.util.net.ConnectionListener;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public class DownloadClientHandler implements ClientHandler,
                                              ChannelHandler<DownloadPacket>,
                                              ConnectionListener {
   private static final Logger LOGGER = LoggerFactory.getLogger(DownloadClientHandler.class);

   private final Base64Connection connection;
   private DownloadManager<ServerDownload> downloads = new DownloadManager<>();

   private boolean secure;
   private final long timeStamp;

   public DownloadClientHandler(Base64Connection connection,
                                boolean secure) throws IOException,
                                                       NullPointerException {
      this.connection = Objects.requireNonNull(connection);
      this.secure = secure;
      connection.addListener(this);
      connection.attachMultiplexer(this);
      connection.start();
      this.timeStamp = System.currentTimeMillis();
   }

   @Override
   public void newChannelRequested(Channel<DownloadPacket> channel,
                                   DownloadPacket initialMsg) throws ChannelDeclinedException {
      LOGGER.debug("A new channel is requested by the client.");
      Channel<DownloadPacket> downloadChannel = (Channel<DownloadPacket>) channel;
      ServerDownload download = new ServerDownload(downloadChannel);
      downloadChannel.addListener(download);
      downloads.add(download);
   }

   @Override
   public void closed() {
      LOGGER.debug("Connection to download client " + connection.getInetAddress() + " closed.");
   }

   @Override
   public synchronized void close(long timeout) throws IOException, InterruptedException {
      LOGGER.debug("Breaking up with " + connection.getInetAddress());

      LOGGER.debug("Canceling downloads to " + connection.getInetAddress());
      for (ServerDownload download : downloads)
         download.cancel(null, timeout);

      if (!connection.isClosed()) {
         LOGGER.debug("Closing connection to " + connection.getInetAddress());
         connection.close(timeout);
      }
   }

   @Override
   public void destroy(long timeout) throws IOException, InterruptedException {
      if (!connection.isClosed())
         connection.close(timeout);
   }

   @Override
   public Base64Connection getConnection() {
      return connection;
   }

   @Override
   public ConnectionMetadata getMetadata() {
      String host = connection.getInetAddress()
                              .toString();

      int port = connection.getSocket()
                           .getLocalPort();

      return new ConnectionMetadata(host,
                                    port,
                                    ConnectionType.CLIENT,
                                    secure,
                                    timeStamp,
                                    connection.getBytesSent(),
                                    connection.getBytesReceived());
   }

   @Override
   public void channelClosed(Channel<DownloadPacket> channel) {
      // Nothing to do here
   }

   @Override
   public Class<DownloadPacket> getType() {
      return DownloadPacket.class;
   }
}
