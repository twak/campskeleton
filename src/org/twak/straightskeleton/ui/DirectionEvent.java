
package org.twak.straightskeleton.ui;

import java.util.List;
import javax.swing.JComponent;

import org.twak.straightskeleton.Edge;
import org.twak.straightskeleton.HeightEvent;
import org.twak.straightskeleton.Machine;

/**
 *
 * @author twak
 */
public class DirectionEvent extends MachineEvent
{
    public double angle;

    public DirectionEvent( double angle, double height )
    {
        super (height);
        this.angle = angle;
    }
    public DirectionEvent()
    {}

    @Override
    public HeightEvent createHeightEvent( Machine m, List<Edge> edgesToChange )
    {
        return new DirectionHeightEvent(m,  height, angle );
    }
}
