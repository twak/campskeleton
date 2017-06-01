package org.twak.camp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.twak.camp.Output.Face;
import org.twak.camp.ui.DirectionHeightEvent;
import org.twak.utils.DHash;
import org.twak.utils.Loop;
import org.twak.utils.LoopL;
import org.twak.utils.ManyManyMap;
import org.twak.utils.SetCorrespondence;

/**
 * input - given in constructor
 * old - the copy we make and store in var corners
 * output - the copies we return in the Offset
 *
 * @author twak
 */
public class OffsetSkeleton<E extends Machine>
{
    private List<Offset> output;

    public Map<E, OffsetMachine> profileOffset = new LinkedHashMap();
    public Map<OffsetMachine, E> offsetProfile = new LinkedHashMap();

    public double interval;

    LoopL<Corner> corners;

    int machinesCount = 0;
    int machinesOutstanding = 0;
    int lastStep = -1;

    SetCorrespondence<Corner, Corner> oldInputSegments;

    // the skeleton resulting from
    public Skeleton outputSkeleton;

    public OffsetSkeleton ( LoopL<Corner> corners, double interval )
    {
        this.interval = interval;

        // clone the input, so we're non-destructive
        CornerClone cc = new CornerClone(corners);
        this.corners = cc.output;
        oldInputSegments = cc.nOSegments;
    }

    /**
     * We ignore the contents of the profile, and just use it as a marker to set the specified offset
     * at teh specified height.
     *
     * @param step - which multiple of interval are we at?
     */
    public void registerProfile( E profile, double angle, int step )
    {
        assert(profile != null);
        double height  = step * interval;
        OffsetMachine om = profileOffset.get( profile );
        if (om == null)
        {
            om = new OffsetMachine();
            profileOffset.put( profile, om );
            offsetProfile.put( om, profile );
            machinesCount++;
        }

        om.addHeightEvent( new DirectionHeightEvent (om, height, angle));

        if (step > lastStep)
        {
            lastStep = step;
        }
    }

//    public List<Offset> getResults()
//    {
//        return getResults( false );
//    }
    
    public List<Offset> getResults()//boolean kludge)
    {
        output = new ArrayList();

        Map<Machine, Machine> unspecifiedNewMachineOld = new HashMap();

        // assign the new machines we've created
        for ( Corner c : corners.eIterator() )
        {
            Edge e = c.nextL;
            Machine m = profileOffset.get( e.machine );
            if ( m == null )
            {
                // default angle is 0
                m = new Machine(0);
                unspecifiedNewMachineOld.put( m, e.machine );
                e.machine = m;
            }
            else
            {
                e.machine = m;
            }
        }
        outputSkeleton = new Skeleton ( corners, (lastStep + 1) * interval, true );
        outputSkeleton.name = "offset";

        InstanceHeightEvent last = null;

        // add instancing events to capture skeleton cap at each height
        for (int i = 0; i <= lastStep; i++)
            outputSkeleton.qu.add( last = new InstanceHeightEvent (i) );

        if (last != null)
            last.endHere = true;

//        DebugWindow.showIfNotShown( edges );
        outputSkeleton.skeleton();

        // restore original machines for calling routine
        for ( Offset offset : output )
        {
            for ( Edge e : Edge.uniqueEdges( offset.shape ) )
            {
                assert ( e.machine != null );
                Machine origMachine = offsetProfile.get( e.machine );
                if (origMachine != null)
                    e.machine = origMachine;
                else
                    e.machine = unspecifiedNewMachineOld.get( e.machine );
                e.machine.addEdge( e, outputSkeleton );
            }
        }


//        for ( Face f : outputShape.output.faces.values() )// new HashSet ( outputShape.output.faces.values().size()).size()
//        {
//            Edge e = f.edge;
//
//            System.out.println(">>> "+e);
//
//            Machine origMachine = offsetProfile.get( e.machine );
//            if ( origMachine != null )
//                e.machine = origMachine;
//            else
//                e.machine = unspecifiedNewMachineOld.get( e.machine );
//            e.machine.addEdge( e, outputShape );
//        }

        return output;
    }

    public List<Corner> getInputEdge( Face f )
    {
        Edge first = outputSkeleton.output.getGreatestGrandParent( f ).edge;
        return new ArrayList ( oldInputSegments.getSetA( first.start ) );
    }

    public class InstanceHeightEvent implements HeightEvent
    {
        int step;
        double height;
        boolean endHere = false;

        public InstanceHeightEvent( int step )
        {
            this.step = step;
            height = Math.nextAfter( (step+1) * interval, -1 ); // just before we change direction for the next set of edges
        }

        public double getHeight()
        {
            return height;
        }

        public boolean process( Skeleton skel )
        {
            LoopL<Corner> copy = skel.capCopy( height );

            // I would like to appologise to my future self for writing this method.
            // (skel -> cap) segment map to (skel-before-direction-events -> cap)
            ManyManyMap<Corner, Corner> outputOldSegments = 
                    skel.segmentMap.new ConvertInputCollection<Corner>   ( skel.getSegmentOriginator() ).get();

            // to input -> cap
            ManyManyMap<Corner, Corner> inputCapSegments =
                    outputOldSegments.new ConvertInputCollection<Corner>( oldInputSegments.asCache () ).get();

            inputCapSegments = inputCapSegments.getFlipShallow();

            output.add( new Offset( copy , inputCapSegments ) ); // should be new->old segments
            
            if ( endHere ) // fixme: part of a kludge-fix. seems to be responsible for final cap.
            {
                // cap everything below to tidy up.
                for ( Corner c : skel.liveCorners )
                {
                    Corner top = skel.cornerMap.teg( c );

                    skel.output.addOutputSideTo( c, top, c.nextL, c.prevL );
                    skel.output.addOutputSideTo( true, top, top.nextC, c.nextL );
                }

                // no more output events pls!
//                skel.output.faces.clear();
                skel.liveEdges.clear();
                skel.liveCorners.clear();
                skel.qu.clearFaceEvents();
            }


            return false;
        }
    }

    public static class Offset
    {
        // the shape of the offset at the given height
        public LoopL<Corner> shape;

        // Contains a map between the input edges and the edges in shape, above.
        public ManyManyMap<Corner, Corner> nOSegments;

        public Offset( LoopL<Corner> shape, ManyManyMap<Corner, Corner> nOSegments )
        {
            this.shape = shape;
            this.nOSegments = nOSegments;
        }
    }

    public class OffsetMachine extends Machine
    {
//        E profile;

        public OffsetMachine ()
        {
            super();
            events.clear();
        }
    }


    public static LoopL<Corner> shrink (LoopL<Edge> in, double dist)
    {
    	LoopL<Corner> cLoopL = Corner.cornerToEdgeLoopL( in );
    	
        OffsetSkeleton<Machine> os = new OffsetSkeleton<Machine>( cLoopL, 100 );

        Set<Machine> allMachines = new HashSet();
        for (Edge e : in.eIterator() )
            allMachines.add(e.machine);

        for (Machine m : allMachines)
            os.registerProfile( m, Math.atan( dist/100 ), 0 );

        List<Offset> res = os.getResults();

        if (res.isEmpty())
            return new LoopL();
        else
            return res.get( 0 ).shape;
    }



    /**
     * The output of an offset surface only knows about corresponding edges, not corresponding corners. Given
     * that you know if an old corner will have moved (did you assign a weight to it's machine?) this class recovers
     * the map.
     *
     * Unmoved corners end up in nOCorner.
     * Equivilent edges end up in nOSegment (this may not be what you want)
     *
     */
    public static abstract class FindNOCorner
    {
        // results end up here
        public SetCorrespondence<Corner, Corner> nOSegmentsUpdate;
        public DHash<Corner, Corner> nOCorner = new DHash<Corner, Corner>();

        public FindNOCorner( Offset offset, LoopL<Corner> cap )
        {
            nOSegmentsUpdate = offset.nOSegments.toSetCorrespondence();

            /**
             * The maps that come out of offset relate the old edge positions to their
             * new positions, not the direct edge or corner correspondence that they do in
             * cap update => goal:
             *
             * if both adjacent edges of speed 0, entry in nOCorner.
             * for all input corners:
             *    if equivilent edges exist in cap output, and had speed 0
             *        add an entry between (find new corner between two edges) -> existing
             *
             * if segment's edge has speed 0, entry in nOSegment
             **/
            // add all valid corners to nOCorner
            for ( Corner oldC : cap.eIterator() )
                // only if both edges are 0-speed (don't use our machine)
                if ( didThisOldCornerRemainUnchanged( oldC ) )
                {
                    // two edges in the
                    Set<Corner> second = nOSegmentsUpdate.getSetB( oldC );
                    Set<Corner> first = nOSegmentsUpdate.getSetB( oldC.prevC );

                    Corner neuC = findAdjacent( first, second );

                    if ( neuC != null ) // corner hasn't been swallowed
                        nOCorner.put( neuC, oldC );
                }

        }

        /**
         * Given two sets of leading corners, find the corner (if any) that lies on
         * both edges
         *
         * @return
         */
        private Corner findAdjacent( Set<Corner> first, Set<Corner> second )
        {
            for ( Corner c : first )
                if ( second.contains( c.nextC ) )
                    return c.nextC;

            return null;
        }

        public abstract boolean didThisOldCornerRemainUnchanged( Corner oldCorner );
    }

}
