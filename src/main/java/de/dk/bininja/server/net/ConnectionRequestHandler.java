package de.dk.bininja.server.net;

import java.io.IOException;
import java.net.Socket;

public interface ConnectionRequestHandler {
   public void newAdminConnection(ConnectionRequest request, Socket socket);
   public void newDownloadConnection(ConnectionRequest request, Socket socket);
   public void failed(ConnectionRequest request, IOException e);
}