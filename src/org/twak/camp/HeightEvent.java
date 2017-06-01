package org.twak.camp;

import java.util.Comparator;

/**
 * An event...that occurs at a specific height
 *
 * Needs to deal with equals and hash for hashsets
 *
 * @author twak
 */
public interface HeightEvent
{
    public double getHeight();

    /**
     * Return true if the event takes some action, false otherwise
     */
    public boolean process( Skeleton skel );

    public static Comparator<HeightEvent> heightComparator = new Comparator<HeightEvent>()
    {
        public int compare( HeightEvent o1, HeightEvent o2 )
        {
            return Double.compare( o1.getHeight(), o2.getHeight() );
        }
    };
}
