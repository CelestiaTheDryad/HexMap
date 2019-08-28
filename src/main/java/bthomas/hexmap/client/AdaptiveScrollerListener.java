package bthomas.hexmap.client;

import javax.swing.BoundedRangeModel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 Keep text field scrolled to the bottom if 1. user has scrolled to the bottom 2. user does not have text highlighted
 <p>
 https://stackoverflow.com/questions/4045722/how-to-make-jtextpane-autoscroll-only-when-scroll-bar-is-at-bottom-and-scroll-lo
 */
public class AdaptiveScrollerListener implements AdjustmentListener, MouseWheelListener, CaretListener
{
    private boolean doAutoScroll = true;
    private BoundedRangeModel brm;

    public AdaptiveScrollerListener(JScrollPane scrollPaneToManage)
    {
        brm = scrollPaneToManage.getVerticalScrollBar().getModel();
    }

    @Override
    public synchronized void adjustmentValueChanged(AdjustmentEvent e)
    {
        if(!brm.getValueIsAdjusting())
        {
            if(doAutoScroll)
            {
                SwingUtilities.invokeLater(() -> brm.setValue(brm.getMaximum()));
            }
        }
        else
        {
            // doAutoScroll will be set to true when user reaches at the bottom of document.
            doAutoScroll = ((brm.getValue() + brm.getExtent()) == brm.getMaximum());
        }
    }

    @Override
    public synchronized void mouseWheelMoved(MouseWheelEvent e)
    {
        if(e.getWheelRotation() < 0)
        {
            doAutoScroll = false;
        }
        else
        {
            doAutoScroll = ((brm.getValue() + brm.getExtent()) == brm.getMaximum());
        }
    }

    @Override
    public synchronized void caretUpdate(CaretEvent e)
    {
        doAutoScroll = (e.getDot() == e.getMark()) && ((brm.getValue() + brm.getExtent()) == brm.getMaximum());
    }
}
