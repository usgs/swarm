package gov.usgs.swarm.data.seedLink;

import edu.iris.dmc.seedcodec.Codec;
import edu.iris.dmc.seedcodec.CodecException;
import edu.iris.dmc.seedcodec.DecompressedData;
import edu.iris.dmc.seedcodec.UnsupportedCompressionType;
import edu.sc.seis.seisFile.mseed.Blockette1000;
import edu.sc.seis.seisFile.mseed.Btime;
import edu.sc.seis.seisFile.mseed.DataHeader;
import edu.sc.seis.seisFile.mseed.DataRecord;
import gov.usgs.plot.data.Wave;
import gov.usgs.util.Util;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class SeedUtils
{
	public static Date btimeToDate(Btime btime)
	{
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.set(Calendar.YEAR, btime.getYear());
		cal.set(Calendar.DAY_OF_YEAR, btime.getDayOfYear());
		cal.set(Calendar.HOUR_OF_DAY, btime.getHour());
		cal.set(Calendar.MINUTE, btime.getMin());
		cal.set(Calendar.SECOND, btime.getSec());
		cal.set(Calendar.MILLISECOND, btime.getTenthMilli() / 10);
		return cal.getTime();
	}

	public static double btimeToJ2K(Btime btime)
	{
		Date date = btimeToDate(btime);
		double j2k = Util.dateToJ2K(date);
		return j2k;
	}

	public static Wave createWave(DataRecord dr, Blockette1000 b1000)
			throws UnsupportedCompressionType, CodecException
	{
		final DataHeader dh = dr.getHeader();
		final int type = b1000.getEncodingFormat();
		final byte[] data = dr.getData();
		final boolean swapNeeded = b1000.getWordOrder() == 0;
		final Codec codec = new Codec();
		final DecompressedData decomp = codec.decompress(type, data, dr
				.getHeader().getNumSamples(), swapNeeded);
		final Wave wave = new Wave();
		wave.setSamplingRate(dh.getSampleRate());
		wave.setStartTime(SeedUtils.getStartTime(dh));
		wave.buffer = decomp.getAsInt();
		wave.register();
		return wave;
	}

	public static double getStartTime(DataHeader dh)
	{
		Btime btime = dh.getStartBtime();
		double j2k = btimeToJ2K(btime);
		return j2k;
	}
}
