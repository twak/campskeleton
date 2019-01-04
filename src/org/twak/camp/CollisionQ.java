/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.twak.camp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;

import org.twak.camp.debug.DebugDevice;

/**
 *
 * @author twak
 */
public class CollisionQ
{
    /**
     * Collisions between edges
     */
    private PriorityQueue<EdgeCollision> faceEvents;
    /**
     * Other control events (gradient changes...)
     */
    private PriorityQueue<HeightEvent> miscEvents;


    Skeleton skel;

    // this is an acceleration structure - we don't process seen triples of edges twice. A similar structure occurs in Skeleton ~ they need merging...
    private Set<EdgeCollision> seen = new HashSet<>();

    /**
     * @param corners the input set of corners
     * @param liveEdges the (continuously updated) set of edges that still feature
     * in the live corner list. Caller has to update this set :)
     */
    public CollisionQ( Skeleton skel )
    {
        this.skel = skel;
        faceEvents = new PriorityQueue<EdgeCollision> (Math.max (3, skel.liveCorners.size()), HeightEvent.heightComparator);
        miscEvents = new PriorityQueue<HeightEvent> (Math.max( 3, skel.liveCorners.size() ), HeightEvent.heightComparator);
    }

    private HeightEvent nextEvent()
    {
        EdgeCollision ec;
        
        for (;;)
        {
            ec = faceEvents.poll();
            if (ec == null)
                break;
            // valid if we haven't seen it, and it's height is "greater" than the current skeleton height
            if ( !skel.seen.contains( ec ) && ec.loc.z - skel.height > -0.001 )
                break;
        }
        
        HeightEvent he = miscEvents.peek();

        if ( ec == null )
        {
            return miscEvents.poll(); // might be null!
        }
        if ( he == null )
        {
            skel.seen.add( ec );
            return ec; // might be null!
        }

        if ( he.getHeight() <= ec.getHeight() )
        {
            faceEvents.add( ec );
            return miscEvents.poll(); // return he
        }
        else
        {
            skel.seen.add( ec );
            return ec; // return ec
        }
    }

    HeightCollision currentCoHeighted = null;

    public HeightEvent poll()
    {
        currentCoHeighted = null; // now working at a new height

        HeightEvent next = nextEvent();

        if (next instanceof EdgeCollision)
        {
            List<EdgeCollision> coHeighted = new ArrayList<>();
            EdgeCollision ec = (EdgeCollision)next;
            coHeighted.add( ec );

            double height = ec.getHeight();

            for (;;)
            {
                EdgeCollision higher = faceEvents.peek();
                if (higher == null)
                    break;
                if (higher.getHeight() - height <  0.00001 )// ephemeral random constant #34 was 0.00001
                {
                    faceEvents.poll(); //same as higher

                    if (skel.seen.contains( higher ))
                        continue;
                    
                    height = higher.getHeight();
                     skel.seen.add( higher );
                    coHeighted.add( higher );
                }
                else break;
            }

            return currentCoHeighted = new HeightCollision ( coHeighted );
        }
        else return next;
    }

    public void add(HeightEvent he)
    {
        if (he instanceof EdgeCollision)
            faceEvents.add( (EdgeCollision) he );
        else
            miscEvents.add( he );
    }


    /**
     * Collide the new edge (toAdd.prev, toAdd.next) against
     * all other edges. Will also add 3 consecutive edges.
     * 
     * @param toAdd
     */
    public void addCorner( Corner toAdd, HeightCollision postProcess )
    {
        addCorner(toAdd, postProcess, false);
    }
    
    public void addCorner( Corner toAdd, HeightCollision postProcess, boolean useCache )
    {
        // check these two edges don't share the same face
        if ( !skel.preserveParallel && toAdd.prevL.sameDirectedLine( toAdd.nextL ) )
        {
            removeCorner( toAdd );
            return;
        }
        
        // loop of two - dissolves to a ridge
        if ( toAdd.prevL == toAdd.nextC.nextL )
        {
            skel.output.addOutputSideTo ( toAdd, toAdd.nextC, toAdd.prevL, toAdd.nextL );

            toAdd.nextL.currentCorners.remove( toAdd ); // we really should automate this
            toAdd.nextL.currentCorners.remove( toAdd.nextC );
            toAdd.prevL.currentCorners.remove( toAdd );
            toAdd.prevL.currentCorners.remove( toAdd.nextC );

            if ( toAdd.nextL.currentCorners.isEmpty() )
                skel.liveEdges.remove( toAdd.nextL );

            if ( toAdd.prevL.currentCorners.isEmpty() )
                skel.liveEdges.remove( toAdd.prevL );

            skel.liveCorners.remove( toAdd );
            skel.liveCorners.remove( toAdd.nextC );
            return;
        }

        // Horizontal bisectors are rounded up and evaluated before leaving the current height event
        if ( toAdd.prevL.isCollisionNearHoriz( toAdd.nextL ) )
        {
            // if not a peak, add as a unsolved horizontal bisector
            if (toAdd.nextL.direction().angle( toAdd.prevL.direction() ) < 0.01 )
                postProcess.newHoriz( toAdd );
            // if just a peak, assume the loops-of-two-rule will finish it awf
            return;
        }

        for (Edge e : skel.liveEdges)
        {
            EdgeCollision ex = new EdgeCollision(null, toAdd.prevL, toAdd.nextL, e);
            if ((!useCache) || !seen.contains(ex))
            {
                seen.add(ex);
                cornerEdgeCollision( toAdd, e );
            }
        }
    }
    
    private static boolean isParallel (Edge a, Edge b) {
    	return a.uphill.angle( b.uphill ) < 0.0001 && a.direction().angle( b.direction() ) < 0.0001;
    }

    private void cornerEdgeCollision( Corner corner, Edge edge )
    {
            // check for the uphill vector of both edges being too similar (parallel edges)
            // also rejects e == corner.nextL or corner.prevL updated to take into account vertical edges - will always have same uphill! - (so we check edge direction too)
            
		if ( skel.preserveParallel ) {
//			if ( edge.start.equals( corner ) || edge.end.equals( corner ) )
//				return;
			
			if ( isParallel (edge, corner.prevL) && isParallel( edge, corner.nextL ) )
				return;
			
			if ( corner.nextL == edge || corner.prevL == edge)
				return;
			
		} else {
			if ( isParallel (edge, corner.prevL) || isParallel( edge, corner.nextL ) )
//					( edge.uphill.angle( corner.prevL.uphill ) < 0.0001 && edge.direction().angle( corner.prevL.direction() ) < 0.0001 ) ||
//				 ( edge.uphill.angle( corner.nextL.uphill ) < 0.0001 && edge.direction().angle( corner.nextL.direction() ) < 0.0001 ) )
				return;
		}
    	
            Tuple3d res = null;
            try
            {
                // sometimes locks up here if edge.linear form has NaN components.
                if ( corner.prevL.linearForm.hasNaN() || corner.nextL.linearForm.hasNaN() || edge.linearForm.hasNaN())
                    throw new Error();
                res =edge.linearForm.collide( corner.prevL.linearForm, corner.nextL.linearForm );
            }
            catch ( Throwable f )
            {
            	if (skel.preserveParallel ) {
            		
            		if ( corner.prevL.uphill.equals(edge.uphill) && corner.prevC.prevL.equals( edge ))
            			res = corner.nextL.linearForm.collide( corner.prevC, corner.prevL.uphill );
            		else if (corner.nextL.uphill.equals( edge.uphill ) && corner.nextC.nextL.equals( edge ))
            			res = corner.prevL.linearForm.collide( corner.nextC, corner.nextL.uphill );
            		else if (corner.nextL.uphill.equals( corner.prevL.uphill ) )
            			res = edge.linearForm.collide( corner, corner.nextL.uphill );
            	}
            }

            if ( res != null )
            {
                // cheap reject: if collision is equal or below (not the correct place to check) the corner, don't bother with it
                if ( res.z < corner.z || res.z < edge.start.z )
                    return;

                EdgeCollision ec =
                        new EdgeCollision( new Point3d(res),
                        corner.prevL,
                        corner.nextL,
                        edge);

                if (! skel.seen.contains( ec ))
                    faceEvents.offer( ec );
            }
    }

    boolean holdRemoves = false;
    List<Corner> removes = new ArrayList<>();
    public void holdRemoves()
    {
        removes.clear();
        holdRemoves = true;
    }

    public void resumeRemoves()
    {
        holdRemoves = false;
        for (Corner c : removes)
            if (skel.liveCorners.contains( c )) // if hasn't been removed by horiz decomp
            removeCorner( c );
        removes.clear();
    }


    /**
     * Given corner should be fully linked into the network. Needs to be removed as
     * it connects two parallel faces. We remove toAdd.nextL
     *
     *
     */
    private void removeCorner( Corner toAdd )
    {
        if ( holdRemoves )
        {
            removes.add( toAdd );
            return;
        }

        DebugDevice.dump("about to delete "+toAdd.toString(), skel);

        // update corners
        toAdd.prevC.nextC = toAdd.nextC;
        toAdd.nextC.prevC = toAdd.prevC;

        // update edges
        toAdd.nextC.prevL = toAdd.prevL;

        // update main corner list
        skel.liveCorners.remove( toAdd );

        //brute force search for all references to old edge (if this was on a per face basis it'd be much nicer)
        for ( Corner lc : skel.liveCorners )
        {
            if (lc.nextL == toAdd.nextL)
            {
                lc.nextL = toAdd.prevL;
            }
            if (lc.prevL == toAdd.nextL)
            {
                lc.prevL = toAdd.prevL;
            }
        }

        if (toAdd.prevL != toAdd.nextL)
        {
            // update live edge list
            skel.liveEdges.remove(toAdd.nextL);

            // update output edge list (the two input edges give one output face)
//            skel.inputEdges.remove(toAdd.nextL);

            // update edges's live corners
            for (Corner c : toAdd.nextL.currentCorners)
                if (toAdd.prevL.currentCorners.add(c)); // also adds toAdd.nextC
            // merge output corner lists

            // add to the results map likewise
            skel.output.merge ( toAdd.prevC, toAdd ); //toAdd.prevL.addOutputSidesFrom (toAdd.nextL);

            // all collisions need recalculation. This situation could be avoided if collisions occur strictly with infinite faces.
            // recurse through all consecutive colinear faces...?
            skel.refindAllFaceEventsLater();
        }

        // update edges's live corners (might have copied this over from nextL)
        toAdd.prevL.currentCorners.remove( toAdd );


        // todo: we've merged two machines! (pick an arbitrary one?)
//        assert ( toAdd.prevL.machine == toAdd.nextL.machine );  
    }

    public void dump()
    {
        int i = 0;
        for (EdgeCollision ec : faceEvents)
            System.out.println(String.format( "%d : %s ", i++, ec) );
    }

    public void clearFaceEvents()
    {
        faceEvents.clear();
    }

    public void clearOtherEvents()
    {
        miscEvents.clear();
    }
}
