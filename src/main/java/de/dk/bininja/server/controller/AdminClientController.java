package de.dk.bininja.server.controller;

import java.util.Collection;

import de.dk.bininja.net.ConnectionDetails;
import de.dk.bininja.net.ConnectionType;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public interface AdminClientController {
   public int countAdminConnections();
   public int countDownloadConnections();
   public int countTotalConnections();
   public Collection<ConnectionDetails> getConnectionDetailsOf(ConnectionType type);
   public void setBufferSize(int bufferSize);
   public int readBufferSize();
   public void shutdown();
}
