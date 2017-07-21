package de.dk.bininja.server.net;

import java.io.IOException;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dk.bininja.net.Base64Connection;
import de.dk.bininja.net.ConnectionType;

public class ConnectionRequest implements Resource {
   private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionRequest.class);

   private final ConnectionRequestHandler handler;
   private final Socket socket;
   private final Thread thread;

   private Scanner scanner;

   public ConnectionRequest(Socket socket, ConnectionRequestHandler handler) throws IOException {
      this.socket = socket;
      this.handler = handler;
      this.thread = new Thread(this::run);
      this.scanner = new Scanner(socket.getInputStream());
      scanner.useDelimiter("\\" + Base64Connection.MSG_DELIMITER);
   }

   public void establish() {
      thread.start();
   }

   private void run() {
      LOGGER.debug("Reading clients initial message to determine what type of client it is.");
      ConnectionType type;
      try {
         type = readType(socket);
         if (type == null)
            return;
      } catch (IOException e) {
         handler.failed(this, e);
         return;
      }

      LOGGER.debug("Initial message from " + socket.getInetAddress() + " received: " + type);

      switch (type) {
      case ADMIN:
         handler.newAdminConnection(this, socket);
         break;
      case CLIENT:
         handler.newDownloadConnection(this, socket);
         break;
      case ALL:
         handler.failed(this, new IOException("Could not establish connection of type " + type));
         break;
      }

   }

   private ConnectionType readType(Socket socket) throws IOException {
      String initMsg;
      try {
         initMsg = scanner.next();
      } catch (IllegalStateException | NoSuchElementException e) {
         return null;
      }
      ConnectionType type = ConnectionType.parse(initMsg);
      if (type != null)
         return type;

      throw new IOException("No valid initial verification message received: " + initMsg);
   }

   @Override
   public void close(long timeout) throws InterruptedException {
      LOGGER.debug("Closing ConnectionRequest from " + socket.getInetAddress());
      destroy();
      if (thread != null && thread != Thread.currentThread())
         thread.join(timeout);
      LOGGER.debug("ConnectionRequest from " + socket.getInetAddress() + " closed.");
   }

   @Override
   public void destroy() {
      if (scanner != null)
         scanner.close();

      if (!socket.isClosed()) {
         try {
            socket.close();
         } catch (IOException e) {
            LOGGER.warn("An error occured while closing socket to " + socket.getInetAddress(), e);
         }
      }
   }

   public Socket getSocket() {
      return socket;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((this.socket == null) ? 0 : this.socket.getInetAddress().hashCode());
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
      if (this.socket == null) {
         if (other.socket != null)
            return false;
      } else if (!this.socket.getInetAddress().equals(other.socket.getInetAddress()))
         return false;
      return true;
   }
}