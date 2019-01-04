
package org.twak.camp;

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
import javax.vecmath.Vector3d;

import org.twak.camp.Output.Face;
import org.twak.camp.debug.DebugDevice;
import org.twak.utils.Cache;
import org.twak.utils.collections.CloneConfirmIterator;
import org.twak.utils.collections.DHash;
import org.twak.utils.collections.Loop;
import org.twak.utils.collections.LoopL;
import org.twak.utils.collections.Loopable;
import org.twak.utils.collections.Loopz;
import org.twak.utils.collections.ManyManyMap;
import org.twak.utils.collections.MultiMap;
import org.twak.utils.collections.SetCorrespondence;
import org.twak.utils.geom.LinearForm3D;

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
    public boolean preserveParallel = false;
	public Set<Corner> liveCorners = new LinkedHashSet<>();
    public Set<Edge> liveEdges = new LinkedHashSet<>();
    public CollisionQ qu;
    public double height = 0;

    // we store the triplets of faces we've already passed out to stop repeats (insensitive to face order)
    public Set<EdgeCollision> seen = new LinkedHashSet<>();

    // output data
//    public LoopL<Corner> flatTop = new LoopL<>();
    public Output output = new Output( this );
    
    // debug
    public List<CoSitedCollision> debugCollisionOrder = new ArrayList<>();

    public Map<Edge, Set<Tag>> planFeatures = new LinkedHashMap<>();

    // for debugging
    public String name = "?";

    // lazy system for refinding all face events. true so we run it once at start
    boolean refindFaceEvents = true;

    public Skeleton(){}

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
    public Skeleton(LoopL<Corner> input, double cap, boolean javaGenericsAreABigPileOfShite) {
        setup(input);

        capAt(cap);
    }	

    /**
     * @Deprecated
     * @param cap height (flat-topped skeleton) to finish at
     */
    public Skeleton( LoopL<Edge> input, final double cap )
    {
        setupForEdges(input);

        capAt(cap);
    }

    /**
     * Stop-gap measure to convert loops of edges (BAD!) to loops of corners (GOOD!)
     * @param input
     */
    public void setupForEdges (LoopL<Edge> input)
    {
        LoopL<Corner> corners = new LoopL<>();
        for (Loop<Edge> le : input) //input.count()
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

        setup (corners); //corners.count()
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

        MultiMap<Edge, Corner> allEdges= new MultiMap<>();

        for (Corner c : input.eIterator()) // input.count()
            allEdges.put( c.nextL, c );

        // combine shared edges into single output faces
        for ( Edge e : allEdges.keySet() ) //allEdges.size()
        {
            e.currentCorners.clear();
            List<Corner> corners = allEdges.get( e );
            Corner first = corners.get( 0 );

            output.newEdge( first.nextL, null, new LinkedHashSet<>() );

            // why don't we need this?
//            for (Corner c : corners)
//                output.newDefiningSegment( first );

            // not sure this is right
            for (int i = 1; i < corners.size(); i++)
                output.merge( first, corners.get( i ) );

            liveEdges.add(e);
        }

        for (Corner c : input.eIterator())
        {
            if (c.z != 0 || c.nextL == null || c.prevL == null) // fixme: threading bug with chordatlas under openJDK11 causes npes on nextL?
                throw new Error("Error in input");

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
                    height = he.getHeight();
                    DebugDevice.dump("main "+height+" "+String.format("%4d", ++i ), this );
                    validate();
                }
                
                refindFaceEventsIfNeeded();
            }
            catch ( Throwable t )
            {
                t.printStackTrace();
                if (t.getCause() != null)
                {
                    System.out.println(" caused by:");
                    t.getCause().printStackTrace();
                }
            }

        DebugDevice.dump("after main "+String.format("%4d", ++i ), this );

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
        cornerMap = new DHash<>();

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
             Map<Edge, Edge> lowToHighEdge = new HashMap<>();

            @Override
            /**
             * @param i the low corner
             */
            public Edge create( Corner i )
            {
//                Edge cached = lowToHighEdge.get (i.nextL);

                // the following two lines reuse an edge when it is referenced twice. this seems like the better way to do it, but our triangulator can't currently handle two-separate loops of vertices
//                if (cached != null)
//                    return cached; // this was one edge, (i.nextL), the raised copy will also be one edge

                Edge edge = new Edge ( cornerMap.teg(i), cornerMap.teg(i.nextC) );

                lowToHighEdge.put(i.nextL, edge);

                edge.setAngle( i.nextL.getAngle() );
                edge.machine = i.nextL.machine; // nextL is null when we have a non root global

                return edge;
            }
         };

        LoopL<Corner> out = new LoopL<>();

        Set<Corner> workingSet = new LinkedHashSet<> ( liveCorners );
        while (!workingSet.isEmpty())
        {
            Loop<Corner> loop = new Loop<>();
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
    
    
    public interface HeresTheArea {
    	public void heresTheArea(double area);
    }

    public void capAt (double cap) {
    	capAt (cap, null);
    	
    }
    public void capAt (double cap, HeresTheArea hta) {
    	
    	  qu.add(new HeightEvent() {

    		  public double getHeight() {
                  return cap;
              }

              public boolean process(Skeleton skel) {
            	  
                  SkeletonCapUpdate capUpdate = new SkeletonCapUpdate(skel);

                  
                  LoopL<Corner> flatTop = capUpdate.getCap(cap);
                  
                  capUpdate.update(new LoopL<>(), new SetCorrespondence<Corner, Corner>(), new DHash<Corner, Corner>());
                  
                  LoopL<Point3d> togo =
                          flatTop.new Map<Point3d>()
                          {
                              @Override
                              public Point3d map( Loopable<Corner> input )
                              {
                                  return new Point3d( input.get().x, input.get().y, input.get().z );
                              }
                          }.run();
                          skel.output.addNonSkeletonOutputFace( togo, new Vector3d( 0, 0, 1 ) );
                          
                          if (hta != null)
                        	  hta.heresTheArea( Loopz.area3( togo ) );
                          
                  DebugDevice.dump("post cap dump", skel);

                  skel.qu.clearFaceEvents();
                  skel.qu.clearOtherEvents();
                  
                  return true;
              }
          });
    }
    
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
         * should really only be done for those edges that have changed
         */

        // context collects events that must be processed immediately following (eg horizontals...)
         HeightCollision context = new HeightCollision();

//        qu.clearFaceEvents();
        for ( Corner lc : new CloneConfirmIterator<Corner>(liveCorners) )
            qu.addCorner( lc, context, true );

        // if we are not adding new events (and this isn't adding the input the first time)
        // this shouldn't do anything
        context.processHoriz( this );
    }

    /**
     * Debug!
     */
    public void validate()
    {
        if (false)
        {
        Set <Corner> all = new LinkedHashSet<> ( liveCorners );
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

    public void setPlanTags (Edge edge, Set<Tag> features)
    {
        planFeatures.put( edge, features );
    }

    public Set<Tag> getPlanTags( Edge originator )
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
        Set<Corner> togo = new HashSet<>(liveCorners);

        while (!togo.isEmpty())
        {
            Loop<Corner> loop = new Loop<>();
            out.add(loop);

            Corner start = togo.iterator().next();

            Corner current = start;
            int handbrake = 0;
            do
            {
                togo.remove(current);
                loop.append(current);

                current = current.nextC;
            }
            while (current !=start && handbrake++ < 1000);

            if (handbrake >= 1000)
            {
                System.err.println("broken loops in findLiveLoop");
                Thread.dumpStack();
            }
        }

        return out; //out.count();

    }
}        