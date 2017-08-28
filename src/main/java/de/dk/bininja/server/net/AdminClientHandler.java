package de.dk.bininja.server.net;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dk.bininja.net.Base64Connection;
import de.dk.bininja.net.packet.admin.AdminPacket;
import de.dk.bininja.net.packet.admin.AdminPacket.AdminPacketType;
import de.dk.bininja.net.packet.admin.BooleanAnswerPacket;
import de.dk.bininja.net.packet.admin.ConnectionDetailsPacket;
import de.dk.bininja.net.packet.admin.CountConnectionsPacket;
import de.dk.bininja.net.packet.admin.CountConnectionsResultPacket;
import de.dk.bininja.net.packet.admin.ReadBufferSizePacket;
import de.dk.bininja.net.packet.admin.SetBufferSizePacket;
import de.dk.bininja.server.controller.AdminClientController;
import de.dk.bininja.server.controller.ClientHandler;
import de.dk.util.net.Connection;
import de.dk.util.net.ConnectionListener;
import de.dk.util.net.Receiver;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public class AdminClientHandler implements ClientHandler, Receiver, ConnectionListener {
   private static final Logger LOGGER = LoggerFactory.getLogger(AdminClientHandler.class);

   private final Base64Connection connection;
   private final AdminClientController controller;

   public AdminClientHandler(Base64Connection connection, AdminClientController controller) throws IOException {
      this.connection = connection;
      this.controller = controller;
      connection.addListener(this);
      connection.addReceiver(this);
      connection.start();
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
         getConnectionDetails((ConnectionDetailsPacket) packet);
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
      LOGGER.debug("Sending answer that buffer size was set to " + connection.getInetAddress());
      try {
         connection.send(new BooleanAnswerPacket(AdminPacketType.SET_BUFFER_SIZE, true));
      } catch (IOException e) {
         String msg = String.format("Could not send answer to %s, that the buffer size was set to %s",
                                    connection.getInetAddress(),
                                    packet.getBufferSize());
         LOGGER.error(msg, e);
      }
   }

   private void readBufferSize() {
      int bufferSize = controller.readBufferSize();
      LOGGER.debug("Sending answer of the buffer size to " + connection.getInetAddress());
      try {
         connection.send(new ReadBufferSizePacket(bufferSize));
      } catch (IOException e) {
         LOGGER.error("Could not send answer for readBufferSize to admin client " + connection.getInetAddress());
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

      LOGGER.debug("Sending answer of the connection count to " + connection.getInetAddress());
      try {
         connection.send(new CountConnectionsResultPacket(count));
      } catch (IOException e) {
         LOGGER.error("Could not send answer for countConnections to admin client " + connection.getInetAddress());
      }
   }

   private void getConnectionDetails(ConnectionDetailsPacket packet) {
      packet.setConnectionDetails(controller.getConnectionDetailsOf(packet.getConnectionType()));
      LOGGER.debug("Sending answer of the connection details to " + connection.getInetAddress());
      try {
         connection.send(packet);
      } catch (IOException e) {
         LOGGER.error("Could not send answer for readConnectionDetails to admin client " + connection.getInetAddress(), e);
      }
   }

   private void shutdown() {
      LOGGER.debug("Sending answer that Im shutting down to " + connection.getInetAddress());
      try {
         connection.send(new BooleanAnswerPacket(AdminPacketType.SHUTDOWN, true));
      } catch (IOException e) {
         LOGGER.error("Could not send answer that shutdown is innitiated to admin client " + connection.getInetAddress());
      }
      controller.shutdown();
   }

   @Override
   public void closed() {
      LOGGER.debug("Connection to admin client " + connection.getInetAddress() + " closed.");
   }

   @Override
   public Connection getConnection() {
      return connection;
   }

   @Override
   public void close(long timeout) throws InterruptedException {
      try {
         connection.close(timeout);
      } catch (IOException e) {
         LOGGER.warn("An error occured while closing the connection to: " + connection.getInetAddress());
      }
   }

   @Override
   public void destroy() {
      if (!connection.isClosed()) {
         try {
            connection.close();
         } catch (IOException e) {
            LOGGER.warn("An error occured while closing the connection to: " + connection.getInetAddress());
         }
      }
   }
}
