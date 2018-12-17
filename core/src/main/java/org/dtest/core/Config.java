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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class creates a static global configuration object.  It contains two maps, both of which map keys (strings)
 * to values (objects).  The first map maps keys to values, the second keys to default values.  The first map is always
 * checked first, and the second only used if there is no entry for the key in the first.
 *
 * The class provides a set of getter methods that allow the user to fetch that value as string, int,
 * time value, etc.
 *
 * Keys for the configuration values are defined in the classes that read those keys.  While other classes can set
 * values for those keys, no other classes should read them.  This is because until those classes are invoked the
 * default values will not be set up.
 */
public class Config {

  private static final Map<String, String> entries = new HashMap<>();
  private static final Map<String, String> defaultEntries = new HashMap<>();
  private static final Pattern TIME_UNIT_SUFFIX = Pattern.compile("([0-9]+)([a-zA-Z]+)");

  private Config() {

  }

  @VisibleForTesting
  static final String CONF_DIR = "conf";
  @VisibleForTesting
  static final String PROPERTIES_FILE = "dtest.properties";

  public static <T> Class<? extends T> getAsClass(String key, Class<T> clazz) throws IOException {
    String val = find(key);
    return val == null ? null : Utils.getClass(val, clazz);
  }

  public static int getAsInt(String key) {
    String val = find(key);
    return val == null ? 0 : Integer.valueOf(val);
  }

  /**
   * Get the value as a time.
   * @param key key to look up
   * @param outUnit time unit to return this as
   * @return time as a long
   */
  public static long getAsTime(String key, TimeUnit outUnit) {
    String val = find(key);
    if (val == null) return 0;
    Matcher m = TIME_UNIT_SUFFIX.matcher(val);
    if (m.matches()) {
      long duration = Long.parseLong(m.group(1));
      String unit = m.group(2).toLowerCase();

      // If/else chain arranged in likely order of frequency for performance
      if (unit.equals("s") || unit.startsWith("sec")) {
        return outUnit.convert(duration, TimeUnit.SECONDS);
      } else if (unit.equals("ms") || unit.equals("u") || unit.startsWith("msec")) {
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

  public static String getAsString(String key) throws IOException {
    return find(key);
  }

  public static void set(String key, String newVal) {
    entries.put(key, newVal);
  }

  public static void setDefaultValue(String key, String defaultVal) {
    defaultEntries.put(key, defaultVal);
  }

  /**
   * Read the configuration file and set the system properties based on values in the file.  This
   * method expects the configuration file to be in $DTEST_HOME/conf/dtest.properties.
   * @throws IOException If the file cannot be found or is not readable or is not the proper format.
   */
  public static void fromConfigFile() throws IOException {
    String dtestHome = System.getenv(DockerTest.DTEST_HOME);
    if (dtestHome == null || dtestHome.isEmpty()) {
      throw new IOException("Unable to find configuration file, please set DTEST_HOME");
    }
    String filename = dtestHome + File.separator + CONF_DIR + File.separator + PROPERTIES_FILE;
    FileInputStream input = new FileInputStream(filename);
    Properties p = new Properties();
    p.load(input);
    for (Object key : p.keySet()) {
      // Only set the value if it doesn't override an existing value
      entries.putIfAbsent(key.toString(), p.getProperty(key.toString()));
    }
    input.close();
  }

  private static String find(String key) {
    String entry = entries.get(key);
    if (entry == null) {
      entry = defaultEntries.get(key);
    }
    return entry;
  }

}
