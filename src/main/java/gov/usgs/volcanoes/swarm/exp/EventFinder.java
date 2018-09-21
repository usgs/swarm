package gov.usgs.volcanoes.swarm.exp;

/*
import java.util.*;
import java.text.*;
import javax.swing.*;
import gov.usgs.wdc.*;
import gov.usgs.util.*;
import gov.usgs.valve.data.wave.*;
import gov.usgs.valve.math.*;
import gov.usgs.valve.plot.*;
*/
@Deprecated
public class EventFinder
{
	/*
	public static void filter(SampledWave sw)
	{
		double[] dBuf = new double[sw.buffer.length];
		for (int i = 0; i < dBuf.length; i++)
			dBuf[i] = sw.buffer[i];

		Butterworth bw = new Butterworth(Butterworth.BANDPASS, 2, 100, 0.5, 6);
		Filter.filter(dBuf, bw.getSize(), bw.getXCoeffs(), bw.getYCoeffs(), bw.getGain(), 0.0d, 0.0d);
		
		double[] dBuf2 = new double[dBuf.length];
		for (int i = 0, j = dBuf.length - 1; i < dBuf.length; i++, j--)
			dBuf2[j] = dBuf[i];	
		
		Filter.filter(dBuf2, bw.getSize(), bw.getXCoeffs(), bw.getYCoeffs(), bw.getGain(), 0.0d, 0.0d);
		for (int i = 0, j = dBuf2.length - 1; i < sw.buffer.length; i++, j--)
			sw.buffer[i] = (int)Math.round(dBuf2[j]);
	}
	
	public static void main(String[] args)
	{
		double bgst = 1.452258E8;
		double bget = 1.452264E8;
		
		double testst = 1.45143E8;
		double testet = testst + 30 * 60;
		
		WaveDataCenter.dbDriver = "org.gjt.mm.mysql.Driver";
		WaveDataCenter.dbURL = "jdbc:mysql://avo-valve/waves?user=wdcuser&password=wdcuser&autoReconnect=true";

		WaveDataCenter wdc = WaveDataCenter.getStaticWDC();
		Wave w = wdc.getWave("CRP_SHZ_AK", bgst, bget);
		SampledWave sw = w.toSampledWave(100);
		
		WaveViewSettings s1 = new WaveViewSettings();
		s1.type = WaveViewSettings.SPECTRA;
		s1.filter.set(Butterworth.BANDPASS, 2, 100, 0.5, 6);
		s1.filterOn = true;
		
		WaveViewPanel wvp1 = new WaveViewPanel(s1);
		wvp1.setSize(1000, 250);
		wvp1.setSampledWave(sw, bgst, bget);
		
		JFrame f1 = new JFrame("Background Level [0.25...6 Hz Filter]");
		f1.setSize(1020, 270);
		f1.setLocation(0, 0);
		f1.setContentPane(wvp1);
		f1.setVisible(true);
		
		SampledWave fsw = null;
		while (fsw == null)
		{
			try { Thread.sleep(100); } catch (Exception e) {}
			fsw = wvp1.getRenderedWave(); // gets filtered wave
		}
		
		double[][] data = fsw.fft();
		data = FFT.halve(data);
		FFT.toPowerFreq(data, fsw.getSamplingRate(), false, false);
		
		System.out.println("Background size: " + data.length);
		double bgarea = 0;
		for (int i = 0; i < data.length - 1; i++)
			bgarea += data[i][1] * (data[i + 1][0] - data[i][0]);
			
		System.out.println("Background area: " + bgarea);
		
		
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		double ct = testst;
		double testperiod = 5;
		double[][] events = new double[(int)((testet - testst) / testperiod)][2];
		int j = 0;
		while (ct + testperiod < testet)
		{
			System.out.print(df.format(Util.j2KToDate(ct)) + ": ");
			Wave w2 = wdc.getWave("CRP_SHZ_AK", ct, ct + testperiod);
			SampledWave sw2 = w2.toSampledWave(100);
			filter(sw2);
			
			double[][] data2 = sw2.fft();
			data2 = FFT.halve(data2);
			FFT.toPowerFreq(data2, sw2.getSamplingRate(), false, false);
			
			double area = 0;
			double f;
			for (int i = 0; i < data2.length - 1; i++)
			{
				f = data2[i][1] * (data2[i + 1][0] - data2[i][0]);
				area += f*f;
			}
			events[j][0] = ct;
			events[j][1] = area;
				
			System.out.println(area + " " + (bgarea - area));
			ct += testperiod;	
			j++;
		}
		
		WaveViewSettings s2 = new WaveViewSettings();
		
		WaveViewPanel wvp2 = new WaveViewPanel(s2);
		wvp2.setSize(1500, 250);
		Wave w2 = wdc.getWave("CRP_SHZ_AK", testst, testet);
		SampledWave sw2 = w2.toSampledWave(100);
		wvp2.setSampledWave(sw2, testst, testet);
		wvp2.setEvents(events, 200000);
		
		JFrame f2 = new JFrame("Detected Events");
		f2.setSize(1520, 270);
		f2.setLocation(0, 300);
		f2.setContentPane(wvp2);
		f2.setVisible(true);
		
	}
	
	private void findEvents()
	{
		double testperiod = 5;
		double ct = startTime - (startTime % testperiod);
		
		eventThreshold = 4000000;
		events = new double[(int)((endTime - ct) / testperiod) + 2][2];
		int j = 0;
		while (ct < endTime)
		{
			//System.out.print(df.format(Util.j2KToDate(ct)) + ": ");
			//Wave w2 = wdc.getWave("CRP_SHZ_AK", ct, ct + testperiod);
			SampledWave sw2 = source.getSampledWave(channel, ct, ct + testperiod);
			if (sw2 != null)
			{
				filter(sw2);
				
				double[][] data2 = sw2.fft();
				data2 = FFT.halve(data2);
				FFT.toPowerFreq(data2, sw2.getSamplingRate(), false, false);
				
				double area = 0;
				double f;
				for (int i = 0; i < data2.length - 1; i++)
				{
					f = data2[i][1] * (data2[i + 1][0] - data2[i][0]);
					area += f*f;
				}
				events[j][0] = ct;
				events[j][1] = area;
			}
				//System.out.println(area + " " + (bgarea - area));
			ct += testperiod;	
			j++;
		}	
	}
	
	private double[][] events;
	private double eventThreshold;
	
	public void setEvents(double[][] e, double et)
	{
		events = e;
		eventThreshold = et;
	}
	
					/*
				// This code highlights events, used for automatic event finding
				if (events != null)
				{
					for (int i = 0; i < events.length; i++)
					{
						if (events[i][1] >= eventThreshold)
						{
							int x1 = (int)getXPixel(events[i][0]);
							int x2 = (int)getXPixel(events[i + 1][0]);
							int width = x2 - x1 + 1;
							Paint pnt = g.getPaint();
							g.setPaint(new Color(255, 0, 0, 128));
							g.fillRect(x1, Y_OFFSET + 1, width, WaveViewPanel.this.getSize().height - BOTTOM_HEIGHT - Y_OFFSET);
							g.setPaint(pnt);
						}
					}	
				}
	*/
}