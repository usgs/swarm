package gov.usgs.volcanoes.swarm.exp;

/**
 * @author Peter Cervelli (USGS)
 */
@Deprecated
public class SeedDataSource {}
/*
public class SeedDataSource extends SeismicDataSource
{
	private List stations;
	private static HashMap stationMap; 

	private static boolean read = false;
	
	public SeedDataSource(String fn)
	{
		if (read)
			return;
		stations = new ArrayList();
		try
		{
			stationMap = new HashMap();
			
			DataInputStream ls = new DataInputStream(new BufferedInputStream(
	                new FileInputStream(fn)));
			byte[] buf = new byte[0xe000];
			ls.read(buf, 0, 0xe000);
			MiniSeedRead msr = new MiniSeedRead(ls);
			SeedRecord sr;
			
			while ((sr = msr.getNextRecord()) != null)
            {
                Codec codec = new Codec();
                if (sr instanceof DataRecord) 
                {
                    DataRecord dr = (DataRecord)sr;
                    DataHeader dh = dr.getHeader();
                    String code = dh.getStationIdentifier().trim() + "_" + dh.getChannelIdentifier().trim() + "_"
							+ dh.getNetworkCode().trim();
                    List parts = (List)stationMap.get(code);
                    if (parts == null)
                    {
                    	parts = new ArrayList();
                    	stationMap.put(code, parts);
                    }
                    SampledWave sw = new SampledWave();
                    sw.setSamplingRate(dh.getSampleRate());
                    sw.setStartTime(Util.dateToJ2K(dh.getStartBtime().toDate()));
                    byte[] data = dr.getData();
                    Blockette[] blockettes = dr.getBlockettes(1000);
                    Blockette1000 b1000 = (Blockette1000)blockettes[0];
                    boolean swapNeeded = false;
                    if (b1000.getWordOrder() == 0) 
                    	swapNeeded = true;
                    try 
					{
                        if ((int)b1000.getEncodingFormat() == 0) 
                        {
                        	System.out.println("not compressed");
                        	// not compressed ?
//                            String s = new String(data);
//                            System.out.println(s);
                        }
                        else
                        {
                            DecompressedData decomp = codec.decompress((int)b1000.getEncodingFormat(),
                                                                       data,
                                                                       dr.getHeader().getNumSamples(),
                                                                       swapNeeded);
//	                            int[] outData = decomp.getAsInt();
                            sw.buffer = decomp.getAsInt();
                            sw.register();
                            parts.add(sw);
                        }
					}
                    catch (UnsupportedCompressionType ex) 
					{
                        System.out.println("compression type not supported"+b1000.getEncodingFormat());
                    }
                }
            }
			
			for (Iterator it = stationMap.keySet().iterator(); it.hasNext(); )
			{
				String key = (String)it.next();
				List parts = (List)stationMap.get(key);
				SampledWave wave = SampledWave.join(parts);
				System.out.println(wave.mean() + " " + Util.j2KToDateString(wave.getStartTime()));
				Swarm.getCache().cacheWaveAsHelicorder(key, wave);
				Swarm.getCache().putWave(key, wave);
				stationMap.put(key, wave);
				stations.add(key);
			}
			read = true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public HelicorderData getHelicorder(String station, double t1, double t2)
	{
		double dt = t2 - t1;
		double now = CurrentTime.nowJ2K();
		if (Math.abs(now - t2) < 3600)
		{
			SampledWave wave = (SampledWave)stationMap.get(station);
			if (wave == null)
				return null;
			
			t2 = wave.getEndTime();
			t1 = t2 - dt;
		}
		return Swarm.getCache().getHelicorder(station, t1, t2);
	}
	
	public List getHelicorderStations()
	{
		return stations;
	}

	public SampledWave getSampledWave(String station, double t1, double t2) 
	{
		return Swarm.getCache().getSampledWave(station, t1, t2);
	}
	
	public List getWaveStations() 
	{
		return stations;
	}
}
*/