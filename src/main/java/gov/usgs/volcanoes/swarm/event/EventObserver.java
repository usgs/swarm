/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.swarm.event;

/**
 * Observer interface for Event objects.
 * 
 * @author Tom Parker
 *
 */
public interface EventObserver {
  public void eventUpdated();
}
