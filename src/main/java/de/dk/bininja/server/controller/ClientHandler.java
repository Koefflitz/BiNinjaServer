package de.dk.bininja.server.controller;

import de.dk.bininja.server.net.Resource;
import de.dk.util.net.Connection;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public interface ClientHandler extends Resource {
   public Connection getConnection();
}
