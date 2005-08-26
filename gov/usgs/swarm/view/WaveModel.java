package gov.usgs.swarm.view;


/**
 * @author cervelli
 */
public class WaveModel
{}
/*
{
	protected Wave wave;
	
	protected List listeners;
	
	public WaveModel()
	{
		listeners = new ArrayList(10);
	}
	
	public WaveModel(Wave w)
	{
		this();
		wave = w;
	}
	
	public Wave getWave()
	{
		return wave;
	}
	
	public void setWave(Wave sw)
	{
		wave = sw;
		fireChangeAction();
	}
	
	public void addListener(ActionListener al)
	{
		listeners.add(al);
	}
	
	public void removeListener(ActionListener al)
	{
		listeners.remove(al);
	}
	
	public void fireChangeAction()
	{
		for (Iterator it = listeners.iterator(); it.hasNext(); )
			((ActionListener)it.next()).actionPerformed(new ActionEvent(this,
					ActionEvent.ACTION_PERFORMED, "modelChanged"));
	}
	
	public void dispose()
	{
		wave = null;
		listeners.clear();
	}
}
*/