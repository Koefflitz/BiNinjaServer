package de.dk.bininja.server.net;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dk.bininja.net.Base64Connection;
import de.dk.bininja.net.ConnectionMetadata;
import de.dk.bininja.net.ConnectionType;
import de.dk.bininja.net.packet.admin.AdminPacket;
import de.dk.bininja.net.packet.admin.AdminPacket.AdminPacketType;
import de.dk.bininja.net.packet.admin.BooleanAnswerPacket;
import de.dk.bininja.net.packet.admin.ConnectionMetaPacket;
import de.dk.bininja.net.packet.admin.CountConnectionsPacket;
import de.dk.bininja.net.packet.admin.CountConnectionsResultPacket;
import de.dk.bininja.net.packet.admin.ReadBufferSizePacket;
import de.dk.bininja.net.packet.admin.SetBufferSizePacket;
import de.dk.bininja.server.controller.AdminClientController;
import de.dk.bininja.server.controller.ClientHandler;
import de.dk.ch.Receiver;
import de.dk.util.net.Connection;
import de.dk.util.net.ConnectionListener;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public class AdminClientHandler implements ClientHandler, Receiver, ConnectionListener {
   private static final Logger LOGGER = LoggerFactory.getLogger(AdminClientHandler.class);

   private final Base64Connection connection;
   private final AdminClientController controller;

   private boolean secure;

   private final long timeStamp;

   public AdminClientHandler(Base64Connection connection,
                             AdminClientController controller,
                             boolean secure) throws IOException {
      this.connection = connection;
      this.controller = controller;
      this.secure = secure;
      connection.addListener(this);
      connection.addReceiver(this);
      connection.start();
      this.timeStamp = System.currentTimeMillis();
   }

   @Override
   public void receive(Object msg) throws IllegalArgumentException {
      LOGGER.debug("Message received: " + msg);
      if (!(msg instanceof AdminPacket))
         throw new IllegalArgumentException("The received message was no AdminPacket: " + msg);

      AdminPacket packet = (AdminPacket) msg;
      switch (packet.getType()) {
      case COUNT_CONNECTIONS:
         countConnections((CountConnectionsPacket) packet);
         break;
      case CONNECTION_DETAILS:
         getConnectionMetadata((ConnectionMetaPacket) packet);
         break;
      case READ_BUFFER_SIZE:
         readBufferSize();
         break;
      case SET_BUFFER_SIZE:
         setBufferSize((SetBufferSizePacket) packet);
         break;
      case SHUTDOWN:
         shutdown();
         break;
      }
   }

   private void setBufferSize(SetBufferSizePacket packet) {
      controller.setBufferSize(packet.getBufferSize());
      LOGGER.debug("Sending answer that buffer size was set to " + connection.getAddress());
      try {
         connection.send(new BooleanAnswerPacket(AdminPacketType.SET_BUFFER_SIZE, true));
      } catch (IOException e) {
         String msg = String.format("Could not send answer to %s, that the buffer size was set to %s",
                                    connection.getAddress(),
                                    packet.getBufferSize());
         LOGGER.error(msg, e);
      }
   }

   private void readBufferSize() {
      int bufferSize = controller.readBufferSize();
      LOGGER.debug("Sending answer of the buffer size to " + connection.getAddress());
      try {
         connection.send(new ReadBufferSizePacket(bufferSize));
      } catch (IOException e) {
         LOGGER.error("Could not send answer for readBufferSize to admin client " + connection.getAddress());
      }
   }

   private void countConnections(CountConnectionsPacket packet) {
      int count;
      switch (packet.getConnectionType()) {
      case ADMIN:
         count = controller.countAdminConnections();
         break;
      case CLIENT:
         count = controller.countDownloadConnections();
         break;
      case ALL:
         count = controller.countTotalConnections();
         break;
      default:
         LOGGER.warn("A CountConnectionsPacket with an invalid ConnectionType was received: " + packet);
         return;
      }

      LOGGER.debug("Sending answer of the connection count to " + connection.getAddress());
      try {
         connection.send(new CountConnectionsResultPacket(count));
      } catch (IOException e) {
         LOGGER.error("Could not send answer for countConnections to admin client " + connection.getAddress());
      }
   }

   private void getConnectionMetadata(ConnectionMetaPacket packet) {
      packet.setConnectionMeta(controller.getConnectionMetadataOf(packet.getConnectionType()));
      LOGGER.debug("Sending answer of the connection details to " + connection.getAddress());
      try {
         connection.send(packet);
      } catch (IOException e) {
         LOGGER.error("Could not send answer for readConnectionDetails to admin client " + connection.getAddress(), e);
      }
   }

   private void shutdown() {
      LOGGER.debug("Sending answer that Im shutting down to " + connection.getAddress());
      try {
         connection.send(new BooleanAnswerPacket(AdminPacketType.SHUTDOWN, true));
      } catch (IOException e) {
         LOGGER.error("Could not send answer that shutdown is innitiated to admin client " + connection.getAddress());
      }
      controller.shutdown();
   }

   @Override
   public void closed(Connection connection) {
      LOGGER.debug("Connection to admin client " + connection.getAddress() + " closed.");
   }

   @Override
   public Connection getConnection() {
      return connection;
   }

   @Override
   public ConnectionMetadata getMetadata() {
      String host = connection.getAddress()
                              .toString();

      int port = connection.getSocket()
                           .getLocalPort();

      return new ConnectionMetadata(host,
                                    port,
                                    ConnectionType.ADMIN,
                                    secure,
                                    timeStamp,
                                    connection.getBytesSent(),
                                    connection.getBytesReceived());
   }

   @Override
   public void close(long timeout) throws IOException, InterruptedException {
      LOGGER.debug("Closing admin connection to " + connection.getAddress());
      destroy(timeout);
      LOGGER.debug("Admin connection to " + connection.getAddress() + " closed.");
   }

   @Override
   public void destroy(long timeout) throws IOException, InterruptedException {
      if (connection != null && !connection.isClosed())
         connection.close(timeout);
   }
}
