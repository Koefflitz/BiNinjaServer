package de.dk.bininja.server.entrypoint;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dk.bininja.server.controller.MasterControlProgram;

public class Entrypoint implements DaemonContext {
   private static final Logger LOGGER = LoggerFactory.getLogger(Entrypoint.class);

   private final DaemonController controller;
   private final String[] args;

   private Entrypoint(DaemonController controller, String... args) {
      this.controller = controller;
      this.args = args;
   }

   public static void main(String... args) {
      Daemon daemon = new MasterControlProgram();
      Entrypoint ctx = new Entrypoint(new Controller(daemon), args);
      try {
         daemon.init(ctx);
      } catch (Exception e) {
         LOGGER.error(e.getMessage(), e);
         System.exit(1);
      }
      try {
         daemon.start();
      } catch (Exception e) {
         LOGGER.error(e.getMessage(), e);
         System.exit(1);
      }
   }

   @Override
   public DaemonController getController() {
      return controller;
   }

   @Override
   public String[] getArguments() {
      return args;
   }
}