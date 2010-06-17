
package utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import javax.vecmath.Point2d;

/**
 *
 * @author twak
 */
public class Intersector 
{

    PriorityQueue <Event> events;
    TreeSet<Line> beachline = new TreeSet( new XOrderLineComparator() );
    List<Collision> output = new ArrayList();

    // double sweepY; // height of sweep line through line-field (used for entering stuff into the beachline)
    Point2d processing;
         
    /**
     */
    public List<Collision> intersectLines (List<Line> a)
    {
        index_ = 0;
        events = new PriorityQueue<Event>();
       
        // add all start and end line events into beachfront, 
        // swap start and end for minimum x coord.
        for (Line orig : a)
        {
            Line iL = new ILine(orig);
            
            Event sE = new Event( iL.start, START);
            sE.a = iL;
            events.add( sE );
            
            Event eE = new Event( iL.end, END);
            eE.a = iL;
            events.add( eE );
        }
        
        // get all events at the equivelent location
        while (events.size() > 0)
        {
            // cosited events of different types
            Set<Event> starts = new HashSet();
            Set<Event> ends = new HashSet();
            Set<Event> intersects = new HashSet();
            
            Event e = events.peek();
            Event f = e;
        
            while ( e.location.equals(f.location) )
            {
                Event toAdd = events.poll();
                switch (toAdd.type)
                {
                    case INTERSECT:
                        intersects.add(toAdd);
                        break;
                    case START:
                        starts.add(toAdd);
                        break;
                    case END:
                        ends.add(toAdd);
                        break;
                }
                                
                f = events.peek();
                if (f == null)
                    break;
            }

//            dumpBeach();
            process( starts, ends, intersects, e.location );
        }
        
        return output;
    }
    
    private void collide( Line a, Line b )
    {
        Point2d collide = a.intersects( b );

        if (collide == null)
            return;

        for (Line l : new Line[] {a,b})
        {
            if (l.isHoriz())
                collide.y = l.start.y;
            if (l.isVert())
                collide.x = l.start.x;
        }

        if ( a.start.equals( b.end ) || a.start.equals( b.start ))
            collide = a.start;
        
        if ( a.end.equals( b.end ) || a.end.equals( b.start ))
            collide = a.end;

        // don't report collisions behind the line
        if (collide.y < processing.y || (collide. y == processing.y && collide.x <= processing.x))
            return;


        Event e = new Event(collide, INTERSECT);
        e.a = a;
        e.b = b;
        events.add(e);
    }
    
    /**
     * 
     * @param starts line-start events
     * @param ends line-end events
     * @param intersects line-line intersect events
     */
    private void process (Set<Event> starts, Set<Event> ends, Set<Event> intersects, Point2d location )
    {
        // just gimme an event!
        Event sample = starts.size() > 0 ? 
            starts.iterator().next() :
            ends.size() > 0 ?
                ends.iterator().next() :
            intersects.iterator().next();
        
//        if (sample.a.isHoriz())
//        {
//            System.out.println("that's the one");
//        }
        
        // if 2 lines are involved
        if ( intersects.size() > 0 || starts.size() + ends.size() > 1)
        {
            Set<Line> lines = new LinkedHashSet();
            
            for (Event e : new ItComb <Event> (starts, ends) )
                lines.add( ((ILine)e.a).orig );
            for (Event e : intersects )
            {
                lines.add(((ILine)e.a).orig );
                lines.add(((ILine)e.b).orig );
            }
            Collision c = new Collision(sample.location, new ArrayList ( lines ));
                output.add( c );
        }

        Set<Line> ending = new HashSet();
        for (Event e : ends)
            ending.add (e.a);
        
        for ( Event e : new ItComb<Event> (ends, intersects ) ) // intersects.get(0).b.equals (intersects.get(1).b)
        {
            boolean removed = beachline.remove(e.a);
//            assert (removed);
            if (e.b != null)
            {
                removed = beachline.remove(e.b);
//                assert(removed);
            }
        }

        // to be able to remove horizontal lines, the value of processing must be the same as when they were last touched
        // (otherwise they're lost in the table). horiztonal lines should be involved in every event at their level
        processing = location;

//        Set<Line> lines= new HashSet (beachline);
//        beachline.clear();
//        beachline.addAll( lines );

        // all elements in 3 lists have same x value. Higer will return next higher value in beachlines
        Line nextLine = beachline.higher ( sample.a );
        Line prevLine = beachline.lower( sample.a );

        // do not add any lines that have just ended (some intersections fall into this category).
        for ( Event e : new ItComb<Event>(starts, intersects) )
        {

            if ( !ending.contains( e.a ) )
                beachline.add(e.a);
            if ( e.b != null && !ending.contains( e.b ) )
            {
                beachline.add( e.b ); //intersects
            }
        }

        Set<Line> processed = new HashSet();
        for (Event e : intersects)
        {
            processed.add(e.a);
            processed.add(e.b);
        }
        
        for (Event e : ends)
            processed.remove( e.a );

        for (Event e : starts)
            processed.add( e.a );


        // if we've removed everything we processed
        if (processed.size() == 0)
        {
            if (prevLine != null && nextLine != null ) // not at end or start of beachline
                collide( prevLine, nextLine);
        }
        else
        {
            // go through start and intersections to find min and maximum
            // deal with horizontal lines!

            List<Line> p = new ArrayList(processed);
            Collections.sort( p, new XOrderLineComparator() ); 

            Set<Line> horiz= new HashSet();
            for ( Line l : p )
                if ( l.isHoriz() )
                    horiz.add( l );

            if (nextLine != null )
            {
                Line h = p.get( p.size() - 1 );
                if ( !horiz.isEmpty() )
                    for ( Line l : horiz )
                        collide( l, nextLine );
                else // business as normal
                    collide( h, nextLine );

            }
            
            if (prevLine != null)
            {
                Line l = p.get( 0 );
                collide( l, prevLine );
            }
        }
    }
    
    final static int START = 1, END = 2, INTERSECT = 4;
    
    private static class PointComparator implements Comparator<Point2d>
    {
        public int compare( Point2d o1, Point2d o2 )
        {
            double r = o1.y - o2.y;
            
            if (r < 0)
                return -1;
            if (r > 0)
                return 1;
            
            r = o1.x - o2.x;
            
            if (r < 0)
                return -1;
            if (r > 0)
                return 1;
            return 0;
        }
    }
    
    static PointComparator pointComparator = new PointComparator();
    
    private class Event implements Comparable<Event>
    {
        Point2d location = new Point2d(Double.MAX_VALUE, Double.MAX_VALUE);
        int type = 0;
        Line a,b;
        
        public Event (Point2d location, int type)
        {
            this.location = location;
            this.type = type;
        }
        
        public int compareTo( Event o )
        {
            return pointComparator.compare(location, o.location);
        }
        
        @Override
        public String toString()
        {
            switch (type)
            {
                case START:
                    return "start "+location;
                case END:
                    return "end   "+location;
                case INTERSECT:
                    return "inter "+location;
            }
            
            return "invalid type in Event!";
        }

        @Override
        public int hashCode()
        {
            int hash = 7;
            hash = 29 * hash + ( this.location != null ? this.location.hashCode() : 0 );
            hash = 29 * hash + this.type;
            hash = 29 * hash + ( this.a != null ? this.a.hashCode() : 0 )+ ( this.b != null ? this.b.hashCode() : 0 );
            return hash;
        }

        @Override
        public boolean equals( Object obj )
        {
            if ( obj == null )
                return false;
            if ( getClass() != obj.getClass() )
                return false;
            final Event other = (Event) obj;
            if ( !this.location.equals  ( other.location ) )
                return false;
            if ( this.type != other.type )
                return false;
            if ( this.b != null  )
            {
                if ( this.a.equals( other.a ) )
                    return ( this.b.equals( other.b ) );
                else if ( this.b.equals( other.a ) )
                    return ( this.a.equals( other.b ) );
            }
            else if ( !this.a.equals( other.a ) )
                return false;
            
            return true;
        }
    }
    
    
    private void dumpBeach()
            
    {
        System.out.print( processing+":  " );
        for (Line l : beachline)
        {
            System.out.print(l+"["+l.xAtY( processing.y )+"]:: ");
        }
        System.out.println();
    }
    
    private class XOrderLineComparator implements Comparator<Line>
    {

        public int compare( Line o1, Line o2 )
        {
            Line horiz = null;
            Line notHoriz = null;
            int factor = 1;
            
            if (o1.equals( o2 ))
                return 0;
            
            // if both are horizontal, reults are undefined.
            if ( o1.isHoriz() && o2.isHoriz())
            {
                 if ( o1.start.y == o2.start.y )
                 {
                    // assign an arbitrary order
                    return o1.hashCode() - o2.hashCode();
//                if (o1.start.x != o2.start.x);
//                    return Double.compare( o1.start.x, o2.start.x );\
                 }
                 else
                 {
                     // shouldn't really be used!
                     return Double.compare( o1.start.y, o1.end.y);
                 }
            }
            else if ( o1.isHoriz() )
            {
                horiz = o1;
                notHoriz = o2;
                factor = 1;
            }
            else if (o2.isHoriz())
            {
                horiz = o2;
                notHoriz = o1;
                factor = -1; // swap ordering          
            }
            
            if ( horiz != null )
            {
                double x = notHoriz.xAtY( processing.y );

                if ( notHoriz.isVert() )
                    x = notHoriz.start.x;

                if (x < horiz.start.x)
                {
                    return 1 * factor;
                }
                else if (x > horiz.end.x)
                {
                    return -1 * factor;
                }
                else if ( processing.x < x ) // intersection between start and end
                {
                        return -1 * factor;
                }
                else if (processing.x > x)
                {
                        return 1 * factor;
                }
                else
                {
                    // horizontal is always second in the beachline to a vertical (if we are processing tha tpoint)
                    return factor;
                }
            }
            else
            {
                
            /* 
             * We compare a little higher, so when 
             * sweepLine == intersect point the order reflects the line's new positions
             */
                double height = processing.y + 0.0001;

                double 
                h1 = o1.xAtY( height ), 
                h2 = o2.xAtY( height );

                if (o1.isVert())
                    h1 = o1.start.x;
                if (o2.isVert())
                    h2 = o2.start.x;

                if (h1 == h2)
                {
                    // assign an arbitrary order to vertical lines
                    return o1.hashCode() - o2.hashCode();
                }
                else return Double.compare( h1, h2 );
            }
        }
    }

   static int index_ = 0;

    public static class Collision
    {
        public Point2d location;
        public List<Line> lines;
        public int index = index_++;
        public Collision (Point2d loc, List<Line> lines)
        {
            this.location = loc;
            this.lines = lines;
        }
    }
    
      
    /**
     * We change the line directions to be homogenious. But we need to return a
     * list of the original Lines with the collisions. This class orders the original
     * line correctly, while keeping a reference to the original.
     */
    static class ILine extends Line
    {
        Line orig; // line passed into intersector
        
        public ILine (Line l)
        {
            super ();
            
            if (pointComparator.compare(l.start, l.end) > 0)
            {
                start = l.end;
                end = l.start;
            }
            else
            {
                start = l.start;
                end = l.end;
            }
            
            this.orig = l;
        }
    }
    
    
    public static void main (String[] args)
    {
        int count = 100;
        Random randy = new Random();
        while (true)
        {
            count += 100;
            
            List<Line> list = new ArrayList();
            for (int i = 0; i < count; i++)
            {
                list.add (new Line (
                        new Point2d(randy.nextInt( 600 ),randy.nextInt( 600 )),
                        new Point2d(randy.nextInt( 600 ),randy.nextInt( 600 ))));
            }
            long t = System.currentTimeMillis();
            Intersector i = new Intersector();
            i.intersectLines( list );
            System.out.printf ( "%d, %d \n", count, System.currentTimeMillis() - t);
        }
    }
}
