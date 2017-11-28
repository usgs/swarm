/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0
 * Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.swarm.data.fdsnWs;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.iris.dmc.seedcodec.CodecException;
import edu.iris.dmc.seedcodec.UnsupportedCompressionType;
import edu.sc.seis.seisFile.mseed.Blockette;
import edu.sc.seis.seisFile.mseed.Blockette1000;
import edu.sc.seis.seisFile.mseed.DataRecord;
import gov.usgs.volcanoes.core.data.Wave;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.ChannelInfo;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;
import gov.usgs.volcanoes.swarm.data.seedLink.SeedUtils;

public abstract class AbstractDataRecordClient {
  private final SeismicDataSource source;

  /**
   * Create the abstract data record client.
   * 
   * @param source the Seismic Data source.
   */
  public AbstractDataRecordClient(SeismicDataSource source) {
    this.source = source;
  }

  /**
   * Add the waves.
   * 
   * @param waves the list of waves.
   * @param dr the data record.
   * @return the list of waves.
   * @throws UnsupportedCompressionType
   * @throws CodecException
   */
  public static List<Wave> addWaves(final List<Wave> waves, final DataRecord dr)
      throws UnsupportedCompressionType, CodecException {
    for (Blockette blockette : dr.getBlockettes(1000)) {
      if (blockette instanceof Blockette1000) {
        waves.add(SeedUtils.createWave(dr, (Blockette1000) blockette));
      }
    }
    return waves;
  }

  /**
   * Assign the channels.
   * 
   * @param channels the list of channels.
   */
  public void assignChannels(List<String> channels) {
    WebServiceUtils.assignChannels(channels, source);
  }

  /**
   * Create an empty list of waves.
   * 
   * @return the list of waves.
   */
  public static List<Wave> createWaves() {
    return new ArrayList<Wave>();
  }

  /**
   * Get the channel information.
   * 
   * @return the list of channels.
   */
  public abstract List<String> getChannels();

  /**
   * Get the date for the specified time.
   * 
   * @param t the time.
   * @return the date.
   */
  protected static Date getDate(double t) {
    return J2kSec.asDate(t);
  }

  /**
   * Get the text for the specified date.
   * 
   * @param date the date.
   * @return the text.
   */
  protected static String getDateText(Date date) {
    return WebServiceUtils.getDateText(date);
  }

  /**
   * Get the text for the specified time.
   * 
   * @param date the date.
   * @return the text.
   */
  protected static String getDateText(double t) {
    return getDateText(getDate(t));
  }

  /**
   * Get the raw data.
   * 
   * @param channelInfo the channel information.
   * @param t1 the start time.
   * @param t2 the end time.
   * @return the raw data.
   */
  public abstract Wave getRawData(final ChannelInfo channelInfo, final double t1, final double t2);

  /**
   * Get the seismic data source.
   * 
   * @return the source.
   */
  public SeismicDataSource getSource() {
    return source;
  }

  /**
   * Joins together a list of waves into one large wave.
   * 
   * @param waves the list of <code>Wave</code> s
   * @return the new joined wave
   */
  public static Wave join(List<Wave> waves) {
    // TODO ensure index is good and no gaps?
    return Wave.join(waves);
  }
}
