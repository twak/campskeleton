package org.twak.camp.offset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.twak.camp.Corner;
import org.twak.camp.CornerClone;
import org.twak.camp.Edge;
import org.twak.camp.HeightEvent;
import org.twak.camp.Machine;
import org.twak.camp.Skeleton;
import org.twak.camp.Output.Face;
import org.twak.camp.debug.DebugDevice;
import org.twak.camp.ui.DirectionHeightEvent;
import org.twak.utils.Cache;
import org.twak.utils.collections.LoopL;
import org.twak.utils.collections.ManyManyMap;
import org.twak.utils.collections.SetCorrespondence;

/**
 *
 * An offset skeleton where the speed of each edge is specified on a per-edge
 * basis.
 *
 * input - given in constructor
 * old - the copy we make and store in var corners
 * output - the copies we return in the Offset
 *
 * @author twak
 */
public class PerEdgeOffsetSkeleton
{
     Offset output;

    LoopL<Corner> oldCorners;

    double step = 100;

    int machinesCount = 0;
    int machinesOutstanding = 0;
    int lastStep = -1;

    public SetCorrespondence<Corner, Corner> oldInputSegments;

    // the skeleton resulting from
    public Skeleton outputSkeleton;


    public PerEdgeOffsetSkeleton ( LoopL<Corner> corners, LoopL<Double> speeds )
    {
        setup (corners);

        Iterator<Corner> cit = corners.eIterator().iterator();
        Iterator<Double> sit = speeds.eIterator().iterator();

        while (cit.hasNext())
        {
            Corner c = cit.next();
            assert (sit.hasNext());
            double speed = sit.next();
            registerEdge(c, speed );
        }
    }

    public PerEdgeOffsetSkeleton ( LoopL<Corner> corners, double speed )
    {
        setup (corners);

        Iterator<Corner> cit = corners.eIterator().iterator();

        while (cit.hasNext())
        {
            Corner c = cit.next();
            registerEdge(c, speed );
        }
    }

    public PerEdgeOffsetSkeleton ( LoopL<Corner> corners )
    {
        setup (corners );
    }

   /**
    * If you use this constructor, you should call setup(shape) whith the
    * shape you intend to offset. You can call registerEdge at any time.
    */
    public PerEdgeOffsetSkeleton()
    {}

    public void setup ( LoopL<Corner> inputCorners )
    {
        // clone the input, so we're non-destructive
        CornerClone cc = new CornerClone(inputCorners);
        this.oldCorners = cc.output;
        oldInputSegments = cc.nOSegments;

        // default for all edges is 0
        for (Corner oldC : this.oldCorners.eIterator())
            oldC.nextL.machine = machineGenerator.get( new Double ( 0 ) );
    }

    Cache<Double, OffsetMachine> machineGenerator = new Cache<Double, OffsetMachine>()
    {
        @Override
        public OffsetMachine create(Double speed)
        {
            OffsetMachine om = new OffsetMachine();
            om.currentAngle = Math.atan2(speed, step); // <-- debug only
            om.addHeightEvent(new DirectionHeightEvent(om, 0,  Math.atan2(speed, step)));
            return om;
        }
    };

    public Map<Corner, Double> inputCornerToSpeed= new HashMap();

    public void registerEdge( Corner inputCorner, double speed )
    {
        inputCornerToSpeed.put(inputCorner, speed);
    }

    public Offset getResult()
    {
        // replace existing machines with ones of the correct speed
        for (Corner inputCorner : inputCornerToSpeed.keySet())
        {
            double speed = (Double)inputCornerToSpeed.get(inputCorner);
            for (Corner oldC : oldInputSegments.getSetB(inputCorner))
            {
                oldC.nextL.machine = machineGenerator.get(speed);
            }
        }

        for (Corner c : oldCorners.eIterator())
            c.z = 0;

        // create the skeleton
        outputSkeleton = new Skeleton ( oldCorners );
        outputSkeleton.name = "offset";

        InstanceHeightEvent last = null;

        DebugDevice.dump("p/e offset skeleton (in)", oldCorners);

        // add instancing events to capture result at given offset
        outputSkeleton.qu.add( last = new InstanceHeightEvent ( step ) );
        outputSkeleton.skeleton();

        DebugDevice.dump("p/e offset skeleton (out)", outputSkeleton);

//        System.out.println(">>>>p/e now done");

        // restore original machines in output
        for ( Corner newCorner : output.shape.eIterator()) //newCorner.nextL.machine.currentAngle
        {
            Machine original = null;
            for (Corner input : output.nOSegments.getNext(newCorner)) // old.nextL.machine.currentAngle
            {
                    if (original == null)
                        original = input.nextL.machine;

                    assert original == input.nextL.machine;
            }
            newCorner.nextL.machine = original;
//            original.addEdge(newCorner.nextL, outputSkeleton);
        }

        return output;
    }

    public List<Corner> getInputEdge( Face f )
    {
        Edge first = outputSkeleton.output.getGreatestGrandParent( f ).edge;
        return new ArrayList ( oldInputSegments.getSetA( first.start ) );
    }

    public class InstanceHeightEvent implements HeightEvent
    {
        double height;
        boolean endHere = false;

        public InstanceHeightEvent( double height )
        {
            this.height = height;
        }

        public double getHeight()
        {
            return height;
        }

        public boolean process( Skeleton skel )
        {
            LoopL<Corner> copy = skel.capCopy( height );

            // I would like to appologise to my future self for writing this...
            // (skel -> cap) segment map to (skel-before-direction-events -> cap)
            ManyManyMap<Corner, Corner> outputOldSegments = 
                    skel.segmentMap.new ConvertInputCollection<Corner>   ( skel.getSegmentOriginator() ).get();

            // to input -> cap
            ManyManyMap<Corner, Corner> inputCapSegments =
                    outputOldSegments.new ConvertInputCollection<Corner>( oldInputSegments.asCache () ).get();

            inputCapSegments = inputCapSegments.getFlipShallow();

            output = new Offset( copy , inputCapSegments ); // should be new->old segments
            
            // cap everything below to tidy up.
            for (Corner c : skel.liveCorners)
            {
                Corner top = skel.cornerMap.teg(c);

                skel.output.addOutputSideTo(c, top, c.nextL, c.prevL);
                skel.output.addOutputSideTo(true, top, top.nextC, c.nextL);
            }

            // no more output events pls!
            skel.liveEdges.clear();
            skel.liveCorners.clear();
            skel.qu.clearFaceEvents();

            return false;
        }
    }
}
