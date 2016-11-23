package straightskeleton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import straightskeleton.debug.DebugDevice;
import org.twak.utils.DHash;
import org.twak.utils.Line3D;
import org.twak.utils.LoopL;
import org.twak.utils.SetCorrespondence;

/**
 * Arbitrary topology update of the corners/edges on the sweep plane
 *
 * @author twak
 */
public class SkeletonCapUpdate
{
    Skeleton skel;
    // height is our height now, finalHeight is the height of all new geometry (we might slop upwards a bit)
    double height, finalHeight;
    DHash<Corner,Corner> oBCorner = new DHash();
    
    LoopL<Corner> oldCorners;
    Map<Edge, EdgeInfo> edgeInfo = new HashMap();

    // given in update
    DHash<Corner, Corner> nOCorner;
    SetCorrespondence<Corner, Corner> nOSegments;

    public SkeletonCapUpdate( Skeleton skel )
    {
        this.skel = skel;
    }

    /**
     * Returns a copy of "old" loop. Users are expected to duplicate it, before returning it to
     * update, below, with the corresponding data about what came from where.
     *
     * > you must not modify it
     * > you must not change the .currentCorners
     *
     */
    public LoopL<Corner> getCap( double height )
    {
        return getCap(height, height);
    }
    public LoopL<Corner> getCap( double height, double finalHeight )
    {
        this.height = height;
        this.finalHeight = finalHeight;
        oldCorners = skel.capCopy( height );
        oBCorner = skel.cornerMap;

        for (Corner c : oldCorners.eIterator())
        {
            c.z = 0;

            Corner baseC = skel.cornerMap.get(c);

            EdgeInfo ei = edgeInfo.get( baseC.nextL );
            if (ei == null)
                edgeInfo.put(baseC.nextL, ei = new EdgeInfo(baseC.nextL));
            ei.addBottomSeg( c );
        }
        
        for (Corner c : oldCorners.eIterator())
            c.nextL.calculateLinearForm();

        return oldCorners;
    }

    public DHash<Corner,Corner> getOldBaseLookup()
    {
        return oBCorner;
    }

    public static class EdgeInfo
    {
        Edge base;
        
        // corners with their start on the old edge
//        private Set<Corner>
//                topSegs = new LinkedHashSet(),
//                bottomSegs = new LinkedHashSet();

        List<Segment> segs = new ArrayList();
        
        public EdgeInfo ( Edge base )
        {
            this.base = base;
        }


        public void addTopSeg( Corner c )
        {
            segs.add( new Segment( c, true, true ) );
            segs.add( new Segment( c.nextC, true, false ) );
        }
        public void addBottomSeg( Corner c )
        {
                segs.add( new Segment( c, false, true));
                segs.add( new Segment( c.nextC, false, false));
        }

        List<Segment> sort()
        {
            Collections.sort( segs, new LineProjectionComparator( base.start, base.end ) );
            
            return segs;
        }

        void addFrom( EdgeInfo togo )
        {
            segs.addAll( togo.segs );
        }
    }

    static class Segment
    {
        Corner corner;
        boolean top,   // !bottom
                start; // !end

        Segment (Corner corner, boolean top, boolean start )
        {
            this.corner = corner;
            this.top = top;
            this.start = start;
        }
    }

    /**
     * The complicated bit:
     *
     * Given a new topology, and a map of what corresponds to the old bits, we ensure the output-faces of the skeleton
     * are continuous. A picture of the algorithm is in a SVG file. somewhere.
     *
     * When we update there are several possibilities:
     * (*) One edge may split to several parallel edges
     * (*) Several edges may combine to one (if parallel)
     * (*) Edges are created/destroyed
     *
     * @param newPlan The new plan that we're updating to
     * @param nOSegments The edge-correspondence between the new plan and the old. These two segments (corner -> corner.nextC ) are aligned, and the edge remains the same.
     * @param nOCorner The corner-correspondence between the new plan and the old. A corner has an entry here if it is in exzacadey the same place.
     *
     * NewPlan contains new edges, if there is a mapping for them in nOsegments, then the skeleton should only contain the corresponding edge from base.
     *
     */
    public void update ( LoopL<Corner> newPlan, 
            SetCorrespondence<Corner, Corner> nOSegments,
            DHash<Corner, Corner> nOCorner )
    {
        this.nOSegments = nOSegments;
        this.nOCorner = nOCorner;
//        DebugWindow.showIfNotShown( newPlan );


        // The edge info sections were constructed on the base topology. Colinear edges may have been merged. Extract the info from nOSegments, and merge edgeInfos.
        for (Corner nc : newPlan.eIterator())
        {
            Set<EdgeInfo> eiToMerge = new LinkedHashSet();
            for (Corner old :nOSegments.getSetA( nc ) )
                eiToMerge.add( edgeInfo.get ( oBCorner.get( old).nextL) );

            if (eiToMerge.size() < 2)
                continue;

            Iterator<EdgeInfo> eiit = eiToMerge.iterator();
            EdgeInfo toKeep = eiit.next(); // edgeInfo.size()
            while (eiit.hasNext())
            {
                EdgeInfo togo = eiit.next();
                toKeep.addFrom (togo);
                edgeInfo.remove( togo.base );

                skel.output.merge( toKeep.base.start, togo.base.start );
            }
        }


        for (Corner old : oBCorner.ab.keySet())
            old.z= height;

        for ( Corner c : newPlan.eIterator() ) // del: c.nextC.z != 0
            c.z = finalHeight;

        for ( Corner neu : newPlan.eIterator() )
        {
            neu.nextL.calculateLinearForm();

            // before neu list is broken up (by collectCorners), store the segments in edgeInfo
            for ( Corner oldSegment : nOSegments.getSetA( neu ) )
            {
//            assert ( oldEdge == null || oldSegment.nextL == oldEdge ); // if all coplanar edges are merged into one
//
//            oldEdge = oldSegment.nextL;

                Edge baseEdge = oBCorner.get( oldSegment ).nextL;
                EdgeInfo ei = edgeInfo.get( baseEdge );

                if ( ei == null )
                    edgeInfo.put( baseEdge, ei = new EdgeInfo( baseEdge ) );

                ei.addTopSeg( neu );
            }
        }


        Set<Corner> cornersToDelete = new HashSet (skel.liveCorners);

        for ( Corner c : newPlan.eIterator() )
            collectCorners(c, cornersToDelete);

        for ( Corner baseC : cornersToDelete )
        {
            Corner old = oBCorner.teg( baseC );
            // add vertical edge
            skel.output.addOutputSideTo( old , baseC, baseC.prevL, baseC.nextL);

            baseC.prevL.currentCorners.remove( baseC );
            baseC.nextL.currentCorners.remove( baseC );

            // actual removal happens below
        }
        
        skel.liveCorners.removeAll( cornersToDelete );


        // edgeInfo contains all segments from old and new that pretend to come from the same old edge
        oldEdges:
        for ( Edge baseE : edgeInfo.keySet() )
        {
//            Edge baseE;
//            old.end.z = old.start.z = height;
//            old.calculateLinearForm();

            // edges with joins/departs have an edgeInfo entry
            // everything else is already dealt with. old is still in the new structure
            EdgeInfo ei = edgeInfo.get( baseE );

            if ( ei == null )
                continue oldEdges;

            List<Segment> segs = ei.sort();

            Segment trailing = null;
            
            segs:
            for ( Segment s : segs )
            {
                // if it's a unchanged segment, do nothing,
                if (nOCorner.containsA( s.corner ) || nOCorner.containsB(s.corner) )
                {
                    // the corner isn't being replaced by this edge, nothing to do
                    assert ( trailing == null );
                    // don't count towards towards alternating trailing
                    continue segs;
                }
                // if it's one of the bottoms
                else if (!s.top)
                {
                    // add vertical end for base-> old
                    Corner baseC = oBCorner.get( s.corner );
                    skel.output.addOutputSideTo( s.corner, baseC, baseC.prevL, baseC.nextL );
                    baseE.currentCorners.remove( s.corner );
                }
                // one of the tops is being used
                else
                {
                    // just add the relevant entries in the corner maps
                    skel.liveCorners.add(s.corner);
                    baseE.currentCorners.add( s.corner );
                    if (s.start)
                        s.corner.nextL = baseE;
                    else
                        s.corner.prevL = baseE;

                }

                if (trailing != null) // polarity needs fixing up? first section sometimes capped, sometimes not.
                {
                    // because the cap (base) is at height 0, we elevate it here.
                    skel.output.addOutputSideTo( true, trailing.corner, s.corner, baseE );
                    trailing = null; // every other segment
                }
                else
                    trailing = s; // every other segment
            }
        }

        /**
         * Pointers may have been removed by previous/next edges, or some edges may have been removed entirely
         */
        // this is broken - where do we remove other corners that might reference htis edge?
        Iterator<Edge> eit = skel.liveEdges.iterator();
        while ( eit.hasNext() )
        {
            Edge e = eit.next();
            if ( e.currentCorners.size() == 0 )
            {
                eit.remove();
                skel.liveCorners.remove( e.start );
                skel.liveCorners.remove( e.end );
            }
        }

        skel.refindAllFaceEventsLater();

        DebugDevice.dump("post cap update", skel);
        skel.validate();
    }

    /**
     * this accumulates the structure of EdgeInfos, detailing the points
     * to be inserted into each edge of the old structure.
     *
     * @param toDelete -- remove the corner from this list if it shouldn't be deleted
     */
    private void collectCorners( Corner neu, Set<Corner> toDelete  )
    {
        Set<Corner> oldSegments = nOSegments.getSetA( neu );

//        Edge oldEdge = null;
        Corner old = nOCorner.get( neu );
        Corner base = oBCorner.get( old );

        Edge neuEdge = neu.nextL;
        if ( oldSegments.isEmpty() ) // tis an entirely new edge, add it in!
        {
//            Corner previous = base != null ? base.nextL.start
            if ( skel.liveEdges.add( neuEdge ) )
            {
                skel.output.newEdge( neuEdge, null, neuEdge.profileFeatures );
                neuEdge.machine.addEdge( neuEdge, skel );
            }

            skel.output.newDefiningSegment( neu );
        }
        
        if ( old != null )
        {
            assert (base != null); // topology of old, base matches
            neu.prevC.nextC = base;
            neu.nextC.prevC = base;
            base.nextC = neu.nextC;
            base.prevC = neu.prevC;
            
            toDelete.remove( base ); // we'll use that, thanks
        }
        else // old == null (=> base == null)
        {
            skel.liveCorners.add( neu );

            neuEdge.currentCorners.add( neu );
            neu.prevL.currentCorners.add( neu );
        }
    }

    /**
     * Clones a loop (corner and edges), and returns a map of new to old edges and corners.
     *
     * DEPRICATED: use CornerClone
     *
     */
//    public static LoopL<Corner>cloneEdges (LoopL<Corner> in, final DHash<Edge, Edge> nOEdge, final DHash<Corner, Corner> nOCorner)
//    {
//        final Cache<Corner, Corner> cornerCache = new Cache<Corner, Corner>()
//        {
//            @Override
//            public Corner create( Corner i )
//            {
//                Corner out = new Corner( i.x, i.y, i.z );
//                nOCorner.put( out, i );
//                return out;
//            }
//        };
//
//        Cache<Edge, Edge> edgeCache = new Cache<Edge, Edge>()
//        {
//            @Override
//            public Edge create( Edge i )
//            {
//                Edge nE = new Edge( cornerCache.get( i.start), cornerCache.get(i.end) );
//                nOEdge.put( nE, i );
//
//                nE.machine = i.machine;
//                nE.profileFeatures = new HashSet(i.profileFeatures);
//                nE.setAngle( i.getAngle() );
//
//                return nE;
//            }
//        };
//
//        LoopL<Corner> out = new LoopL();
//
//        for ( Loop<Corner> inLoop : in )
//        {
//            Loop<Corner> outLoop = new Loop<Corner>();
//            out.add( outLoop );
//            for (Corner c : inLoop)
//            {
//                Corner nC = cornerCache.get( c );
//                outLoop.append( nC );
//                nC.nextL = edgeCache.get( c.nextL );
//                nC.prevL = edgeCache.get( c.prevL );
//            }
//        }
//
//        return out;
//    }

    static class LineProjectionComparator implements Comparator<Segment>
    {
        Line3D line;

        public LineProjectionComparator( Point3d start, Point3d end )
        {
            Vector3d dir = new Vector3d(end);
            dir.sub( start );
            line = new Line3D( start, dir);
        }

        public int compare( Segment o1, Segment o2 )
        {
            return Double.compare( line.projectParam( o1.corner ), line.projectParam(o2.corner) );
        }
    }





//                if (!s.neu)
//                {
//                    if (s.start)
//                        inOld = true;
//                    else
//                        inOld = false;
//                }
//                else
//                {
//                    if (s.start)
//                        inNeu = true;
//                    else
//                        inOld = false;
//                }

//            }

//            // if the old corner from the start of this line is longer in use
//            if ( nOCorner.get( firstC ) == null )
//            {
//                Corner togo = cornerMap.get( old.start );
//                assert ( togo.nextL == base );
//                base.currentCorners.remove( togo );
//                skel.output.addOutputSideTo( togo, old.start, togo.nextL, togo.prevL );
//                // should also add output side to the previous horizontal underside!
//                skel.output.addOutputSideTo( old.start, firstC, togo.nextL );
//            }
//            if ( firstC.nextC.prevC != firstC )
//                // corner is a lower corner - was never raised up
//                cornersFromStart.set( 0, firstC.nextC.prevC );
//
//            if ( nOCorner.get( lastC ) == null )
//            {
//                Corner togo = cornerMap.get( old.end );
//                assert (togo.prevL == base);
//                base.currentCorners.remove( togo );
//                skel.output.addOutputSideTo( togo, old.end, togo.prevL, togo.nextL );
//                skel.output.addOutputSideTo( old.end, lastC, togo.prevL ); // fixme: missing underside on nextL
//                }
//            if ( lastC.prevC.nextC != lastC )
//                // corner is a lower corner - was never raised up
//                cornersFromStart.set( cornersFromStart.size() - 1, lastC.prevC.nextC );
//
//            // if this is an old edge that we're keeping we go through the following, but shouldn't change anything!
//
//            // are we currently inside the new solid? -_-
//            boolean onOld = true;
//            for ( Pair<Corner, Corner> pair : new ConsecutiveItPairs<Corner>( cornersFromStart ) )
//            {
//                skel.liveCorners.add( pair.first() ); // adds twice for each interior point, no harm done
//                skel.liveCorners.add( pair.second() );
//
//                if ( onOld )
//                {
//                    pair.first().nextL = base;
//                    pair.second().prevL = base;
//                    base.currentCorners.add( pair.first() );
//                    base.currentCorners.add( pair.second() );
//                }
//                else
//                    skel.output.addOutputSideTo( pair.first(), pair.second(), base );
//
//                onOld = !onOld;
//            }
//        }



        // remove any old edges from the current graph - old if
//        liveEdges:
//        while (eit.hasNext())
//        {
//            Edge base = eit.next();
//
//            Edge old = edgeMap.teg( base );
//
//            if (old == null) // new line!
//                continue liveEdges;
//
//            old.end.z = old.start.z = height;
//
//            EdgeInfo ei = edgeInfo.get( old );
//
//            if (ei == null)
//            {
//                // this edge isn't referenced in the new edge loop. kill it.
//                eit.remove();
//
//                // add output edges to cap the edge
//                skel.output.addOutputSideTo( old.start, old.end, base );
//
//                base.currentCorners.clear();
//            }
//        }
}
