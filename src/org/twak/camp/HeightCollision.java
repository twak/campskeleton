
package org.twak.camp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.tinspin.index.Index.PointEntryKnn;
import org.tinspin.index.Index.PointIteratorKnn;
import org.tinspin.index.PointMap;
import org.tinspin.index.kdtree.KDTree;
import org.twak.utils.Pair;
import org.twak.utils.collections.CloneConfirmIterator;
import org.twak.utils.collections.ConsecutivePairs;

/**
 * A bunch of faces that collide at the same height
 * @author twak
 */
public class HeightCollision implements HeightEvent
{
    double height;
    List<EdgeCollision> coHeighted;

    private Set<Corner> newHoriz = new LinkedHashSet();

    /**
     * @param coHeighted.size() > 1
     */
    public HeightCollision(){};
    public HeightCollision( List<EdgeCollision> coHeighted )
    {
        this.coHeighted = coHeighted;
        height = coHeighted.get( 0 ).getHeight();
    }

    public double getHeight()
    {
        return height;
    }

	/**
	 * This is a bit of quest!
	 * <p>
	 * Processes collisions occurring at the same height to update the topology of
	 * the given skeleton.
	 * <p>
	 * Assumption is that there are no parallel edges creating horizontal bisectors
	 * in the current loops. We create some here, then process them all, again
	 * removing all horizontal bisectors from the current loops.
	 *
	 * @return true if topology has changed; false otherwise (we ignored all events)
	 */
	public boolean process(Skeleton skel) {
		if (coHeighted.isEmpty()) {
			processHoriz(skel);
            return false;
        }
        
        boolean changed = false;
        
        PointMap<CoSitedCollision> kdTree = KDTree.create(2);
        final double tolerance = 0.01;
        
        // coSited will store collisions that were not merged
        List<CoSitedCollision> coSited = new ArrayList<>();
        
        // Use the first collision to seed the kd-tree
        EdgeCollision first = coHeighted.get(0);
        double[] firstPoint = { first.loc.x, first.loc.y };
        CoSitedCollision firstCollision = new CoSitedCollision(first.loc, first, this);
        kdTree.insert(firstPoint, firstCollision);
        coSited.add(firstCollision);
        
        // Process remaining collisions
        int len = coHeighted.size();
        for (int i = 1; i < len; i++) {
            EdgeCollision ec = coHeighted.get(i);
            double[] qp = { ec.loc.x, ec.loc.y };
            
            // Query the KD-Tree for the nearest neighbor
            PointEntryKnn<CoSitedCollision> nearest = kdTree.query1nn(qp);
            if (nearest.dist() < tolerance) {
                nearest.value().add(ec);
            } else {
                CoSitedCollision newCollision = new CoSitedCollision(ec.loc, ec, this);
                kdTree.insert(qp, newCollision);
                coSited.add(newCollision);
            }
        }
        
        // Step 1: Remove collisions that fail the chain finding
        coSited.removeIf(css -> !css.findChains(skel));
        
        // Step 2: Process remaining chains
        skel.qu.holdRemoves();
        for (CoSitedCollision css : coSited) {
            css.validateChains(skel);
            changed |= css.processChains(skel);
        }
        skel.qu.resumeRemoves();
        
        processHoriz(skel);
        return changed;
    }
    
    /**
     * Process horizontals.
     *
     * assumes that all corners with horizontal bisectors are at the same height(?), and
     * that all faces that need to be merged, have been.
     *
     * @param skel
     */
    public void processHoriz (Skeleton skel)
    {
        Set<Chain> chains = new LinkedHashSet();
        while (newHoriz.size() > 0)
            chains.add( CoSitedCollision.buildChain2( newHoriz.iterator().next(), newHoriz ) );

        if (chains.size() == 0)
            return;

        // if there are two lines of events at the same hight (but different lines), we need them to share their end points.
        Set<Corner> intraface = new LinkedHashSet();

        for (Chain chain : chains)
        {
//            if (chain.chain.isEmpty())
//                continue;
            List<Edge> priority = new ArrayList();
            for (Corner c : chain.chain)
            {
                // both edges are parallel - these are the only corners added to newHoriz...
                priority.add( c.nextL );
                priority.add( c.prevL );
            }

            // find a set of coplanar edges that survive this transition in winners (equal highest priority)
            Comparator<Edge> hComp = skel.getHorizontalComparator();
            Collections.sort( priority, hComp );
            Set<Edge> winners = new LinkedHashSet();
            Edge winner = priority.remove( 0 );
            winners.add( winner );
            while ( !priority.isEmpty() && hComp.compare( winner, priority.get( 0 ) ) == 0 )
                winners.add( priority.remove( 0 ) );

            // if first edge needs an additional corner - "if we're adding a cap at the start" and "first isn't already an interface"
            Corner first = chain.chain.get( 0 );
            if (!winners.contains( first.prevL ) ) //skel.liveCorners.contains(first)
            {
                if (!intraface.contains( first.prevC )) // hasn't already been raised up by a previous chain
                {
//                    Tuple3d res =//new LinearForm3D( 0, 0, 1, -first.z ).collide( first.prevL.linearForm, first.prevC.prevL.linearForm );
                    Corner newFirst = new Corner(  Edge.collide(first.prevC, first.z) );
                    skel.output.addOutputSideTo( first.prevC, newFirst, first.prevL, first.prevC.prevL );
                    Corner.replace( first.prevC, newFirst, skel );

                    chain.chain.add( 0, newFirst);
                    intraface.add (newFirst);
                    first = newFirst;
                }
                else
                    chain.chain.add( 0, first.prevC );
            }
            else
            {
                // the edge before the first point is a winner, add it
                chain.chain.add( 0, first = first.prevC );
            }
            Corner last = chain.chain.get(chain.chain.size()-1);
            // if last edge needs an additional corner
            if ( !winners.contains( last.nextL ) )
            {
                if ( !intraface.contains( last.nextC ) ) // hasn't already been raised up by a previous chain
                {
//                    Tuple3d res = new LinearForm3D( 0, 0, 1, -last.z ).collide( last.nextL.linearForm, last.nextC.nextL.linearForm );
                    Corner newLast = new Corner( Edge.collide(last.nextC, last.z) );
                    skel.output.addOutputSideTo( last.nextC, newLast, last.nextL, last.nextC.nextL );
                    Corner.replace( last.nextC, newLast, skel );

                    chain.chain.add( newLast );
                    intraface.add( newLast );
                    last = newLast;
                }
                else
                    chain.chain.add( last.nextC );
            }
            else
            {
                // the edge after the last point is a winner, add it
                chain.chain.add( last = last.nextC);
            }

            for (Pair<Corner, Corner> pair : new ConsecutivePairs<Corner>( chain.chain, false))
            {
                Corner s = pair.first(), e = pair.second();
                assert (s.nextL == e.prevL);
                // if this is the edge that spreads out over all others
                if (winners.contains( s.nextL ))
                {
                    if (s.nextL != winner)
                    {
                        skel.output.merge( winner.start, s ); // assumes start of edge forms part of it's output
                    }
                    s.nextL.currentCorners.remove( e );
                    s.nextL.currentCorners.remove( s );
                }
                else
                {
                    // this (section of this ) edge ends at this height
                    s.nextL.currentCorners.remove( s );
                    s.nextL.currentCorners.remove( e );
                    skel.output.addOutputSideTo( true, s, e, s.nextL, winner );
                }

                skel.liveCorners.remove( s ); // add in first and last below
                skel.liveCorners.remove( e );
            }

            skel.liveCorners.add( first );
            skel.liveCorners.add( last );

            winner.currentCorners.add ( first );
            winner.currentCorners.add ( last );

            first.nextC = last;
            last.prevC = first;
            first.nextL = winner;
            last.prevL = winner;

            for (Corner c : chain.chain)
            {
                if (c.nextL.currentCorners.size() == 0)
                {
                    skel.liveEdges.remove( c.nextL );
                }
            }
        }
        
        // no need to recalculate events - no faces added. wrong!- any new connectivity needs to be flagged as loop-of-two etc...
        skel.qu.clearFaceEvents();
        for ( Corner lc : new CloneConfirmIterator<Corner>(skel.liveCorners) )
            skel.qu.addCorner( lc, this );

        // can't think of a case wher ethis could happen. could iterate in...
        assert( newHoriz.size() == 0 );

        skel.validate();
    }


    public void newHoriz( Corner toAdd )
    {
        newHoriz.add( toAdd );
    }

    @Override
    public String toString()
    {
        return "collisions at "+height;
    }
}
