package straightskeleton.ui;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import straightskeleton.Corner;
import straightskeleton.Edge;
import straightskeleton.Feature;
import straightskeleton.Machine;
import straightskeleton.OffsetSkeleton;
import straightskeleton.OffsetSkeleton.Offset;
import straightskeleton.Output.Face;
import straightskeleton.Output.SharedEdge;
import straightskeleton.Skeleton;
import straightskeleton.SkeletonCapUpdate;
import straightskeleton.debug.DebugDevice;
import utils.Cache;
import utils.DHash;
import utils.LoopL;
import utils.SetCorrespondence;

/**
 * Represents a horizontal skeleton edge for a length, followed by an angle change
 * 
 * @author twak
 */
public class HorizontalHeightEvent extends DirectionHeightEvent
{
    double length;
    // features for the horizontal event (following section's feature in superclass)
    public Set<Feature> horizProfileFeatures = new HashSet();

    public HorizontalHeightEvent( Machine machine, double height, double angle, double length )
    {
        super( machine, height, angle );
        this.length = length;
    }


    @Override
    public boolean process( Skeleton skel )
    {
        final List<Edge> toChange = machine.findOurEdges( skel );

        // this is a horizontal edge, use an offset skeleton to calculate the new profile and a SkeletonCapUpdate to
        // flush it's changes into the current skeleton
        final SkeletonCapUpdate update = new SkeletonCapUpdate( skel );
        LoopL<Corner> cap = update.getCap( height );

//        DebugDevice.dump( "horiz initial cap", cap);

        double step = 100;
        OffsetSkeleton<Machine> offsetSkel = new OffsetSkeleton( cap, step );

        // all those edges with the machine this height event is in, offset the given distance (other edges will be 0)
        offsetSkel.registerProfile( machine, Math.atan( length / step ), 0 );

        Offset offset = offsetSkel.getResults().get( 0 );
        LoopL<Corner> offsetLoops = offset.shape;


        OffsetSkeleton.FindNOCorner findNOCorner = new OffsetSkeleton.FindNOCorner( offset, cap)
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

            e.setAngle( length > 0 ? Math.PI / 2 : -Math.PI / 2 );

            skel.output.newEdge( e, baseCorner, horizProfileFeatures);
            skel.output.newDefiningSegment( e.start );
            baseCornerToOldFace.put( baseCorner, e.start );

            for (SharedEdge se : f.edges.eIterator())
            {
                Tuple3d start = se.getStart( f ), end = se.getEnd( f );
                skel.output.addOutputSideTo( new Point3d (start.x, start.y, height), new Point3d (end.x, end.y, height), e );
            }
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

        for ( Edge e : machine.findOurEdges( skel ) )
            e.setAngle( newAngle );

        skel.refindAllFaceEventsLater();

        skel.validate();

        machine.findNextHeight( skel );

        DebugDevice.dump( "post-horizontal", skel);

        // now set the angle on the new edges
        return true;
    }

    // delme: moved to offsetskeleton.findnocorner
//        /**
//         * The maps that come out of offset relate the old edge positions to their
//         * new positions, not the direct edge or corner correspondence that they do in
//         * cap update => goal:
//         *
//         * if both adjacent edges of speed 0, entry in nOCorner.
//         * for all input corners:
//         *    if equivilent edges exist in cap output, and had speed 0
//         *        add an entry between (find new corner between two edges) -> existing
//         *
//         * if segment's edge has speed 0, entry in nOSegment
//         **/
//
//        // add all valid corners to nOCorner
//        for (Corner oldC : cap.eIterator())
//        {
//            // only if both edges are 0-speed (don't use our machine)
//            if ( oldC.prevL.machine != machine && oldC.nextL.machine != machine )
//            {
//                // two edges in the
//                Set<Corner> second = nOSegmentsUpdate.getSetB( oldC );
//                Set<Corner> first = nOSegmentsUpdate.getSetB( oldC.prevC );
//
//                Corner neuC = findAdjacent( first, second );
//
//                if ( neuC != null ) // corner hasn't been swallowed
//                    nOCorner.put( neuC, oldC );
//            }
//        }



}
