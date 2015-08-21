package gov.usgs.volcanoes.swarm;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * This class was automatically created by Eclipse.
 * 
 * @author Dan Cervelli
 */
public class Messages
{
	private static final String BUNDLE_NAME = "messages"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ENGLISH);

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
