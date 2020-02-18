/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.swarm.event;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Translate FDSN network codes into organizational names.
 * 
 * @author Tom Parker
 *
 */
public class Networks {
  private static final Logger LOGGER = LoggerFactory.getLogger(Networks.class);

  private static final String NETWORKS_FILE = "networks.csv";
  private final Map<String, String> networks;

  public String getName(String code) {
    return networks.get(code);
  }

  private Networks() {
    networks = new HashMap<String, String>();
    Reader reader = null;
    try {
      reader = new InputStreamReader(ClassLoader.getSystemResource(NETWORKS_FILE).openStream());

      try {
        List<String> fields = Networks.parseLine(reader);
        while (fields != null) {
          networks.put(fields.get(0), fields.get(2));
          fields = Networks.parseLine(reader);
        }
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    } catch (IOException e) {
      LOGGER.info("Unable to read networks", e);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException ignored) {
        }
      }
    }

  }

  public static Networks getInstance() {
    return NetworksHolder.networks;
  }

  private static class NetworksHolder {
    public static Networks networks = new Networks();
  }

  /**
   * Adapted from https://agiletribe.wordpress.com/2012/11/23/the-only-class-you-need-for-csv-files/
   * 
   * @param r
   * @return
   * @throws Exception
   */
  public static List<String> parseLine(Reader r) throws Exception {
    int ch = r.read();
    while (ch == '\r') {
      // ignore linefeed chars wherever, particularly just before end of file
      ch = r.read();
    }
    if (ch < 0) {
      return null;
    }
    Vector<String> store = new Vector<String>();
    StringBuffer curVal = new StringBuffer();
    boolean inquotes = false;
    boolean started = false;
    while (ch >= 0) {
      if (inquotes) {
        started = true;
        if (ch == '\"') {
          inquotes = false;
        } else {
          curVal.append((char) ch);
        }
      } else {
        if (ch == '\"') {
          inquotes = true;
          if (started) {
            // if this is the second quote in a value, add a quote
            // this is for the double quote in the middle of a value
            curVal.append('\"');
          }
        } else if (ch == ',') {
          store.add(curVal.toString());
          curVal = new StringBuffer();
          started = false;
        } else if (ch == '\r') {
          // ignore LF characters
        } else if (ch == '\n') {
          // end of a line, break out
          break;
        } else {
          curVal.append((char) ch);
        }
      }
      ch = r.read();
    }
    store.add(curVal.toString());
    return store;
  }
}
