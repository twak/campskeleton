package org.twak.camp.ui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;

import org.twak.camp.Corner;
import org.twak.camp.Edge;
import org.twak.camp.HeightEvent;
import org.twak.camp.Machine;
import org.twak.camp.Skeleton;
import org.twak.camp.SkeletonCapUpdate;
import org.twak.camp.Output.Face;
import org.twak.camp.Output.SharedEdge;
import org.twak.camp.debug.DebugDevice;
import org.twak.camp.offset.FindNOCorner;
import org.twak.camp.offset.Offset;
import org.twak.camp.offset.OffsetSkeleton;
import org.twak.utils.Cache;
import org.twak.utils.collections.DHash;
import org.twak.utils.collections.LoopL;
import org.twak.utils.collections.SetCorrespondence;

/**
 * Represents a horizontal skeleton edge for a length, followed by an angle change
 * 
 * @author twak
 */
public class HorizontalHeightEvent extends DirectionHeightEvent
{
    double length;
    // features for the horizontal event (following section's feature in superclass)
//    public Set<Feature> horizProfileFeatures = new HashSet();

    public HeightEvent next = null;

    // bad experiment:
    @Deprecated public double finalHeight;

    public HorizontalHeightEvent( Machine machine, double height, double length )
    {
        super( machine, height, 0 ); // angle unused
        this.length = length;
        this.next = null;
    }

    public void whenDone(Skeleton skel)
    {
        // override this method to decide what gets done immediately after the horizontal growth has been modeled (before the next horizontal height)
    }

    @Override
    public boolean process( Skeleton skel )
    {
        final List<Edge> toChange = machine.findOurEdges( skel );

        // this is a horizontal edge, use an offset skeleton to calculate the new profile and a SkeletonCapUpdate to
        // flush it's changes into the current skeleton
        final SkeletonCapUpdate update = new SkeletonCapUpdate( skel );
        LoopL<Corner> cap = update.getCap( height ); //, finalHeight );

        DebugDevice.dump( "horiz initial cap", cap);

        double step = 100;
        OffsetSkeleton<Machine> offsetSkel = new OffsetSkeleton( cap, step );

        // all those edges with the machine this height event is in, offset the given distance (other edges will be 0)
        offsetSkel.registerProfile( machine, Math.atan( length / step ), 0 );

        Offset offset = offsetSkel.getResults().get( 0 );
        LoopL<Corner> offsetLoops = offset.shape;


        FindNOCorner findNOCorner = new FindNOCorner( offset, cap)
        {
            @Override
            public boolean didThisOldCornerRemainUnchanged( Corner oldC )
            {
                return oldC.prevL.machine != machine && oldC.nextL.machine != machine;
            }
        };

        SetCorrespondence<Corner, Corner> nOSegmentsUpdate = findNOCorner.nOSegmentsUpdate;
        DHash<Corner, Corner> nOCorner = findNOCorner.nOCorner;

        // remove from update segment map
         for (Corner neuC : offsetLoops.eIterator())
         {
             Set<Corner> matchingSegs = nOSegmentsUpdate.getSetA( neuC );

             for ( Corner oldC : matchingSegs )
             {
                if ( oldC.nextL.machine == machine )
                    nOSegmentsUpdate.removeA(neuC);
             }
         }

        // wire in the new profile to skeleton
        update.update( offsetLoops, nOSegmentsUpdate, nOCorner );

        Cache <Corner, Corner> cache = new Cache<Corner, Corner>()
        {
            @Override
            public Corner create( Corner i )
            {
                return new Corner (i.x, i.y, height);
            }
        };


        Map <Corner, Corner> baseCornerToOldFace = new LinkedHashMap();
        // add horizontal face to output mesh - fixme: does this add an extra face?
        for (Face f : offsetSkel.outputSkeleton.output.faces.values() )
        {
            // calculate the  input corner's defining edge start, to assign parent
            List<Corner> oldParent = offsetSkel.getInputEdge(f);//offsetSkel.outputSkeleton.output.getGrandestParent (f);
            Corner baseCorner = update.getOldBaseLookup().get( oldParent.get(0) ).nextL.start;

            Edge e = new Edge ( cache.get (f.edge.end), cache.get(f.edge.start ) );
            e.start.nextC = e.end; // variables that skel.output relies on existing.
            e.end.prevC = e.end;

            e.setAngle( length > 0 ? Math.PI / 4 : -Math.PI / 4 );

            skel.output.newEdge( e, baseCorner, profileFeatures);
            skel.output.newDefiningSegment( e.start );
            baseCornerToOldFace.put( baseCorner, e.start );

            e.uphill = e.direction();
            e.uphill.normalize();
            e.uphill.set( -e.uphill.y, e.uphill.x, 0 );
            
            
            for (SharedEdge se : f.edges.eIterator())
            {
                Tuple3d start = se.getStart( f ), end = se.getEnd( f );
                skel.output.addOutputSideTo( false, new Point3d (start.x, start.y, height),
                		new Point3d (end.x, end.y, height), e ); // it is unclear if these should be parent or child edges
            }
//            skel.output.faces.get(e.start).edge
        }

        // assign parent face to new edges. This is a mess, we go via the tmp lookup baseCornerToOldFace :(
        for (Corner c : skel.liveCorners)
        {
            if (c.nextL.machine == machine)
            {
                List<Corner> sc = offset.nOSegments.getNext( c );
                for (Corner cc : sc )
                {
                    Corner base = update.getOldBaseLookup().get( cc );
//                    Face neu = skel.output.faces.get( c.nextL.start );
//                    neu.parent = baseCornerToOldFace.get( base.nextL.start );
                    skel.output.setParent( c.nextL.start,  baseCornerToOldFace.get( base.nextL.start ));
                    break; // fixme: should be able add more than one parent?
                }
            }
        }

//        for ( Edge e : machine.findOurEdges( skel ) )
//            e.setAngle( newAngle );

        skel.refindAllFaceEventsLater();

        skel.validate();

        DebugDevice.dump( "post-horizontal", skel);

        machine.findNextHeight( skel );

        whenDone(skel);

        if (next != null)
            next.process(skel);

        // now set the angle on the new edges
        return true;
    }
}
