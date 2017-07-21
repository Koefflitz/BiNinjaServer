package de.dk.bininja.server.controller;

import de.dk.bininja.server.net.Resource;
import de.dk.util.net.Connection;

public interface ClientHandler extends Resource {
   public Connection getConnection();
}