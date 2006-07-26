package gov.usgs.swarm.data;

/**
 * 
 * @author Dan Cervelli
 */
public interface GulperListener
{
	public void gulperStarted();
	public void gulperStopped(boolean killed);
	public void gulperGulped(double t1, double t2, boolean success);
}
