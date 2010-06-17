/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package straightskeleton.ui;

import java.util.List;
import javax.swing.JComponent;
import straightskeleton.Edge;
import straightskeleton.HeightEvent;
import straightskeleton.Machine;

/**
 *
 * @author twak
 */
public abstract class MachineEvent implements Comparable<MachineEvent>
{
    public double height;

    public MachineEvent( double height )
    {
        this.height = height;
    }
    public MachineEvent()
    {}

    public int compareTo( MachineEvent o )
    {
        return Double.compare( height, o.height );
    }

    public abstract HeightEvent createHeightEvent( Machine machine, List<Edge> edgesToChange );
}
