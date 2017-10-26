package de.dk.bininja.server.net;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dk.bininja.net.Base64Connection;
import de.dk.bininja.net.ConnectionMetadata;
import de.dk.bininja.net.ConnectionType;
import de.dk.bininja.net.packet.ConnectionAnswerPacket;
import de.dk.bininja.server.controller.AdminClientController;
import de.dk.bininja.server.controller.ClientHandler;
import de.dk.bininja.server.controller.ClientManager;
import de.dk.bininja.server.controller.DownloadClientManager;
import de.dk.util.net.security.SessionKeyArrangement;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public class Server implements ConnectionRequestHandler, AdminClientController {
   private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

   private final ServerController controller;
   private final KeyPair keys;

   private final DownloadClientManager downloadClients = new DownloadClientManager();
   private final ClientManager<AdminClientHandler> adminClients = new ClientManager<>();
   private final Set<ConnectionRequest> requests = new HashSet<>();

   public Server(ServerController controller, KeyPair keys) {
      this.controller = Objects.requireNonNull(controller);
      this.keys = keys;
   }

   public synchronized void newConnection(Socket socket) throws IOException {
      LOGGER.info("Establishing connection to client " + socket.getInetAddress());
      ConnectionRequest request = new ConnectionRequest(new Base64Connection(socket), this);
      requests.add(request);
      request.establish();
   }

   @Override
   public void newAdminConnection(ConnectionRequest request, Base64Connection connection) {
      LOGGER.debug("Establishing new admin client connection to " + connection.getAddress());
      requests.remove(request);
      AdminClientHandler adminClient;
      try {
         adminClient = new AdminClientHandler(connection, this, request.isSecure());
         adminClients.add(adminClient);
         adminClient.getConnection().send(new ConnectionAnswerPacket(true));
         LOGGER.debug("New connection to admin client " + connection.getAddress() + " established.");
      } catch (IOException e) {
         failed(request, e);
      }
   }

   @Override
   public void newDownloadConnection(ConnectionRequest request, Base64Connection connection) {
      LOGGER.debug("Establishing new download client connection to " + connection.getAddress());
      requests.remove(request);

      try {
         DownloadClientHandler downloadClient = new DownloadClientHandler(connection, request.isSecure());
         downloadClient.getConnection()
                       .send(new ConnectionAnswerPacket(true));

         downloadClients.add(downloadClient);
         LOGGER.debug("New connection to download client " + connection.getAddress() + " established.");
      } catch (IOException e) {
         failed(request, e);
      }
   }

   @Override
   public SecretKey buildSecureCoder(SessionKeyArrangement builder) throws IOException {
      if (keys == null)
         throw new IOException("Secure connections not supported.");

      return builder.setGenerateSessionKey(false)
                    .setPublicKey(keys.getPublic())
                    .setPrivateKey(keys.getPrivate())
                    .arrange();
   }

   @Override
   public void failed(ConnectionRequest request, IOException e) {
      String target = request.getConnection()
                             .getAddress()
                             .toString();

      LOGGER.error("Could not establish connection to client " + target, e);
      try {
         request.getConnection()
                .send(new ConnectionAnswerPacket(false, e.getMessage()));
      } catch (IOException ex) {
         LOGGER.warn("Could not send connection denial to " + target);
      }
      LOGGER.debug("Destroying connection request from " + target);
      requests.remove(request);
      try {
         request.destroy(0);
      } catch (IOException | InterruptedException ex) {
         LOGGER.warn("Error destroying the connection request from "
                     + request.getConnection().getAddress(),
                     ex);
      }
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
   public Collection<ConnectionMetadata> getConnectionMetadataOf(ConnectionType type) {
      switch (type) {
      case ADMIN:
         return adminClients.getClients()
                            .stream()
                            .map(ClientHandler::getMetadata)
                            .collect(Collectors.toList());
      case CLIENT:
         return downloadClients.getClients()
                               .stream()
                               .map(ClientHandler::getMetadata)
                               .collect(Collectors.toList());
      case ALL:
         Stream<ClientHandler> s = Stream.concat(adminClients.getClients().stream(),
                                                 downloadClients.getClients().stream());
         return s.map(ClientHandler::getMetadata)
                 .collect(Collectors.toList());
      }
      return null;
   }

   @Override
   public void shutdown() {
      controller.shutdown();
   }

   public synchronized void destroy() throws InterruptedException, IOException {
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
            resource.destroy(0);
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
