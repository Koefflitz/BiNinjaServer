package de.dk.bininja.server.net;

import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dk.bininja.net.ConnectionDetails;
import de.dk.bininja.net.ConnectionType;
import de.dk.bininja.server.controller.AdminClientController;
import de.dk.bininja.server.controller.ClientHandler;
import de.dk.bininja.server.controller.ClientManager;
import de.dk.bininja.server.controller.DownloadClientManager;
import de.dk.util.net.Connection;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public class Server implements ConnectionRequestHandler, AdminClientController {
   private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

   private final DownloadClientManager downloadClients = new DownloadClientManager();
   private final ClientManager<AdminClientHandler> adminClients = new ClientManager<>();
   private final Set<ConnectionRequest> requests = new HashSet<>();

   private final ServerController controller;

   public Server(ServerController controller) {
      this.controller = controller;
   }

   public synchronized void newConnection(Socket socket) throws IOException {
      LOGGER.info("Establishing connection to client " + socket.getInetAddress());
      ConnectionRequest request = new ConnectionRequest(socket, this);
      requests.add(request);
      request.establish();
   }

   @Override
   public void newAdminConnection(ConnectionRequest request, Socket socket) {
      LOGGER.debug("Establishing new admin client connection to " + socket.getInetAddress());
      requests.remove(request);
      AdminClientHandler adminClient;
      try {
         adminClient = new AdminClientHandler(socket, this);
         adminClients.add(adminClient);
         LOGGER.debug("New connection to admin client " + socket.getInetAddress() + " established.");
      } catch (IOException e) {
         failed(request, e);
      }
   }

   @Override
   public void newDownloadConnection(ConnectionRequest request, Socket socket) {
      LOGGER.debug("Establishing new download client connection to " + socket.getInetAddress());
      requests.remove(request);
      DownloadClientHandler downloadClient;
      try {
         downloadClient = new DownloadClientHandler(socket);
         downloadClients.add(downloadClient);
         LOGGER.debug("New connection to download client " + socket.getInetAddress() + " established.");
      } catch (IOException e) {
         failed(request, e);
      }
   }

   @Override
   public void failed(ConnectionRequest request, IOException e) {
      requests.remove(request);
      LOGGER.error("Could not establish connection to download client " + request.getSocket().getInetAddress(), e);
      LOGGER.debug("Removing request from " + request.getSocket().getInetAddress());
      request.destroy();
   }

   @Override
   public void setBufferSize(int bufferSize) {
      LOGGER.info("Setting buffer size to " + bufferSize);
      downloadClients.setBufferSize(bufferSize);
   }

   @Override
   public int readBufferSize() {
      return downloadClients.getBufferSize();
   }

   @Override
   public int countAdminConnections() {
      return adminClients.count();
   }

   @Override
   public int countDownloadConnections() {
      return downloadClients.count();
   }

   @Override
   public int countTotalConnections() {
      return adminClients.count() + downloadClients.count();
   }

   @Override
   public Collection<ConnectionDetails> getConnectionDetailsOf(ConnectionType type) {
      switch (type) {
      case ADMIN:
         return buildConnectionDetailsOf(adminClients, type).collect(Collectors.toList());
      case CLIENT:
         return buildConnectionDetailsOf(downloadClients, type).collect(Collectors.toList());
      case ALL:
         return Stream.concat(buildConnectionDetailsOf(adminClients, ConnectionType.ADMIN),
                              buildConnectionDetailsOf(downloadClients, ConnectionType.CLIENT))
                      .collect(Collectors.toList());
      }
      return null;
   }

   private Stream<ConnectionDetails> buildConnectionDetailsOf(ClientManager<? extends ClientHandler> manager, ConnectionType type) {
      return manager.getClients()
                    .stream()
                    .map(ClientHandler::getConnection)
                    .map(c -> getDetailsOf(c, type));
   }

   @Override
   public void shutdown() {
      controller.shutdown();
   }

   private ConnectionDetails getDetailsOf(Connection connection, ConnectionType type) {
      String host = connection.getInetAddress()
                              .getHostName();

      int port = connection.getSocket()
                           .getPort();

      return new ConnectionDetails(host, port, type);
   }

   public synchronized void destroy() throws InterruptedException {
      LOGGER.debug("Destroying all resources");

      InterruptedException e = null;
      for (Resource resource : getResources()) {
         if (e == null) {
            try {
               resource.close(0);
            } catch (InterruptedException ex) {
               e = ex;
            }
         }
         if (e != null)
            resource.destroy();
      }

      if (e != null)
         throw e;
   }

   private Collection<Resource> getResources() {
      return Stream.concat(Stream.concat(adminClients.getClients().stream(),
                                         downloadClients.getClients().stream()),
                           requests.stream())
                   .collect(Collectors.toList());
   }
}
