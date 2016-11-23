
package straightskeleton;

import java.util.Comparator;
import java.util.Iterator;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import straightskeleton.ui.Bar;
import org.twak.utils.Cache;
import org.twak.utils.LContext;
import org.twak.utils.Loop;
import org.twak.utils.LoopL;
import org.twak.utils.Loopable;

/**
 *
 * @author twak
 */
public class Corner extends Point3d implements Iterable<Corner>
{
    public Edge nextL, prevL;

    public Corner nextC, prevC;

    public Corner( double x, double y, double z )
    {
        super(x,y,z);
    }

    public Corner (Tuple3d in)
    {
        super (in);
    }

    public Corner( double x, double y )
    {
        super(x,y,0);
    }

    public Point3d getLoc3()
    {
        return new Point3d (x, y, 0);
    }

    /**
     * Corners (unlike point3ds) are only equal to themselves. We never move a point,
     * but can create multiple (uniques) at one location. We also change prev/next pointers to
     * edges and other corners but need to retain hashing behaviour. Therefore we revert to
     * the system hash.
     *
     * @param t1
     * @return
     */
    @Override
    public boolean equals( Object t1 )
    {
        return this == t1;
//        try
//        {
//            Corner other = (Corner )t1;
//            return super.equals( t1 ) &&
//                    other.nextL == nextL &&
//                    other.prevL == prevL;
//        }
//        catch (ClassCastException e)
//        { return false; }
    }

    @Override
    public String toString()
    {
        return String.format("(%f,%f,%f)", x,y,z);
    }

    /**
     * We rely on the fact that we can shift the point's heights without changing
     * their locations in hashmaps.
     * @return
     */
    @Override
    public int hashCode()
    {
        return System.identityHashCode( this );
    }




    public Iterator iterator()
    {
        return new CornerIterator( this );
    }

    /**
     * Over all corners in the same loop as the one given
     */
    public class CornerIterator implements Iterator<Corner>
    {
        Corner s, n, start;

        public CornerIterator( Corner start )
        {
            s = start;
            n = null;
        }
        public boolean hasNext()
        {
            if (s == null)
                return false;
            if (n == null)
                return true;
            return n != s;
        }

        public Corner next()
        {
            if (n == null)
                n = s;

            Corner out = n;
            n = n.nextC;
            return out;
        }

        public void remove()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }
    }

    public static class CornerDistanceComparator implements Comparator<Point3d>
    {
        Point3d start;
        
        public CornerDistanceComparator( Point3d corner )
        {
            this.start = corner;
        }

        public int compare( Point3d o1, Point3d o2 )
        {
            return Double.compare( start.distanceSquared( o1 ), start.distanceSquared( o2 ) );
        }
    }
    
    public static void replace( Corner old, Corner neu, Skeleton skel )
    {
        old.prevL.currentCorners.remove( old );
        old.nextL.currentCorners.remove( old );
        
        old.nextC.prevC = neu;
        old.prevC.nextC = neu;
        neu.prevC = old.prevC;
        neu.nextC = old.nextC;

        neu.nextL = old.nextL;
        neu.prevL = old.prevL;

        neu.prevL.currentCorners.add( neu );
        neu.nextL.currentCorners.add( neu );

        skel.liveCorners.remove( old );
        skel.liveCorners.add( neu );
    }

    /**
     * Clones this set of corners.
     *
     * Creates new edges, corners and machines from the given set.
     * Preserves currentCorners.
     *
     * @param ribbon
     * @return
     */
    public static LoopL<Corner> dupeNewAll( LoopL<Corner> ribbon )
    {

        final Cache<Corner, Corner> cacheC = new Cache<Corner, Corner>()
        {
            Cache<Machine, Machine> cacheM = new Cache<Machine, Machine>() {

                @Override
                public Machine create(Machine i) {
                    return new Machine(i.currentAngle);
                }
            };
            
            Cache<Edge, Edge> cacheE = new Cache<Edge, Edge>() {

                @Override
                public Edge create(Edge i) {
                    Edge out = new Edge(getCorner(i.start), getCorner(i.end));
                    out.setAngle(i.getAngle());
                    out.machine = cacheM.get(i.machine);

                    for (Corner c : i.currentCorners) {
                        out.currentCorners.add(getCorner(c));
                    }

                    return out;
                }
            };

            public Corner getCorner( Corner input ) // wrapper for inner caches - stupid java
            {
                Corner ner = get (input);

                return ner;
            }

            @Override
            public Corner create( Corner i )
            {
                Corner ner = new Corner (i.x, i.y);

                cache.put( i, ner);

                ner.nextC = get (i.nextC); // Cache<Corner,Corner>.get()
                ner.prevC = get (i.prevC);
                ner.nextL = cacheE.get (i.nextL);
                ner.prevL = cacheE.get (i.prevL);

                return ner;
            }
        };



        LoopL<Corner> loopl = new LoopL();
        for (Loop<Corner> pLoop : ribbon)
        {
            Loop<Corner> loop = new Loop();
            loopl.add( loop );
            for ( Corner c : pLoop )
                loop.append( cacheC.get(c) );
        }

        return loopl;
    }

    public static LoopL<Point3d> dupeNewAllPoints( LoopL<Point3d> ribbon )
    {

        final Cache<Point3d, Point3d> cacheC = new Cache<Point3d, Point3d>()
        {
                @Override
                public Point3d create(Point3d i) {
                    return new Point3d(i);
                }
        };

        LoopL<Point3d> loopl = new LoopL();
        for (Loop<Point3d> pLoop : ribbon)
        {
            Loop<Point3d> loop = new Loop();
            loopl.add( loop );
            for ( Point3d c : pLoop )
                loop.append( cacheC.get(c) );
        }

        return loopl;
    }

    public static LoopL<Point3d> dupeNewAllPoints( LoopL<Point3d> ribbon, final double height )
    {

        final Cache<Point3d, Point3d> cacheC = new Cache<Point3d, Point3d>()
        {
                @Override
                public Point3d create(Point3d i) {
                    return new Point3d(i.x, i.y, height);
                }
        };

        LoopL<Point3d> loopl = new LoopL();
        for (Loop<Point3d> pLoop : ribbon)
        {
            Loop<Point3d> loop = new Loop();
            loopl.add( loop );
            for ( Point3d c : pLoop )
                loop.append( cacheC.get(c) );
        }

        return loopl;
    }

    public static LoopL<Corner> fromBar( LoopL<Bar> ribbon )
    {
        LoopL<Corner> loopl = new LoopL();


        Cache<Point2d, Corner> cache = new Cache<Point2d, Corner>()
        {
            @Override
            public Corner create( Point2d i )
            {
                return new Corner (i.x, i.y);
            }
        };

        for (Loop<Bar> pLoop : ribbon)
        {
            Loop<Corner> loop = new Loop();
            loopl.add( loop );
            for ( Bar bar : pLoop )
                loop.append( cache.get(bar.start) );
        }

        for (Loop<Corner> loop : loopl)
            for (Loopable<Corner> loopable : loop.loopableIterator())
            {
                Corner p = loopable.get(), n = loopable.getNext().get();
                p.nextC = n;
                n.prevC = p;
                Edge e = new Edge(p, n);
                p.nextL = e;
                n.prevL = e;
            }

        return loopl;
    }

    public static LoopL<Point2d> toPoint2d(LoopL<Corner> shape)
    {
        return shape.new Map<Point2d>()
        {
            @Override
            public Point2d map(Loopable<Corner> input)
            {
                return new Point2d(input.get().x, input.get().y);
            }
        }.run();
    }

    public static LoopL<Point3d> toPoint3d(LoopL<Corner> shape)
    {
        return shape.new Map<Point3d>()
        {
            @Override
            public Point3d map(Loopable<Corner> input)
            {
                return new Point3d(input.get().x, input.get().y, input.get().z);
            }
        }.run();
    }

    public static LContext<Corner> findLContext( LoopL<Corner> in, Corner c )
    {
        for ( Loop<Corner> loop : in )
            for ( Loopable<Corner> lc : loop.loopableIterator() )
                if ( lc.get() == c )
                    return new LContext<Corner>( lc, loop );
        
        return null;
    }
    
	public static LoopL<Corner> cornerToEdgeLoopL(LoopL<Edge> in) {
		
		LoopL<Corner> corners = new LoopL();
		
         for (Loop<Edge> le : in)
         {
             Loop<Corner> lc = new Loop<Corner>();
             corners.add(lc);
             for (Edge e : le)
             {
                 lc.append( e.start);
                 e.start.nextL = e;
                 e.end.prevL = e;
                 e.start.nextC = e.end;
                 e.end.prevC = e.start;
             }
         }
         
         return corners;
	}
}
