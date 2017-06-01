/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.twak.camp.ui;

import java.util.List;
import javax.swing.JComponent;

import org.twak.camp.Edge;
import org.twak.camp.HeightEvent;
import org.twak.camp.Machine;

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
