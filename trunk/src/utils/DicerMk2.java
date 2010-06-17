package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static utils.DicerMk2.FType.*;
import static utils.DRectangle.*;

/**
 *
 * Rewrite with a different algorithm
 *
 * Three classes of line segment. high - part of a draw square
 * low - an extension of  a drawn square
 * none- a class we find via processing of line segments to remove
 *
 * Start by adding all line segments using low or high
 * Classify an array of points, one for each line cross every other. known the class of the line in each direction (nsew)
 * Classify some line sections as none. Those conginuous low sections that don't start at a high corner -> none
 * Output the rectangles made by the high/low lines. By finding all the possible
 * top lefts, traversing to fing the top rights, then traversing again to find the bottom rights...
 * 
 *
 * @author twak
 */
public class DicerMk2
{
    List<DRectangle> rects = new ArrayList();
    Map<Double, RC> horiz = new LinkedHashMap(), vert = new LinkedHashMap();

    public DicerMk2( List<DRectangle> uiInput_ )
    {
        if ( uiInput_.size() == 0 )
            return;

        // remove lines that are too close together (slight bug - doesn't adjust max coord when min. changes)
        final double TOL = 5;
        List<Double> xes = new ArrayList(), yes = new ArrayList();

        List<DRectangle> rectangles = new ArrayList();
        for (DRectangle dr : uiInput_)
            rectangles.add (new DRectangle (dr).toPositive());

        for (DRectangle dr : rectangles)
        {
            for (Object[] oa : new Object[][] { 
                { XMIN, xes },
                { XMAX, xes },
                { YMIN, yes },
                { YMAX, yes } })
            {
                double val = dr.get( oa[0] );
                List<Double> vals = (List) oa[1];

                int res = Collections.binarySearch( vals, val );

                if ( res < 0 && vals.size() > 0 )
                {
                    int index = -(res+1);
                    
                    double max = vals.get( MUtils.clamp( index, 0, vals.size()-1) );
                    double min = vals.get( MUtils.clamp( index-1, 0, vals.size()-1) );

                    if ( Math.abs ( max - val) < TOL )
                    {
                        val = max;
                    }
                    else if ( Math.abs ( val - min) < TOL )
                    {
                        val = min;
                    }
                }
                
                dr.set( oa[0], val, true );
                
                if ( ! vals.contains( val ) )
                {
                    vals.add( val );
                    Collections.sort( vals );
                }
            }
        }


        // add the edge of each rectangle to the data structure
        for (DRectangle dr : rectangles)
        {
            if (dr.area() < TOL * 3) // removes negative areas too :)
                continue;

            addTo( horiz, dr.y, dr.x, dr.x + dr.width );
            addTo( horiz, dr.y + dr.height, dr.x, dr.x + dr.width );
            addTo( vert, dr.x, dr.y, dr.y + dr.height );
            addTo( vert, dr.x + dr.width, dr.y, dr.y + dr.height );
        }


        List<Double> hKeys = new ArrayList( horiz.keySet() ),
                vKeys = new ArrayList( vert.keySet() );

//        Collections.sort( hKeys );
//        Collections.sort( vKeys );
//
//        RC lastRC = null;
//        for ( double h : hKeys )
//        {
//            if ( lastRC != null && h - lastRC.val < TOL )
//            {
//                lastRC.merge( horiz.get( h ) );
//                horiz.remove( h );
//            }
//            else
//            {
//                lastRC = horiz.get( h );
//            }
//        }
        
//        hKeys = new ArrayList( horiz.keySet() );
//        vKeys = new ArrayList( vert.keySet() );

        Collections.sort( hKeys );
        Collections.sort( vKeys );

        Pt[][] pts = new Pt[ horiz.keySet().size()] [ vert.keySet().size()];

        int h_ = 0;
        for ( double h : hKeys )
        {
            int v_ = 0;
            for ( double v : vKeys )
            {
                RC rcH = horiz.get( h );
                RC rcV = vert.get( v );

                Pair<FType, FType> hTypes = rcH.getPairsAt(v);
                Pair<FType, FType> vTypes = rcV.getPairsAt(h);

                pts[h_][v_] = new Pt( v, h, hTypes.first(), hTypes.second(), vTypes.first(), vTypes.second() );
                
                v_++;
            }
            h_++;
        }

        // remove (none-ify) doubly soft lines - horiz first...
        for (int h = 0; h < pts.length; h++)
        {
            int start = -1;
            boolean startWall = false;

            for (int v = 0; v < pts[0].length; v++)
            {
                Pt pt = pts[h][v];
                if (pt.hasHard())
                {
                    boolean isWall =  pt.t == HARD && pt.b == HARD && pt.l != HARD && pt.r != HARD;

                    if (isWall || ( pt.l != HARD && pt.r == HARD ) )
                    {
                        if ( isWall && startWall && start >= 0 )
                        {
                            pts[h][start].r = NONE;
                            pts[h][v].l = NONE;
                            for ( int v2 = start + 1; v2 < v; v2++ )
                            {
                                pts[h][v2].r = NONE;
                                pts[h][v2].l = NONE;
                            }
                        }
                    }
                    if (isWall || ( pt.l == HARD && pt.r != HARD) )
                    {
                        start = v;
                        startWall = isWall;
                    }
                }
            }
        }
        // ..now vertically
        for (int v = 0; v < pts[0].length; v++)
        {
            int start = -1;
            boolean startWall = false;

            for (int h = 0; h < pts.length; h++)
            {
                Pt pt = pts[h][v];
                if (pt.hasHard())
                {
                    boolean isWall =  pt.l == HARD && pt.r == HARD && pt.t != HARD && pt.b != HARD;

                    if (isWall || ( pt.t != HARD && pt.b == HARD ) )
                    {
                        if ( isWall && startWall && start >= 0 )
                        {
                            pts[start][v].b = NONE;
                            pts[h][v].t = NONE;
                            for ( int h2 = start + 1; h2 < h; h2++ )
                            {
                                pts[h2][v].b = NONE;
                                pts[h2][v].t = NONE;
                            }
                        }
                    }
                    if (isWall || ( pt.t == HARD && pt.b != HARD) )
                    {
                        start = h;
                        startWall = isWall;
                    }
                }
            }
        }

        // box up non-none corners into a rect array
        for ( int h = 0; h < horiz.size(); h++ )
            for ( int v = 0; v < vert.size(); v++ )
            {
                if (pts[h][v].isTL())
                {
                    vTwo:
                    for (int v2 = v+1; v2 < vert.size(); v2++)
                    {
                        if (pts[h][v2].isTR())
                        {
                            hTwo:
                            for (int h2 = h+1; h2 < horiz.size(); h2++)
                            {
                                if (pts[h2][v2].isBR())
                                {
                                    Pt pt2 = pts[h2][v2], pt1 = pts[h][v];

                                    rects.add( new DRectangle( pt1.x, pt1.y,
                                            pt2.x - pt1.x,
                                            pt2.y - pt1.y ) );

                                    break hTwo;
                                }
                            }
                            break vTwo;
                        }
                    }
                }
            }

        Iterator<DRectangle> dit = this.rects.iterator();
        while (dit.hasNext())
        {
            if (dit.next().area() == 0)
                dit.remove();
        }
    }

    public List<DRectangle> getCopyOResults()
    {
        List<DRectangle> out = new ArrayList();
        for ( DRectangle dr : rects )
            out.add( new DRectangle( dr ) );
        return out;
    }
    
    static class Pt
    {
        List<DRectangle> out = new ArrayList();
        FType l, r, b, t;
        double x, y;

        Pt( double x, double y, FType l, FType r, FType t, FType b )
        {
           this.l = l;
           this.r = r;
           this.t = t;
           this.b = b;
           this.x = x;
           this.y = y;
        }

        public boolean isTL()
        {
            return r != NONE && b != NONE;
        }

        public boolean isTR()
        {
            return l != NONE && b != NONE;
        }

        public boolean isBR()
        {
            return t != NONE && l != NONE;
        }

        private boolean isCorner()
        {
            return
                    (t == HARD && r == HARD) ||
                    (r == HARD && b == HARD) ||
                    (b == HARD && l == HARD) ||
                    (l == HARD && t == HARD);
        }

        private boolean hasHard()
        {
            return t == HARD || b == HARD || l == HARD || r == HARD;
        }
    }

    private void addTo( Map<Double, RC> horiz, double index, double start, double end )
    {
        RC rc = horiz.get( index );
        if ( rc == null )
        {
            horiz.put (index, rc = new RC( index ));
        }

        rc.add( start, end );

    }

    static enum FType { NONE, HARD, SOFT;

        FType union( FType two )
        {
            for (FType f : Arrays.asList( HARD, SOFT, NONE))
            {
                if (this == f || two == f)
                    return f;
            }
            return null;
        }

    };

    public static class Following implements Comparable<Following>
    {
        double value;
        FType type = NONE;
        Following (Double value, FType type)
        {
            this.type = type;
            this.value = value;
        }

        public int compareTo( Following o )
        {
            return Double.compare( value, o.value );
        }
    }
    
    public static class RC
    {
        double val;

        // we assume that +-oo is outside
        List<Following> boundaries = new ArrayList();

        RC(double val)
        {
            this.val = val;
            boundaries.add( new Following( -Double.MAX_VALUE, SOFT));
        }

        // only works with hard and soft (eg not none)
        public void add (double startIn, double endIn)
        {
            assert (startIn < endIn);

            Iterator<Following> fit = boundaries.iterator();
            // remove boundaries between startIn and endIn
            while (fit.hasNext())
            {
                Following next = fit.next();
                if (next.value >= startIn && next.value <= endIn)
                    fit.remove();
            }


            boundaries.add( new Following( startIn, HARD ) );
            boundaries.add( new Following( endIn, SOFT ) );

            Collections.sort (boundaries);

            fit = boundaries.iterator();
            Following last = boundaries.get( 0 );
            fit.next();
            // remove consecutive identical entries
            while (fit.hasNext())
            {
                Following current = fit.next();
                if (current.type == last.type)
                {
                    fit.remove(); // remove current
                    if (current.type == SOFT)
                        last.value = current.value; // but propogate last value if end
                }
                else
                    last = current;
            }
        }

        private Pair<FType, FType> getPairsAt( double v )
        {
            int res = Collections.binarySearch( boundaries, new Following( v, NONE ) ); // NONE==dummy
            if (res < 0)
            {
                FType out;
                if (-(res + 1) >= boundaries.size())
                    out = boundaries.get( boundaries.size()-1).type; // shoudl always be soft?
                else
                    out = boundaries.get( - (res+1) -1  ).type;
                return new Pair<FType, FType> (out, out);
            }
            else
            {
                assert (res != 0); // should be -Double.max
                return new Pair<FType, FType> (boundaries.get( res-1 ).type, boundaries.get( res ).type);
            }
        }

        private void merge( RC other )
        {
            Following last = null;
            for (Following fw : other.boundaries)
            {
                if (last != null)
                {
                    if (last.type == HARD && fw.type == SOFT)
                        add( last.value, fw.value );
                }
                last = fw;
            }
        }
    }


}
