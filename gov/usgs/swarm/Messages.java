package gov.usgs.swarm;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * This class was automatically created by Eclipse.
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public class Messages
{
	private static final String BUNDLE_NAME = "gov.usgs.swarm.messages"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			.getBundle(BUNDLE_NAME);

	private Messages()
	{
	}

	public static String getString(String key)
	{
		// TODO Auto-generated method stub
		try
		{
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e)
		{
			return '!' + key + '!';
		}
	}
}
