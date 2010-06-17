package straightskeleton;


import javax.vecmath.Point3d;
import utils.LinearForm3D;

/**
 *
 * @author twak
 */
public class EdgeCollision implements HeightEvent
{
    public Point3d loc;
    public Edge a,b,c;

    public boolean debugInfinite = false;
    
    public EdgeCollision (Point3d location, Edge e1, Edge e2, Edge e3)
    {
        this.loc = location;

        a = e1;b=e2;c=e3;
    }

    @Override
    public boolean equals( Object obj )
    {
        if (obj instanceof EdgeCollision)
        {
            EdgeCollision other = (EdgeCollision)obj;

            return // if this is a bottleneck we could reject quickly with a hash compare?
            (a.equals( other.a ) &&
                        ((b.equals( other.b ) && c.equals( other.c ) ) ||
                         (b.equals( other.c ) && c.equals( other.b ) ) )) ||
            (a.equals( other.b ) &&
                        ((b.equals( other.a ) && c.equals( other.c ) ) ||
                         (b.equals( other.c ) && c.equals( other.a ) ) )) ||
            (a.equals( other.c ) &&
                        ((b.equals( other.a ) && c.equals( other.b ) ) ||
                         (b.equals( other.b ) && c.equals( other.a ) ) ) );


        }
        return false;
    }

    /**
     * Hash is agnostic to which edge is in a, b and c
     * @return
     */
    @Override
    public int hashCode()
    {
        int hash = 3;
        hash += ( this.a != null ? this.a.hashCode() : 0 );
        hash += ( this.b != null ? this.b.hashCode() : 0 );
        hash += ( this.c != null ? this.c.hashCode() : 0 );
        return hash * 31;
    }

    public double getHeight()
    {
        return loc.z;
    }

    /**
     * Three way collisions are delt with in CoSitedCollision
     */
    public boolean process( Skeleton skel )
    {
        throw new Error();
    }

    public static void processConsecutive (
            Point3d loc,
            Corner a, Corner b,
            Skeleton skel )
    {
            // add line b -> loc
            skel.output.addOutputSideTo (b,loc, b.prevL, b.nextL );

            // remove b from edge's map
            a.nextL.currentCorners.remove( b );
            b.nextL.currentCorners.remove( b ); 

            skel.liveCorners.remove( b );
    }

    public static void processJump(
            Point3d loc,
            Corner a, Corner an, Corner b,
            Skeleton skel, HeightCollision hc )
    {
        Corner ab = new Corner( loc.x, loc.y, loc.z );
        ab.prevL = b.nextL;
        ab.prevL.currentCorners.add( ab );

        ab.nextL = a.nextL;
        ab.nextL.currentCorners.add( ab );

        // take's A's place in the loop
        an.prevC = ab;// where it breaks down without an...
        b.nextC = ab;
        ab.prevC = b;
        ab.nextC = an; // ..and here

        skel.liveCorners.add( ab );

        // check for new collisions
        skel.qu.addCorner( ab, hc ); // we could optimize and move to after having removed from liveEdges? (commented out - above)

    }

    /**
     * @return the loop corner whose nextL points to the given edge and whose
     * bisectors for the edge contain the collision.
     */
    public static Corner findCorner (Edge in, Point3d collision, Skeleton skel)
    {
        for (Corner lc : in.currentCorners)
        {
                if ( lc.nextL == in )
                {
                    // the two edges that form the bisector with this edge
                    LinearForm3D prev = lc.prevC.nextL.linearForm.clone(); // clone not needed now?
                    LinearForm3D next = lc.nextC.nextL.linearForm.clone();

                    double pDist = prev.pointDistance( collision ),
                           nDist = next.pointDistance( collision );

                    double prevDot = prev.normal().dot( in.direction() ),
                           nextDot = next.normal().dot( in.direction() );

                    // depending on if the angle is obtuse or reflex, we'll need to flip the normals
                    // to the convention that a point with a positive plane distance is on the correct side of both bisecting planes

                    if ( prevDot < 0 ) // should only be 0 if two edges are parallel!
                        pDist = -pDist;
                    if ( nextDot > 0 )
                        nDist = -nDist;

                    // important constant - must prefer to accept rather than "leak" a collision
                    final double c = -0.0001;

                    if ( pDist >= c && nDist >= c ) // a bit of slack!
                        return lc;
                }
        }
        return null; // no candidates
    }

    @Override
    public String toString()
    {
        return loc + ":"+a +","+ b +"," + c;
    }
}
