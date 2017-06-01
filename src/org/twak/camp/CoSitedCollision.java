
package org.twak.camp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;
import org.twak.utils.ConsecutivePairs;
import org.twak.utils.ConsecutiveTriples;
import org.twak.utils.Line3D;
import org.twak.utils.LinearForm3D;
import org.twak.utils.Pair;
import org.twak.utils.Triple;

/**
 * A bunch of faces that collide at one point
 * @author twak
 */
public class CoSitedCollision
{
    public Set<EdgeCollision> edges = new LinkedHashSet();
    public Point3d loc;

    public boolean debugHoriz = false;

    public List<Chain> chains = new ArrayList();

    private HeightCollision parent;

    public CoSitedCollision( Point3d loc, EdgeCollision ec, HeightCollision parent )
    {
        this.loc= loc;
        this.parent = parent;
        add( ec );
    }

    public void add(EdgeCollision ec)
    {
        edges.add( ec );
    }

    /**
     * New routine
     * @return true if valid chains found at this site
     */
    public boolean findChains ( Skeleton skel )
    {
        chains = new ArrayList();
        
        // remove duplicate edges
        Set<Edge> allEdges = new LinkedHashSet();
        for (EdgeCollision ec : edges)
        {
            allEdges.add( ec.a );
            allEdges.add( ec.b );
            allEdges.add( ec.c );
        }

        Iterator<Edge> eit = allEdges.iterator();
        while ( eit.hasNext() )
            if ( !skel.liveEdges.contains( eit.next() ) )
                eit.remove();

        if (allEdges.size() < 3)
            return false;
            // fixme: why do we add all starting corners? just to filter them later?
        Set<Corner> edgeStarts = new LinkedHashSet();
        for ( Edge e : allEdges )
            for ( Corner c : e.currentCorners )
                if ( c.nextL == e )
                    edgeStarts.add( c );


        while (!edgeStarts.isEmpty())
        {
            Corner start = edgeStarts.iterator().next();
            Chain chain = buildChain2( start, edgeStarts );
            if (chain != null)
                chains.add( chain );
        }

        edgeStarts.clear();
        for (Chain c : chains)
            if (c.chain.size() > 1)
                edgeStarts.addAll( c.chain );


        Iterator<Chain> chit = chains.iterator();
        while (chit.hasNext())
        {
            Chain chain = chit.next();
            if (chain.chain.size() == 1)
            {
                // first corner of edge is not necessarily the corner of the edge segment bounding the collision
                Corner s = chain.chain.get( 0 ); 
                Corner found = EdgeCollision.findCorner( s.nextL, loc, skel );
//                if (found != null && !edgeStarts.contains( found ))
                // fixme: because we (strangely) add all the chain starts above, we can just check if it's unchanged...
                if (found == s && !edgeStarts.contains( found ))
                {
//                    chain.chain.clear();
//                    chain.chain.add( found );
                }
                else
                    chit.remove();
            }
        }

        // while still no-horizontals in chains (there may be when dealing with multiple
        // sites at one height), process chains to a counter clockwise order
        if (chains.size() > 1) // size == 1 may have parallels in it (run away!)
            Collections.sort( chains, new ChainComparator( loc.z ) );

        return true;
    }

    /**
     * If another collision has been evaluated at teh same height, this method
     * checks for any changes in the Corners involved in a skeleton. This is a problem
     * when several collisions at the same height occur against one smash edge.
     *
     * If the chain has length > 1, then the lost corner can be recorvered using
     * the following corner (the corners central to the collision, every one after
     * the first will all remain valid at one height).
     *
     * If the chain has length 1, we're in for a bit of a trek.
     *
     * We can skip the finding edges part if the current height only has one
     * collision?
     *
     * @param skel
     */
    public void validateChains (Skeleton skel)
    {
        // in case an edge has been removed by a previous event at this height
        for ( Chain c : chains )
        {
            if (c.loop)
                continue; // nothing to do here

            if (c.chain.size() > 1) // previous
            {
                c.chain.remove( 0 );
                c.chain.add( 0, c.chain.get( 0 ).prevC );
            }
            else // smash edge, search for correct corner (edges not in liveEdges removed next)
            {
                /**
                 * This covers the "corner situation" where a concave angle creates creates
                 * another loop with it's point closer to the target, than the original loop.
                 *
                 * It may well break down with lots of adjacent sides.
                 */
//                broken:(
                
//                if ( e.currentCorners.contains( s ) )
//                    continue; // nothing to see here, move along!

                Corner s = c.chain.get( 0 );
                Edge e = s.nextL;
                

                Line3D projectionLine = new Line3D (loc, e.direction() );

                LinearForm3D ceiling = new LinearForm3D( 0, 0, 1, -loc.z );
                // project start onto line of collisions above smash edge
                try
                {
                    Tuple3d start = e.linearForm.collide( s.prevL.linearForm, ceiling );

                    // line defined using collision point, so we're finding the line before 0
                    double targetParam = 0;

                    // we should only end with start if it hasn't been elevated yet
                    Corner bestPrev = s;
                    // ignore points before start (but allow the first point to override start!)
                    double bestParam = projectionLine.projectParam( start ) - 0.001;

                    // parameterize each corner in e's currentCorners by the line
                    for ( Corner r : e.currentCorners )
                        if ( r.nextL == e )
                        {
                            // parameterize
                            Tuple3d rOnHigh = Math.abs( r.z - loc.z ) < 0.001 ? r : ceiling.collide( r.prevL.linearForm, r.nextL.linearForm );
                            double param = projectionLine.projectParam( rOnHigh );
                            // if this was the previous (todo: does this want a tolerance on < targetParam? why not?)
                            if ( param > bestParam && param <= targetParam )
                            {
                                bestPrev = r;
                                bestParam = param;
                            }
                        }

                    c.chain.remove( 0 );
                    c.chain.add( 0, bestPrev );

                    // might have formed a loop
                    c.loop = c.chain.get( c.chain.size() - 1 ).nextC == c.chain.get( 0 );

                }
                catch ( Throwable t )
                {
                    t.printStackTrace();
//                    System.err.println( "didn't like colliding " + e + "and " + s.prevL );
                    continue;
                }
            }
        }


        Map<Edge, Corner> edgeToCorner = new LinkedHashMap();
        for (Chain cc : chains)
            for (Corner c : cc.chain)
                edgeToCorner.put( c.nextL, c);
        
        
        // Find valid triples ~ now topology is as it will be before evaluation, we
        // can check that the input edge triplets still have two consecutive edges.
         
        Set<Edge> validEdges = new LinkedHashSet();
        for (EdgeCollision ec : edges)
        {
            // todo: adjacent pairs may not be parallel!
            if (hasAdjacent(
                    edgeToCorner.get( ec.a ),
                    edgeToCorner.get( ec.b ),
                    edgeToCorner.get( ec.c ) ))
            {
                if (    skel.liveEdges.contains( ec. a ) &&
                        skel.liveEdges.contains( ec. b ) &&
                        skel.liveEdges.contains( ec. c ) )
                {
                    validEdges.add( ec.a );
                    validEdges.add( ec.b );
                    validEdges.add( ec.c );
                }
            }
        }

        List<Chain> chainOrder = new ArrayList ( chains );

        // remove parts of chains that aren't a valid triple.
        for (Chain cc : chainOrder)
        {
            // remove and split
            chains.addAll( chains.indexOf( cc ), cc.removeCornersWithoutEdges ( validEdges ) );
        }

        // kill 0-length chains
        Iterator<Chain> ccit  = chains.iterator();
        while (ccit.hasNext())
        {
            if (ccit.next().chain.size() == 0)
                ccit.remove();
        }

    }

    private boolean hasAdjacent (Corner a, Corner b, Corner c)
    {
        if (a== null || b == null || c == null)
            return false;

        if ( (a.nextC == b || a.nextC == c) ) // todo: speedup by puting consec in a,b always?
            return true;
        if ( (b.nextC == c || b.nextC == a) )
            return true;
        if ( (c.nextC == a || c.nextC == b) )
            return true;
        
        return false;
    }

    public boolean processChains(Skeleton skel)
    {
        if (moreOneSmashEdge()) // no test example case showing this is required?
            return false;

        Set<Corner> allCorners = new LinkedHashSet();
        for (Chain cc : chains)
        {
            allCorners.addAll( cc.chain ); //cc.chain.get(0).nextL.currentCorners
        }

        // after all the checks, if there are less than three faces involved, it's not a collision any more
        if (allCorners.size() < 3)
            return false;

        skel.debugCollisionOrder.add( this );

        Iterator<Chain> cit = chains.iterator();
        while (cit.hasNext())
        {
            Chain chain = cit.next(); // chain.chain.get(2).nextL

            for ( Pair<Corner, Corner> p : new ConsecutivePairs<Corner>( chain.chain, chain.loop ) )
            {
//                System.out.println( "proc consec " + p.first() + " and " + p.second() );
                EdgeCollision.processConsecutive( loc, p.first(), p.second(), skel );
            }

            // remove the middle faces in the loop from the list of live corners, liveEdges if
            // there are no more live corners, and the liveCorners list
            if ( chain.chain.size() >= 3 )
            {
                Iterator<Triple<Corner, Corner, Corner>> tit = new ConsecutiveTriples<Corner>( chain.chain, chain.loop );
                while ( tit.hasNext() )
                {
                    Edge middle = tit.next().second().nextL;

                    // face no longer referenced, remove from list of live edges 
                    if ( middle.currentCorners.isEmpty() )
                        skel.liveEdges.remove( middle );
                }
            }

            if ( chain.loop )
                cit.remove();
        }

        // was entirely closed loops
        if ( chains.isEmpty() )
            return true;

        // connect end of previous chain, to start of next

        // in case we are colliding against a smash (no-corner/split event)-edge, we cache the next-corner before
        // any alterations
        Map<Corner, Corner> aNext = new LinkedHashMap();
        for ( Chain chain : chains )
        {
            Corner c = chain.chain.get( chain.chain.size() -1 );
            aNext.put( c, c.nextC );
        }

        // process intra-chain collisions (non-consecutive edges)
        for ( Pair<Chain, Chain> adjacentChains : new ConsecutivePairs<Chain>( chains, true ) )
        {
            List<Corner> first = adjacentChains.first().chain;
            Corner a = first.get( first.size() -1 );
            Corner b = adjacentChains.second().chain.get( 0 );
            EdgeCollision.processJump( loc, a, aNext.get( a ), b, skel, parent );
        }
        
        return true; 
    }


    /**
     * Is this actually needed in any of the examples?
     */
    private boolean moreOneSmashEdge()
    {
        // if two chains have length one, this is not a valid collision point
        int oneCount = 0;
        for (Chain ch : chains)
            if (ch.chain.size() == 1)
                oneCount++;
        if (oneCount > 1)
            return false;

        return oneCount > 1;
    }

    public static Chain buildChain2( Corner start, Set<Corner> input ) // start.nextL  start.prevL
    {
        List<Corner> chain = new ArrayList();

        // check backwards
        Corner a = start;
        while ( input.contains( a ) )
        {
            chain.add( 0, a );
            input.remove( a );
            a = a.prevC;
        }

        // check forwards
        a = start.nextC;
        while ( input.contains( a ) )
        {
            chain.add( a );
            input.remove( a );
            a = a.nextC;
        }

        return new Chain (chain);
    }

    /**
     * Defines order by the angle the first corner in a chain makes with the second
     * against a fixed axis at a specified height.
     */
    static Vector3d Y_UP = new Vector3d( 0, 1, 0 );

    public class ChainComparator implements Comparator<Chain>
    {
        double height;
//        LinearForm3D ceiling;

        public ChainComparator (double height)
        {
            this.height = height;
//            this.ceiling = new LinearForm3D( 0, 0, 1, -height );
        }
        
        public int compare( Chain o1, Chain o2 )
        {
            Corner c1 = o1.chain.get( 0 );
            Corner c2 = o2.chain.get( 0 );

            // except for the first and and last point
            // chain's non-start/end points are always at the position of the collision - so to
            // find the angle of the first edge at the specified height, we project it's start
            // coordinate the desired height and take the angle relative to the collision
            // !could speed up with a chain-class that caches this info!

//            try
//            {
                Tuple3d p1 = Edge.collide(c1, height);//ceiling.collide( c1.prevL.linearForm, c1.nextL.linearForm );
                Tuple3d p2 = Edge.collide(c2, height );//ceiling.collide( c2.prevL.linearForm, c2.nextL.linearForm );

                p1.sub( loc );
                p2.sub( loc );

                // start/end line is (+-)Pi
                return Double.compare(
                        Math.atan2( p1.y, p1.x ),
                        Math.atan2( p2.y, p2.x ) );
//            }
//            catch (RuntimeException e)
//            {
//                // we can probably fix these up (by assuming that they're horizontal?)
//                // todo: can we prove they are safe to ignore? eg: no parallel edges inbound, none outbound etc..
//                 System.err.println( "didn't like colliding 1" + c1.prevL + " and " + c1.nextL );
//                 System.err.println( "                      2" + c2.prevL + " and " + c2.nextL );
//                 return 0;
//            }
        }
    }

    public double getHeight()
    {
        return loc.z;
    }

    @Override
    public String toString()
    {
        StringBuilder sb= new StringBuilder("{");
        for (EdgeCollision e :edges)
            sb.append( e +",");
        sb.append( "}");
        return sb.toString();
    }
}
