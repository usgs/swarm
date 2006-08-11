package gov.usgs.swarm;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import com.jgoodies.looks.plastic.PlasticLookAndFeel;

/**
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2006/04/17 04:16:36  dcervelli
 * More 1.3 changes.
 *
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
	
	/**
	 * I've modified the standard jgoodies border to be thicker to make
	 * interal frame resizes easier.
	 */
	public static Border getInternalFrameBorder()
	{
		return new InternalFrameBorder();
	}
	
    private static final class InternalFrameBorder extends AbstractBorder implements UIResource
    {
		private static final long serialVersionUID = 1L;
		private static final Insets NORMAL_INSETS	 = new Insets(3, 3, 3, 3);
        private static final Insets MAXIMIZED_INSETS = new Insets(1, 1, 0, 0);

        private void drawInsetThinFlush3DBorder(Graphics g, int x, int y, int w, int h) {
    		g.translate(x, y);
    		g.setColor(PlasticLookAndFeel.getControlHighlight());
    		g.drawLine(2, 2, w - 4, 2);
    		g.drawLine(2, 2, 2, h - 4);
    		g.setColor(PlasticLookAndFeel.getControlDarkShadow());
    		g.drawLine(w - 3, 2, w - 3, h - 4);
    		g.drawLine(2, h - 3, w - 3, h - 3);
    		g.translate(-x, -y);
    	}
        
		public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
			JInternalFrame frame = (JInternalFrame) c;
			if (frame.isMaximum())
				paintMaximizedBorder(g, x, y, w, h);
			else
                drawInsetThinFlush3DBorder(g, x, y, w, h);
		}
		
		private void paintMaximizedBorder(Graphics g, int x, int y, int w, int h) {
            g.translate(x, y);
            g.setColor(PlasticLookAndFeel.getControlHighlight());
            g.drawLine(0, 0, w - 2, 0);
            g.drawLine(0, 0, 0, h - 2);
            g.translate(-x, -y);
		}

	    public Insets getBorderInsets(Component c) { 
	    	return ((JInternalFrame) c).isMaximum() ? MAXIMIZED_INSETS : NORMAL_INSETS;
	    }
    }
}
