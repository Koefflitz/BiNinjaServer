package de.dk.bininja.server.net;

import java.io.IOException;

import javax.crypto.SecretKey;

import de.dk.bininja.net.Base64Connection;
import de.dk.util.net.security.SessionKeyArrangement;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public interface ConnectionRequestHandler {
   void newAdminConnection(ConnectionRequest request, Base64Connection connection);
   void newDownloadConnection(ConnectionRequest request, Base64Connection connection);
   SecretKey buildSecureCoder(SessionKeyArrangement builder) throws IOException;
   void failed(ConnectionRequest request, IOException e);
}
