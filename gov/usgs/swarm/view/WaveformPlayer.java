package gov.usgs.swarm.view;


/**
 * $Log: not supported by cvs2svn $
 * Revision 1.2  2004/10/26 18:02:29  cvs
 * Moved block comment from Swarm.java to here.
 *
 * @author Dan Cervelli
 */
public class WaveformPlayer 
{}
/*
extends JPanel implements Runnable 
{
	
	// Test code for experimental WaveformPlayer.
	// This code goes in Swarm.java.
	m.getInputMap().put(KeyStroke.getKeyStroke("control F5"), "testkey");
	m.getActionMap().put("testkey", new AbstractAction()
			{
				public void actionPerformed(ActionEvent e)
				{
					JInternalFrame jif = new JInternalFrame("Test");
					jif.setSize(800, 420);
					jif.setLocation(50, 50);
					double now = CurrentTime.nowJ2K();
					SampledWave sw = lastDataSource.getSampledWave("MCK  ", 8.96343E7 - 300, 8.96343E7);
					WaveformPlayer wp = new WaveformPlayer();
					wp.setModel(new WaveModel(new Wave(sw)));
					jif.setContentPane(wp);
					desktop.add(jif);
					jif.setVisible(true);
				}
			});
	
	
	protected WaveModel model;
	
	protected Waveform fullView;
	protected WaveComponent playView;

	protected double markTime;
	protected double span = 60;
	protected double speedFactor = 6.0;
	
	protected Thread playThread;
	
	public WaveformPlayer()
	{
		super(new BorderLayout());
		
		JPanel mainPanel = new JPanel(new GridLayout(2, 1));
		fullView = new Waveform();
		playView = new Spectra();//Waveform();
		mainPanel.add(playView);
		mainPanel.add(fullView);
		
		this.add(mainPanel, BorderLayout.CENTER);
		playThread = new Thread(this);
	}
	
	public void setModel(WaveModel m)
	{
		model = m;
		fullView.setModel(model);
		playView.setModel(model);
		double st = model.getWave().getStartTime();
		fullView.setViewTimes(st, model.getWave().getEndTime());
		fullView.setHighlight(st, st + span);
		playView.setViewTimes(st, st + span);
		markTime = st;
		if (!playThread.isAlive())
			playThread.start();
	}
	
	public void run()
	{
		long last = System.currentTimeMillis();
		while (true)
		{
			long now = System.currentTimeMillis();
			long t = now - last;
			last = now;
			markTime += ((double)t*2 / 1000) * speedFactor;
			fullView.setHighlight(markTime, markTime + span);
			playView.setViewTimes(markTime, markTime + span);
			repaint();
			
			if (markTime + span >= model.getWave().getEndTime())
				markTime = model.getWave().getStartTime();
			try { Thread.sleep(30); }  catch (Exception e) {}
		}
	}
}
*/