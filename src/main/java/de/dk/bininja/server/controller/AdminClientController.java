package de.dk.bininja.server.controller;

public interface AdminClientController {
   public int countAdminConnections();
   public int countDownloadConnections();
   public int countTotalConnections();
   public void setBufferSize(int bufferSize);
   public int readBufferSize();
   public void shutdown();
}