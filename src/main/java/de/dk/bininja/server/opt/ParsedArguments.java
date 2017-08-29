package de.dk.bininja.server.opt;

import java.io.IOException;

import de.dk.bininja.InvalidArgumentException;
import de.dk.bininja.opt.ParsedSecurityArguments;
import de.dk.util.opt.ArgumentModel;
import de.dk.util.opt.ArgumentParser;
import de.dk.util.opt.ArgumentParserBuilder;
import de.dk.util.opt.ex.ArgumentParseException;

public class ParsedArguments {
   private ParsedSecurityArguments securityArgs;
   private int port = -1;

   public ParsedArguments() {

   }

   public static ParsedArguments parse(String... args) throws ArgumentParseException {
      ArgumentParserBuilder builder = ArgumentParserBuilder.begin();
      Option.PORT.build(builder);
      ParsedSecurityArguments.build(builder);

      ArgumentParser parser = builder.buildAndGet();
      ArgumentModel result = parser.parseArguments(args);

      ParsedArguments parsedArgs = new ParsedArguments();
      if (result.isOptionPresent(Option.PORT.getKey())) {
         String portString = result.getOptionValue(Option.PORT.getKey());
         int port;
         try {
            port = Integer.parseInt(portString);
         } catch (NumberFormatException e) {
            throw new InvalidArgumentException("Invalid port: " + portString, e);
         }
         parsedArgs.setPort(port);
      }

      if (result.isCommandPresent(ParsedSecurityArguments.NAME)) {
         ArgumentModel securityResult = result.getCommandValue(ParsedSecurityArguments.NAME);
         try {
            ParsedSecurityArguments secArgs = ParsedSecurityArguments.parse(securityResult);
            parsedArgs.setSecurityArgs(secArgs);
         } catch (IOException e) {
            throw new ArgumentParseException("Could not parse securityArgs", e);
         }
      }
      return parsedArgs;
   }

   public ParsedSecurityArguments getSecurityArgs() {
      return securityArgs;
   }

   public void setSecurityArgs(ParsedSecurityArguments securityArgs) {
      this.securityArgs = securityArgs;
   }

   public boolean isPortSet() {
      return port != -1;
   }

   public int getPort() {
      return port;
   }

   public void setPort(int port) {
      this.port = port;
   }

}
