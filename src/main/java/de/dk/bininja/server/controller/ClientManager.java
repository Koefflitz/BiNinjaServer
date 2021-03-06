package de.dk.bininja.server.controller;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dk.util.net.Connection;
import de.dk.util.net.ConnectionListener;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public class ClientManager<C extends ClientHandler> implements Iterable<C> {
   private static final Logger LOGGER = LoggerFactory.getLogger(ClientManager.class);

   protected Collection<C> clients = new LinkedList<>();

   public ClientManager() {

   }

   public void add(C client) {
      clients.add(client);
      client.getConnection()
            .addListener(new ConnectionListenerAdapter(client));
   }

   public int count() {
      return clients.size();
   }

   public Collection<C> getClients() {
      return clients;
   }

   @Override
   public Iterator<C> iterator() {
      return clients.iterator();
   }

   private class ConnectionListenerAdapter implements ConnectionListener {
      private C client;

      public ConnectionListenerAdapter(C client) {
         this.client = client;
      }

      @Override
      public void closed(Connection connection) {
         clients.remove(client);
         LOGGER.info("Client disconnected. " + clients.size() + " client connections remaining");
      }
   }
}
