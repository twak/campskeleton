package straightskeleton.ui;

import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import straightskeleton.Edge;
import straightskeleton.Feature;
import org.twak.utils.Cache;
import org.twak.utils.Line;
import org.twak.utils.Loop;
import org.twak.utils.LoopL;

/**
 * @author twak
 */
public class Bar
{
    public Point2d start, end;
    public List<Marker> markers = new ArrayList();
    public Set<Feature> tags = new LinkedHashSet();
    public Mould mould = new Mould();

    public Bar (Point2d start, Point2d end)
    {
        this.start = start;
        this.end = end;
    }

    public double distance (Point2d p)
    {
        Line l = new Line (start, end);

        Point2d location = l.project( p, true );

        return location.distance( p );
    }

    public double lengthSquared()
    {
        return Math.pow( end.x - start.x, 2 ) + Math.pow( end.y - start.y, 2 );
    }

    public double length()
    {
        return Math.sqrt( lengthSquared() );
    }
    
    public void addMarker( Marker m )
    {
        m.set( new Line (start, end).project( m, true ) );
        markers.add( m );
        m.bar = this;
    }

    public void updateMarkers()
    {
        for (Marker m : markers)
        {
            m.set( new Line (start, end).project( m, true ) );
            m.bar = this;
        }

    }

    @Override
    public String toString ()
    {
        return "{"+start +", " +end +"}";
    }
    
    /**
     * Creates new loop with same shape, ignoring markers
     */
    public static Loop<Bar> clone (Loop<Bar> in, AffineTransform at )
    {
        Point2d start = new Point2d (in.start.get().start);

        if (at != null)
        {
            Point2D dest = new Point.Double();
            at.transform( new Point.Double (start.x, start.y), dest );
            start = new Point2d(dest.getX(), dest.getY() );
        }

        Loop<Bar> out = new Loop();
        for (Bar bar : in)
        {
            Point2d end = new Point2d( bar.end );
            
            if ( at != null )
            {
                Point2D dest = new Point.Double();
                at.transform( new Point.Double( end.x, end.y ), dest );
                end = new Point2d( dest.getX(), dest.getY() );
            }

            assert (start != null);
            assert (end != null);

            Bar newBar = new Bar( start, end ) ;
            newBar.markers = new ArrayList ();
            for (Marker m : bar.markers)
            {
                Marker newM = new Marker(m.feature);
                Point2D dest = new Point.Double();
                at.transform( new Point.Double( end.x, end.y ), dest );
                newM.set( dest.getX(), dest.getY() );
                newBar.markers.add( newM );
                newM.bar = newBar;
            }

            out.append( newBar );
            start = end;
        }

        // if we were a loop, a loop we shall be
        if (in.start.getPrev().get().end == in.start.get().start)
            out.start.getPrev().get().end = out.start.get().start;



        return out;
    }


    public static LoopL<Bar> fromEdges( LoopL<Edge> outside )
    {
                LoopL<Bar> loopl = new LoopL();


        Cache<Point3d, Point2d> cache = new Cache<Point3d, Point2d>()
        {
            @Override
            public Point2d create( Point3d i )
            {
                return new Point2d (i.x, i.y);
            }
        };

        for (Loop<Edge> pLoop : outside)
        {
            Loop<Bar> loop = new Loop();
            loopl.add( loop );
            for ( Edge edge : pLoop )
                loop.append( new Bar( cache.get( edge.start ), cache.get( edge.end ) ) );

        }

        return loopl;
    }

         public static LoopL<Bar> dupe( LoopL<Bar> ribbon )
    {
        LoopL<Bar> loopl = new LoopL();


        Cache<Point2d, Point2d> cache = new Cache<Point2d, Point2d>()
        {
            @Override
            public Point2d create( Point2d i )
            {
                return new Point2d (i.x, i.y);
            }
        };

        for (Loop<Bar> pLoop : ribbon)
        {
            Loop<Bar> loop = new Loop();
            loopl.add( loop );
            for ( Bar bar : pLoop )
                loop.append( new Bar( cache.get( bar.start ), cache.get( bar.end ) ) );

        }

        return loopl;
    }

    public static void reverse( LoopL<Bar> loopl )
    {
        for ( Bar e : loopl.eIterator() )
        {
            Point2d tmp = e.start;
            e.start = e.end;
            e.end = tmp;
        }

        for ( Loop<Bar> l : loopl )
            l.reverse();
    }

}
