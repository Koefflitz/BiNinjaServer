package de.dk.bininja.server.controller;

import de.dk.bininja.net.Base64Connection;
import de.dk.bininja.server.net.DownloadClientHandler;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public class DownloadClientManager extends ClientManager<DownloadClientHandler> {
   private int bufferSize = Base64Connection.DEFAULT_BUFFER_SIZE;

   public DownloadClientManager(int bufferSize) {
      this.bufferSize = bufferSize;
   }

   public DownloadClientManager() {
      this(Base64Connection.DEFAULT_BUFFER_SIZE);
   }

   @Override
   public void add(DownloadClientHandler client) {
      super.add(client);
      client.getConnection()
            .setBufferSize(bufferSize);
   }

   public int getBufferSize() {
      return bufferSize;
   }

   public void setBufferSize(int bufferSize) {
      this.bufferSize = bufferSize;
      for (DownloadClientHandler client : clients) {
         client.getConnection()
               .setBufferSize(bufferSize);
      }
   }
}
