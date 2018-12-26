/*
 * Copyright (C) 2018 Hortonworks Inc.
 *
 * Licenced under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dtest.core;

import com.google.common.annotations.VisibleForTesting;
import org.dtest.core.impl.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class forms a wrapper around properties, giving callers a way to get the values as appropriate types.
 * No support for default values is provided other than allowing users to pass in a default value that will
 * be returned if the requested key is not in the properties.
 *
 * When the config object is constructed a properties object can be passed in.  If none is, the System properties
 * will be used instead.
 *
 * Properties can also be read from a file.  Any properties already set in the passed in properties object will
 * not be overridden by the values in the file.
 *
 * The intent of this bare bones config system is to build something that plugins can easily tap into.  The
 * envisioned usage is that each component defines its own config values and only that component reads them.
 */
public class Config {

  public static final String PROPERTIES_FILE = "dtest.properties";
  public static final String YAML_FILE = "dtest.yaml";
  private static final Pattern TIME_UNIT_SUFFIX = Pattern.compile("([0-9]+)([a-zA-Z]+)");

  private final Properties entries;

  /**
   * Build a configuration object using the System properties.
   */
  public Config() {
    this(System.getProperties());

  }

  /**
   * Build a configuration object using the specified properties
   * @param props properties
   */
  public Config(Properties props) {
    entries = (Properties)props.clone();
  }

  /**
   * Build a configuration object by reading a config file.  Any values passed in props will override values read
   * in the file.
   * @param confDir configuration directory for the file.  The file should be named dtest.properties
   * @param props Properties that will override anything found in the file
   * @throws IOException if the file cannot be read
   */
  public Config(String confDir, Properties props) throws IOException {
    String filename = confDir + File.separator + PROPERTIES_FILE;
    FileInputStream input = new FileInputStream(filename);
    Properties p = new Properties();
    p.load(input);
    entries = new Properties(p); // Set the file values as defaults for our properties
    // Now copy in our passed in properties
    for (String key : props.stringPropertyNames()) entries.setProperty(key, props.getProperty(key));
    input.close();
  }

  public <T> Class<? extends T> getAsClass(String key, Class<T> clazz, Class<? extends T> defaultVal) throws IOException {
    String val = entries.getProperty(key);
    return val == null ? defaultVal : Utils.getClass(val, clazz);
  }

  public int getAsInt(String key, int defaultVal) {
    String val = entries.getProperty(key);
    return val == null ? defaultVal : Integer.valueOf(val);
  }

  public int getAsInt(String key) {
    return getAsInt(key, 0);
  }

  /**
   * Get the value as a time.
   * @param key key to look up
   * @param outUnit time unit to return this as
   * @param defaultVal default time value
   * @return time as a long, or defaultVal if the key is not present
   */
  public long getAsTime(String key, TimeUnit outUnit, long defaultVal) {
    String val = entries.getProperty(key);
    if (val == null) return defaultVal;
    Matcher m = TIME_UNIT_SUFFIX.matcher(val);
    if (m.matches()) {
      long duration = Long.parseLong(m.group(1));
      String unit = m.group(2).toLowerCase();

      // If/else chain arranged in likely order of frequency for performance
      if (unit.equals("s") || unit.startsWith("sec")) {
        return outUnit.convert(duration, TimeUnit.SECONDS);
      } else if (unit.equals("ms") || unit.startsWith("msec")) {
        return outUnit.convert(duration, TimeUnit.MILLISECONDS);
      } else if (unit.equals("m") || unit.startsWith("min")) {
        return outUnit.convert(duration, TimeUnit.MINUTES);
      } else if (unit.equals("us") || unit.startsWith("usec")) {
        return outUnit.convert(duration, TimeUnit.MICROSECONDS);
      } else if (unit.equals("ns") || unit.startsWith("nsec")) {
        return outUnit.convert(duration, TimeUnit.NANOSECONDS);
      } else if (unit.equals("h") || unit.startsWith("hour")) {
        return outUnit.convert(duration, TimeUnit.HOURS);
      } else if (unit.equals("d") || unit.startsWith("day")) {
        return outUnit.convert(duration, TimeUnit.DAYS);
      } else {
        throw new IllegalArgumentException("Invalid time unit " + unit);
      }
    } else {
      throw new IllegalArgumentException("Invalid time unit " + val);
    }
  }

  /**
   * Get the value as a time.
   * @param key key to look up
   * @param outUnit time unit to return this as
   * @return time as a long, or 0 if key not present
   */
  public long getAsTime(String key, TimeUnit outUnit) {
    return getAsTime(key, outUnit, 0);
  }

  /**
   * Get the entry as a string, or the provided default value if the entry isn't set.
   * @param key key
   * @param defaultVal default value
   * @return value or default value if entry is not set.
   */
  public String getAsString(String key, String defaultVal) {
    return entries.getProperty(key, defaultVal);
  }

  /**
   * Get the entry as a string
   * @param key key
   * @return value, or null if no value
   */
  public String getAsString(String key) {
    return entries.getProperty(key);
  }


}
