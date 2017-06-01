
package org.twak.camp;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.twak.camp.ui.DirectionHeightEvent;

/**
 * A machine controls the angle of it's set of edges over time (height...)
 *
 * superclass of all machines
 *
 * idea is to add all directions before adding edges. When you add the first edge
 *
 * instead of just adding all height changes to the main event queue, this
 * machine only adds the next one. Reason here is that we might want to change
 * our minds as we build upwards.
 *
 * @author twak
 */
public class Machine
{
    public Color color; // color used in the ui
    // a machine will only ever have one pending event in the skeleton.qu, others are stored here
    public List<HeightEvent> events = new ArrayList();
    String description = "unnamed machine";
    public double currentAngle = Math.PI/4; // when a edge is added this is the angle it is given
    public transient HeightEvent heightEvent;
    public transient int currentDirection = -1;

    protected Set<Edge> seenEdges = new LinkedHashSet();

    // for pretty output
    static Color[] rainbow = new Color[] {
        Color.red,
        Color.green,
        Color.blue,
        Color.magenta };

    static String[] rainbowStrings = new String[] {"red", "green", "blue", "magenta" };
    static int rainbowIndex = 0;

    public Machine()
    {
        this (Math.PI/4);
    }

    public Machine( double initial )
    {
        color = rainbow[ rainbowIndex % rainbowStrings.length ];
        description = rainbowStrings[ rainbowIndex % rainbowStrings.length ];
        rainbowIndex++;
        addHeightEvent( new DirectionHeightEvent( this, initial ) );
        currentAngle = initial;
    }


    @Override
    public String toString()
    {
        return description;
    }

    /**
     * Called once after the machine is assigned to it's first edge
     */
    public void addEdge( Edge e, Skeleton skel )
    {
        if ( heightEvent == null )
            findNextHeight( skel );

        // when we're new, or sometimes when we swap a machine out and back in again things get confused
//        if ( seenEdges.add( e ) || e.getAngle() != currentAngle )
//        {
            e.setAngle( currentAngle );
//        }
    }

    public List<Edge> findOurEdges( Skeleton skel )
    {
        List<Edge> edgesToChange = new ArrayList();

        for ( Edge e : skel.liveEdges )
            if (e.machine == this)
                edgesToChange.add(e);
        
        return edgesToChange;
    }

    public void findNextHeight( Skeleton skel )
    {
        if (events.isEmpty())
            throw new Error ("I need height events!");

        currentDirection++;

        if (currentDirection == 0)
        {
            if ( events.get( 0 ) instanceof DirectionHeightEvent )
            {
                // first direction added to a new edge is taken to be the starting angle
                currentAngle = ((DirectionHeightEvent)events.get(0)).newAngle;
                heightEvent = events.get( 0 ); 
                currentDirection ++;
                // proceed to add the following direction...
            }
            else
                // I ran into trouble - as we add edges, we want to be able to set the initial angle and ignore the first height event.
                // Otherwise we immediately call replace edges in skeleton and introduce additional edges
                throw new Error ("You have to think really hard about how the first event sets it's angle before you do this");
        }

        if (currentDirection >= getDirections().size())
            return;

        heightEvent = getDirections().get( currentDirection );

        skel.qu.add( heightEvent );
    }

    /**
     * @return the directions
     */
    public List<HeightEvent> getDirections()
    {
        return events;
    }

    public void sortHeightEvents()
    {
        Collections.sort( events, HeightEvent.heightComparator );
    }

    /**
     * @param directions the directions to set
     */
    public void addHeightEvent( HeightEvent dir )
    {
        events.add( dir );
        sortHeightEvents();
    }
}
