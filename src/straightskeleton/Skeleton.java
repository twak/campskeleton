
package straightskeleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import straightskeleton.Output.Face;
import straightskeleton.debug.DebugDevice;
import utils.Cache;
import utils.CloneConfirmIterator;
import utils.DHash;
import utils.LinearForm3D;
import utils.Loop;
import utils.LoopL;
import utils.ManyManyMap;
import utils.MultiMap;
import utils.SetCorrespondence;

/**
 * to debug: does it work at all (PointEditor)
 * Offset?
 * Horizontal Edges?
 * Height Collision.processHoriz()?
 *
 * Call 
 *  <pre>Skeleton skel = new Skeleton( edges );
 *  skel.skeleton();</pre>
 *
 * get output from
 * <pre>getOutput</pre>
 *
 * @author twak
 */
public class Skeleton
{
    public Set<Corner> liveCorners = new LinkedHashSet();
    public Set<Edge> liveEdges = new LinkedHashSet();
    public CollisionQ qu;
    public double height = 0;
//    public Set<Edge> inputEdges = new LinkedHashSet();

    // we store the triplets of faces we've already passed out to stop repeats (insensitive to face order)
    public Set<EdgeCollision> seen = new LinkedHashSet();

    // output data
    public LoopL<Corner> flatTop = new LoopL();
    public Output output = new Output( this );
    
    // debug
    public List<CoSitedCollision> debugCollisionOrder = new ArrayList();

    // after a direction change, keep track of the edges here
//    public MultiMap<Edge, Edge> inputToOutputEdges = new MultiMap();

    public Map<Edge, Set<Feature>> planFeatures = new LinkedHashMap();

    // for debugging
    public String name = "?";

    // lazy system for refinding all face events. true so we run it once at start
    boolean refindFaceEvents = true;

    protected Skeleton(){}

//    public Skeleton ( List<Edge> edges )
//    {
//        LoopL<Edge> input = new LoopL();
//        Loop<Edge> loop = new Loop();
//        input.add( loop );
//
//        for (Edge e : edges)
//            loop.append( e );
//
//        setup ( input );
//    }

    public Skeleton (LoopL<Corner> corners)
    {
        setup( corners );
    }

    /**
     * @Deprecated
     * @param input list of edges, edges shouldn't be repeated!
     */
    public Skeleton( LoopL<Edge> input, boolean javaGenericsAreABigPileOfShite )
    {
        setupForEdges(input);
    }


    /**
     * @param cap height (flat-topped skeleton) to finish at
     */
    public Skeleton(LoopL<Corner> input, final double cap, boolean javaGenericsAreABigPileOfShite) {
        setup(input);

        qu.add(new HeightEvent() {

            public double getHeight() {
                return cap;
            }

            public boolean process(Skeleton skel) {
                SkeletonCapUpdate capUpdate = new SkeletonCapUpdate(skel);

                flatTop = capUpdate.getCap(cap);
                
//                DebugDevice.dump("pre cap dump", skel);
                // this call should remove all geometry, and cap the remainder...?
                capUpdate.update(new LoopL(), new SetCorrespondence<Corner, Corner>(), new DHash<Corner, Corner>());
                DebugDevice.dump("post cap dump", skel);

                // we're the last event!
//                qu.clearFaceEvents();
//                qu.clearOtherEvents();

                return true;
            }
        });
    }

    /**
     * @Deprecated
     * @param cap height (flat-topped skeleton) to finish at
     */
    public Skeleton( LoopL<Edge> input, final double cap )
    {
        setupForEdges(input);

        qu.add( new HeightEvent() {

            public double getHeight() {
                return cap;
            }

            public boolean process(Skeleton skel)
            {
                SkeletonCapUpdate capUpdate = new SkeletonCapUpdate( skel );

                flatTop = capUpdate.getCap( cap );
                // this call should remove all geometry, and cap the remainder...?
                capUpdate.update( new LoopL(), new SetCorrespondence<Corner, Corner>(), new DHash<Corner, Corner>());
             
                // we're the last event!
//                qu.clearFaceEvents();
//                qu.clearOtherEvents();

                return true;
            }
        });
    }


    /**
     * Stop-gap measure to convert loops of edges (BAD!) to loops of corners (GOOD!)
     * @param input
     */
    public void setupForEdges (LoopL<Edge> input)
    {
        setup (Corner.cornerToEdgeLoopL( input ));
    }

    /**
     * Sanitize input
     * @param input
     */
    public void setup( LoopL<Corner> input )
    {
        // reset all! (not needed...but maybe in future)
        height =0;
        liveCorners.clear(); 
        liveEdges.clear();

        MultiMap<Edge, Corner> allEdges= new MultiMap();

        for (Corner c : input.eIterator()) // input.count()
            allEdges.put( c.nextL, c );

        // combine shared edges into single output faces
        for ( Edge e : allEdges.keySet() ) //allEdges.size()
        {
            e.currentCorners.clear();
            List<Corner> corners = allEdges.get( e );
            Corner first = corners.get( 0 );

            output.newEdge( first.nextL, null, new LinkedHashSet() );

            for (int i = 1; i < corners.size(); i++)
                output.merge( first, corners.get( i ) );

            liveEdges.add(e);
        }

        for (Corner c : input.eIterator())
        {
            output.newDefiningSegment( c );
            liveCorners.add(c );
            c.nextL.currentCorners.add(c);
            c.prevL.currentCorners.add(c);
        }

        qu = new CollisionQ( this ); // yay closely coupled classes

        for ( Edge e : allEdges.keySet() )
        {
            e.machine.addEdge( e, this );
        }

        // now all angles are set, find initial set of intersections (will remove corners if parallel enough)
        refindFaceEventsIfNeeded();

//        qu.dump(); // debug
    }

    /**
     * Execute the skeleton algorithm
     */
    public void skeleton()
    {
        validate();
        HeightEvent he;

        int i = 0;

        DebugDevice.dump("main "+String.format("%4d", ++i ), this );
        while ( ( he = qu.poll() ) != null )
            try
            {
                if ( he.process( this ) ) // business happens here
                {
                    DebugDevice.dump("main "+String.format("%4d", ++i ), this );
                    height = he.getHeight();
                    validate();
                }
                refindFaceEventsIfNeeded();
            }
            catch ( Throwable t )
            {
                t.printStackTrace();
                if (t.getCause() != null)
                {
                    System.out.println("  caused by:");
                    t.getCause().printStackTrace();
                }
            }

        // build output polygons from constructed graph
        output.calculate( this );
    }

    /**
     * This method returns a set of edges representing a horizontal slice through the skeleton
     * at the specified height (given that no other events happen bewteen current height and given cap height).
     *
     * Topology assumed final - eg - we take a copy at of the slice at the given height, not processing any more height events
     *
     * Non destructive - this doesn't change the skeleton. This routine is for taking output mid-way through evaluation.
     *
     * All output edges have the same machines as their originators.
     */
    
    public DHash<Corner,Corner> cornerMap; // contains lookup for results (new->old)
    public ManyManyMap<Corner,Corner> segmentMap; // contains lookup for results ( old -> new )
    
    public LoopL<Corner> capCopy (double height)
    {
        segmentMap = new ManyManyMap<Corner, Corner>();
        cornerMap = new DHash();

        LinearForm3D ceiling = new LinearForm3D( 0, 0, 1, -height );

        for (Corner c : liveCorners)
        {

            try {
                Tuple3d t;

                // don't introduce instabilities if height is already as requested.
                if (height == c.z )
                    t = new Point3d(c);
                else
                    t = ceiling.collide(c.prevL.linearForm, c.nextL.linearForm);

                cornerMap.put(new Corner(t), c);
            } catch (RuntimeException e) {
                //assume, they're all coincident?
                cornerMap.put (new Corner (c.x, c.y, height), c);
            }
        }

         Cache<Corner, Edge> edgeCache = new Cache<Corner, Edge>()
         {
             Map<Edge, Edge> lowToHighEdge = new HashMap();

            @Override
            /**
             * @param i the low corner
             */
            public Edge create( Corner i )
            {
                Edge cached = lowToHighEdge.get (i.nextL);

                // the following two lines reuse an edge when it is referenced twice. this seems like the better way to do it, but our triangulator can't currently handle two-separate loops of vertices
//                if (cached != null)
//                    return cached; // this was one edge, (i.nextL), the raised copy will also be one edge

                Edge edge = new Edge ( cornerMap.teg(i), cornerMap.teg(i.nextC) );

                lowToHighEdge.put(i.nextL, edge);

                edge.setAngle( i.nextL.getAngle() );
                edge.machine = i.nextL.machine; // nextL is null when we have a non root global
//              edge.profileFeatures = new LinkedHashSet<Feature>(current.nextL.profileFeatures);
//              edgeMap.put( edge, current.nextL );

                return edge;
            }
         };

        LoopL<Corner> out = new LoopL();

        Set<Corner> workingSet = new LinkedHashSet ( liveCorners );
        while (!workingSet.isEmpty())
        {
            Loop<Corner> loop = new Loop();
            out.add( loop );
            Corner current = workingSet.iterator().next();
            do
            {
                Corner s = cornerMap.teg( current ),
                       e  = cornerMap.teg( current.nextC );

                // one edge may have two segments, but the topology will not change between old and new,
                // so we may store the leading corner to match segments
                segmentMap.addForwards( current, s );

                Edge edge = edgeCache.get( current );

                loop.append( s );
                s.nextC = e;
                e.prevC = s;
                s.nextL = edge;
                e.prevL = edge;

                workingSet.remove( current );
                current = current.nextC;
            }
            while (workingSet.contains( current ));
        }
        
        return out;
    }

    public Cache<Corner, Collection<Corner>> getSegmentOriginator()
    {
        return output.getSegmentOriginator();
    }

    // when a face is parented, it is flagged here. this allows overriding classes to get even process this information
    public void parent( Face child, Face parent ) // parent is below (older than) child...
    {
        //override me
    }

    /**
     * Given an input edge this method returns a list of
     * all the current edges that have come from that edge.
     *
     * It includes all replaceEdges() edges (not sure if this is correct)
     */
//    public List<Edge> findCurrentEdges( Edge inputEdge )
//    {
//        List<Edge> out= new ArrayList();
//        findCurrentEdges( inputEdge, out );
//        return out;
//    }
//
//    private void findCurrentEdges ( Edge input, List<Edge> results)
//    {
//        if (liveEdges.contains( input ))
//        {
//            results.add( input );
////            return;
//        }
//        List<Edge> next = inputToOutputEdges.get( input );
//        if (next == null)
//            return;
//        else for (Edge e : next)
//            findCurrentEdges( e, results );
//    }
    

    /**
     * Adds in the given set of edges, setting their height (z) to the current height. These edges
     * may new machines etc...
     *
     * We assume that all edge/corners are properly connected.
     *
     * Assume that the area on the sweep plane remains a simple polygon...
     */
//    public void insertPlanAtHeight( LoopL<Corner> corners, double height )
//    {
//
//        MultiMap<Edge, Corner> edges = new MultiMap();
//
//        for (Corner c : corners.eIterator())
//        {
//            edges.put( c.nextL, c );
//            edges.put( c.prevL, c );
//        }
//
//        for (Edge e : edges.keySet())
//        {
//            e.end.z = e.start.z = height;
//
//            e.currentCorners.clear();
//
//            liveEdges.add( e );
//            liveCorners.add( e.start ); // e.start == e.prev.end
//
//            output.newFace( null, null, null );//addFace( e, null, e.profileFeatures );
//
//            e.currentCorners.addAll( edges.get( e ) );
//        }
//
//        // after all adjustments to lines, reset the machines
//        for ( Edge e : edges.keySet() )
//        {
//            e.machine.addEdge( e, this );
//        }
//
//        refindAllFaceEvents();
//
//        validate();
//    }

    /**
     * @depricated
     * @param corners
     * @param height
     */
//    public void insertPlanAtHeight( LoopL<Edge> edges, double height, boolean javaGenericsAreABigPileOffCrapola )
//    {
//        for (Edge e : edges.eIterator())
//        {
//            e.end.z = e.start.z = height;
//
//            e.currentCorners.clear();
//
//            liveEdges.add( e );
//            liveCorners.add( e.start ); // e.start == e.prev.end
//
//            output.newFace( null, null, null );//addFace( e, null, e.profileFeatures );
//
//            e.currentCorners.add( e.start );
//            e.currentCorners.add( e.end );
//
//        }
//
//        // after all adjustments to lines, reset the machines
//        for ( Edge e : edges.eIterator() )
//        {
//            e.machine.addEdge( e, this );
//        }
//
//        refindAllFaceEvents();
//
//        validate();
//    }

    /**
     * This searches for the originating (not additional/forced step) edges that have
     * led to the use of the target edge. Perhaps we want input edge, additional edges
     * and a addition -> input edge map so we have findInputFrom(Edge) & findMaybeAdditionalFrom(Edge)?
     */
//    public Edge findOriginatingEdge( Edge target )
//    {
//        outer:
//        while (target != null)
//        {
//            for (Edge e : inputToOutputEdges.keySet())
//                for (Edge f : inputToOutputEdges.get( e ))
//                    if (f == target)
//                    {
//                        target = e;
//                        continue outer;
//                    }
//             return target;
////                assert (target != null);
//        }
//        return null;
//    }

    /**
     * Called whenever a new edge is created, with
     * @param e
     * @param edgeH
     */
//    public void registerOutputEdge( Edge old, Edge neu )
//    {
//        inputToOutputEdges.put( old, neu );
//    }

    public static class SEC
    {
        Corner start, end;
        Edge nextL, edge, prevL;

        public SEC(Corner start, Edge edge)
        {
            this.start = start;
            end = start.nextC;
            prevL = start.prevL;
            nextL = end.nextL;

            this.edge = edge;
        }
    }

    /**
     * Used for replacing one edge with a set of edges. Generally replaced by SkeletonCapUpdate
     * for more general topological setups.
     *
     * For each edge given, it's naturally extrueded to the given eHeight using
     * it's neighbouring edges ( assumes all collisions below eHeight complete).
     * It's then replaced with a new edge, specified by the edgeCreator factory
     *
     */
//    public void replaceEdges( List<Edge> toReplace, EdgeCreator edgeCreator, double eHeight )
//    {
//        Map<Corner, Corner> oldToNew = new LinkedHashMap();
//
//        LinearForm3D ceiling = new LinearForm3D( 0, 0, 1, -eHeight );
//
//        List<SEC> secs = new ArrayList();
//        /**
//         * Before we start messing with pointers, find all
//         * corners that start a line.
//         */
//        for ( Edge e : toReplace )
//        {
//            if ( liveEdges.contains( e ) )
//                for ( Corner start : e.currentCorners )
//                    if ( start.nextL == e )
//                        secs.add( new SEC( start, e ) );
//        }
//
//        /**
//         * For each section of each edge found, elevate it's corners (if
//         * not done by a previous edge) and it.
//         */
//        for ( SEC sec : secs )
//        {
//            Corner start = sec.start, end = sec.end;
//            Edge e = sec.edge;
//
//            Corner // 'high' points, ones that will form new loop
//                    startH = elevate( start, oldToNew, ceiling ),
//                    endH = elevate( end, oldToNew, ceiling );
//
//            startH.nextC = endH;
//            endH.prevC = startH;
//            startH.nextL = e;
//            endH.prevL = e;
//
//            List<Corner> replacementCorners = edgeCreator.getEdges( e, startH, endH );
//
//            if ( replacementCorners != null )
//            {
//                assert ( replacementCorners.get( 0 ) == startH );
//                assert ( replacementCorners.get( replacementCorners.size() - 1 ) == endH );
//
//                /**
//                 * If we are using the start/end points (the first/last line segment is
//                 * not e), then wire in startH, endH. Otherwise ignore first and last
//                 * points in the replacements and wire between start and end.
//                 *
//                 * bit of a mess...should be refactored and some of of the workings
//                 * moved to the EdgeCreator...
//                 */
//
//                boolean useStartH = startH.nextL != e;
//                if ( useStartH ) // really using startH
//                {
//                    liveCorners.add( startH );
//                    liveCorners.remove( start );
//
//                    startH.prevC.nextC = startH;
//                    startH.prevL.currentCorners.remove( start );
//                    startH.prevL.currentCorners.add( startH );
//
//                    output.addOutputSideTo ( start, startH, start.prevL, e );
//
//                    e.currentCorners.remove( start );
//                    e.currentCorners.remove( startH );
//                }
//                else
//                {
//                    Corner first = replacementCorners.get( 1 );
//                    first.prevC = start;
//                    start.nextC = first;
//                }
//
//                boolean useEndH = endH.prevL != e;
//                if (useEndH) // really using endH
//                {
//                    liveCorners.add( endH );
//                    liveCorners.remove( end );
//
//                    endH.nextC.prevC = endH;
//
//                    endH.nextL.currentCorners.remove( end );
//                    endH.nextL.currentCorners.add( endH );
//
//                    output.addOutputSideTo ( end, endH, e, end.nextL );
//
//                    e.currentCorners.remove( end );
//                    e.currentCorners.remove( endH );
//                }
//                else
//                {
//                    Corner last = replacementCorners.get( replacementCorners.size()-2 );
//                    last.nextC = end;
//                    end.prevC = last;
//                }
//
//                replacementCorners.remove( replacementCorners.size() -1 );
//
//                Corner leaveE = useStartH ? startH : null;
//                for ( Corner c : replacementCorners ) // endH removed!
//                {
//                    Edge edgeH = c.nextL;
//
//                    if (useStartH || c != startH )
//                        liveCorners.add( c );
//
//                    if ( edgeH != e )
//                    {
//                        output.addFace( edgeH, null, edgeCreator.getFeaturesFor(edgeH) );
//                        liveEdges.add( edgeH );
//                        edgeH.currentCorners.clear();
//                        inputEdges.add( edgeH );
//
//                        // keep a check of where each edge started from
//                        registerOutputEdge (e, edgeH);
////                        inputToOutputEdges.put( e, edgeH );
//                    }
//                    else // e == edgeH
//                    {
//                        if (leaveE != null)
//                        {
//                            output.addOutputSideTo( leaveE, c, e );
//                        }
//                        leaveE = c.nextC;
//                    }
//
//                    if ( useStartH || c != startH )
//                        edgeH.currentCorners.add( c );
//                    if ( useEndH || c.nextC != endH )
//                        edgeH.currentCorners.add( c.nextC );
//                }
//                if (leaveE != null && useEndH)
//                    output.addOutputSideTo( leaveE, endH, e );
//
//                for ( Corner c : replacementCorners ) // endH removed!
//                    c.nextL.machine.addEdge( c.nextL, this ); // if new calculates linearform
//            }
//        }
//
//        /**
//         * Pointers may have been removed by previous/next edges
//         */
//        Iterator<Edge> eit = liveEdges.iterator();
//        while (eit.hasNext())
//            if (eit.next().currentCorners.size() == 0)
//                eit.remove();
//
//        refindAllFaceEvents();
//
//        validate();
//    }


    public void refindAllFaceEventsLater()
    {
        refindFaceEvents = true;
    }

    private void refindFaceEventsIfNeeded()
    {
        // on demand
        if (!refindFaceEvents)
            return;

        /**
         * Very expensive part - refind all collisions (including those already processed)
         * MachineEvents remain in their current state
         *
         * should really only be done for those edges that have changed (change for when we integrate eppsteins stuff)
         */

        // context collects events that must be processed immediately following (eg horizontals...)
         HeightCollision context = new HeightCollision();

        qu.clearFaceEvents();
        for ( Corner lc : new CloneConfirmIterator<Corner>(liveCorners) )
            qu.addCorner( lc, context );

        // if we are not adding new events (and this isn't adding the input the first time)
        // this shouldn't do anything
        context.processHoriz( this );
    }

//    private Corner elevate( Corner start, Map<Corner, Corner> oldToNew, LinearForm3D ceiling )
//    {
//        Corner startH;
//        if ( oldToNew.containsKey( start ) )
//            startH = oldToNew.get( start );
//        else
//        {
//            Tuple3d res = ceiling.collide( start.nextL.linearForm, start.prevL.linearForm );
//            startH = new Corner( res );
//
//            oldToNew.put( start, startH );
//            //  don't insert into loop, but copy all ptrs
//            startH.prevC = start.prevC;
//            startH.nextC = start.nextC;
//            startH.prevL = start.prevL;
//            startH.nextL = start.nextL;
//        }
//        return startH;
//    }

    /**
     * Debug!
     */
    public void validate()
    {
        if (true)
        {
        Set <Corner> all = new LinkedHashSet ( liveCorners );
        outer:
        while (!all.isEmpty())
        {
            Corner start = all.iterator().next();
            all.remove(start);

            Corner next = start;

            int count = 0;

            do
            {
                count ++;
                Corner c = next.nextC;
                all.remove( c );

                Edge e = next.nextL;
                try
                {
                    assert ( c.nextC.prevC == c );
                    assert ( c.prevC.nextC == c );

                    assert (c.prevL == e);
                    assert (c.prevC.nextL == e);

//                    assert ( e.start.nextC == e.end );
//                    assert ( e.end.prevC == e.start ); liveEdges.contains(e)
                    for (Corner d : liveCorners)
                    {
                        if (d.nextL == e || d.prevL == e)
                            assert ( e.currentCorners.contains( d ) );
                        else
                            assert ( !e.currentCorners.contains( d ) );
                    }

                    for ( Corner d : e.currentCorners )
                        assert ( liveCorners.contains( d ) );

                    assert (count < 100);
                }
                catch ( AssertionError f )
                {
                    System.err.println( " on edge is "+e);
                    System.err.println( " validate error on corner " + c + "  on line " + f.getStackTrace()[0].getLineNumber() );
                    f.printStackTrace();
                }
                finally
                {
                    if (count > 100)
                        continue outer;
                }

                next = c;
            }
            while (next != start);
        }
        }
    }

    public void setPlanTags (Edge edge, Set<Feature> features)
    {
        planFeatures.put( edge, features );
    }

    public Set<Feature> getPlanTags( Edge originator )
    {
        return planFeatures.get( originator );
    }

    public Comparator<Edge> getHorizontalComparator()
    {
//        final boolean max = Math.random() > 0.5 ? true : false;
        return new Comparator<Edge>()
        {
            /**
             * Volume maximizing resolution
             */
            public int compare( Edge o1, Edge o2 )
            {
//                if ( max )
                    return Double.compare( o1.getAngle(), o2.getAngle() );
//                else
//                    return Double.compare( o2.getAngle(), o1.getAngle() );
            }
        };
    }

    public LoopL<Corner> findLoopLive()
    {
        LoopL<Corner> out = new LoopL<Corner>();
        Set<Corner> togo = new HashSet(liveCorners);

        while (!togo.isEmpty())
        {
            Loop<Corner> loop = new Loop();
            out.add(loop);

            Corner start = togo.iterator().next();

            Corner current = start;
            do
            {
                togo.remove(current);
                loop.append(current);

                current = current.nextC;
            }
            while (current !=start);
        }

        return out; //out.count();

    }
}        