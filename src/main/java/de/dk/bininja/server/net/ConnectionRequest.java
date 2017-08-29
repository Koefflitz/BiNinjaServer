package de.dk.bininja.server.net;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Objects;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dk.bininja.net.Base64Connection;
import de.dk.bininja.net.packet.ConnectionRequestPacket;
import de.dk.util.net.Coder;
import de.dk.util.net.security.CipherCoderAdapter;
import de.dk.util.net.security.SessionKeyArrangement;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public class ConnectionRequest implements Resource {
   private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionRequest.class);

   private final ConnectionRequestHandler handler;
   private final Base64Connection connection;
   private final Thread thread;

   public ConnectionRequest(Base64Connection connection, ConnectionRequestHandler handler) throws IOException {
      this.connection = Objects.requireNonNull(connection);
      this.handler = handler;
      this.thread = new Thread(this::run);
   }

   public void establish() {
      thread.start();
   }

   private void run() {
      LOGGER.debug("Reading the initial message");
      ConnectionRequestPacket packet;
      try {
         packet = (ConnectionRequestPacket) connection.readObject();
      } catch (IOException e) {
         handler.failed(this, new IOException("Failed to read initial message.", e));
         return;
      }

      LOGGER.debug("Initial message received: " + packet);

      if (packet.isSecure()) {
         LOGGER.debug("Securing connection");
         SessionKeyArrangement builder = new SessionKeyArrangement(connection, connection.getObjectOutput());
         Coder secureCoder;
         LOGGER.debug("Arranging the session key.");
         try {
            SecretKey sessionKey = handler.buildSecureCoder(builder);
            secureCoder = new CipherCoderAdapter(sessionKey);
         } catch (IOException e) {
            handler.failed(this, e);
            return;
         } catch (GeneralSecurityException e) {
            handler.failed(this, new IOException("Could not create coder.", e));
            return;
         }
         connection.appendCoder(secureCoder);
         LOGGER.debug("Session key arranged.");

         LOGGER.debug("Reading the type of the client.");
         try {
            packet = (ConnectionRequestPacket) connection.readObject();
         } catch (IOException e) {
            handler.failed(this, new IOException("Could not read connection type.", e));
         }
      }

      LOGGER.debug("Connection type received: " + packet.getConnectionType());
      if (packet.getConnectionType() == null)
         handler.failed(this, new IOException("No connection type specified."));

      switch (packet.getConnectionType()) {
      case ADMIN:
         handler.newAdminConnection(this, connection);
         break;
      case CLIENT:
         handler.newDownloadConnection(this, connection);
         break;
      case ALL:
         handler.failed(this, new IOException("Could not establish connection of type " + packet.getConnectionType()));
         break;
      }

   }

   @Override
   public void close(long timeout) throws IOException, InterruptedException {
      LOGGER.debug("Closing ConnectionRequest from " + connection.getInetAddress());
      destroy(timeout);
      LOGGER.debug("ConnectionRequest from " + connection.getInetAddress() + " closed.");
   }

   @Override
   public void destroy(long timeout) throws IOException, InterruptedException {
      if (!connection.isClosed())
         connection.close(0);

      if (thread != null && thread != Thread.currentThread())
         thread.join(timeout);
   }

   public Base64Connection getConnection() {
      return connection;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((this.connection == null) ? 0 : this.connection.getInetAddress().hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      ConnectionRequest other = (ConnectionRequest) obj;
      if (this.connection == null) {
         if (other.connection != null)
            return false;
      } else if (!this.connection.getInetAddress().equals(other.connection.getInetAddress()))
         return false;
      return true;
   }
}
