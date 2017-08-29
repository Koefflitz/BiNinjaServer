package de.dk.bininja.server.opt;

import java.util.Objects;

import de.dk.util.opt.ArgumentParserBuilder;
import de.dk.util.opt.ExpectedOption;
import de.dk.util.opt.OptionBuilder;

public enum Option {
   PORT('p', "port", "port", "The port to look for connections.", true, false);

   private final char key;
   private final String longKey;
   private final String name;
   private final String description;

   private final boolean expectsValue;
   private final boolean mandatory;

   private Option(char key, String longKey, String name, String description, boolean expectsValue, boolean mandatory) {
      this.key = key;
      this.longKey = longKey;
      this.name = Objects.requireNonNull(name);
      this.description = Objects.requireNonNull(description);
      this.expectsValue = expectsValue;
      this.mandatory = mandatory;
   }

   private Option(char key, String name, String description) {
      this(key, null, name, description, false, false);
   }

   private Option(char key, String name, String description, boolean expectsValue) {
      this(key, null, name, description, expectsValue, false);
   }

   private Option(char key, String name, String description, boolean expectsValue, boolean mandatory) {
      this(key, null, name, description, expectsValue, mandatory);
   }

   private Option(String longKey, String name, String description) {
      this(ExpectedOption.NO_KEY, longKey, name, description, false, false);
   }

   private Option(String longKey, String name, String description, boolean expectsValue) {
      this(ExpectedOption.NO_KEY, longKey, name, description, expectsValue, false);
   }

   private Option(String longKey, String name, String description, boolean expectsValue, boolean mandatory) {
      this(ExpectedOption.NO_KEY, longKey, name, description, expectsValue, mandatory);
   }

   public ArgumentParserBuilder build(ArgumentParserBuilder builder) {
      OptionBuilder oBuilder;
      if (key != ExpectedOption.NO_KEY) {
         oBuilder = builder.buildOption(key, name)
                           .setLongKey(longKey);
      } else {
         oBuilder = builder.buildOption(longKey, name);
      }

      return oBuilder.setDescription(description)
                     .setMandatory(mandatory)
                     .setExpectsValue(expectsValue)
                     .build();
   }

   public char getKey() {
      return key;
   }
}
