package gov.usgs.swarm;

import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;

/**
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public class SwarmUtil
{
	private static final Insets ZERO_INSETS = new Insets(0, 0, 0, 0);
	
	public static JSplitPane createStrippedSplitPane(int orient, JComponent comp1, JComponent comp2)
	{
		JSplitPane split = new JSplitPane(orient, comp1, comp2);
		split.setBorder(BorderFactory.createEmptyBorder());
		SplitPaneUI splitPaneUI = split.getUI();
	    if (splitPaneUI instanceof BasicSplitPaneUI)
	    {
	        BasicSplitPaneUI basicUI = (BasicSplitPaneUI)splitPaneUI;
	        basicUI.getDivider().setBorder(BorderFactory.createEmptyBorder());
	    }
	    return split;
	}
	
	public static JToolBar createToolBar()
	{
		JToolBar tb = new JToolBar();
		tb.setFloatable(false);
		tb.setRollover(true);
		tb.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));
		return tb;
	}
	
	public static JButton createToolBarButton(ImageIcon ic, String toolTip, ActionListener al)
	{
		JButton button = new JButton(ic);
		fixButton(button, toolTip);
		if (al != null)
			button.addActionListener(al);
		
		return button;
	}
	
	public static JToggleButton createToolBarToggleButton(ImageIcon ic, String toolTip, ActionListener al)
	{
		JToggleButton button = new JToggleButton(ic);
		fixButton(button, toolTip);
		if (al != null)
			button.addActionListener(al);
		
		return button;
	}
	
	private static void fixButton(AbstractButton button, String toolTip)
	{
		button.setFocusable(false);
		button.setMargin(ZERO_INSETS);
		button.setToolTipText(toolTip);
	}
	
	public static int linearSearch(int[] array, int val)
	{
		for (int i = 0; i < array.length; i++)
			if (array[i] == val)
				return i;
		
		return -1;
	}
}
