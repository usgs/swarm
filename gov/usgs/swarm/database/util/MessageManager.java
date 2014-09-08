package gov.usgs.swarm.database.util;

import java.awt.Component;

import javax.swing.JOptionPane;

/**
 * Showing messages for tasks.
 * 
 * @author Chirag Patel
 */
public class MessageManager
{
	public static void popupErrorMessage(String message, Component parent)
	{
		JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE, null);
	}

	public static void popupInfoMessage(String message, Component parent)
	{
		JOptionPane.showMessageDialog(parent, message, "Info", JOptionPane.INFORMATION_MESSAGE);
	}
}