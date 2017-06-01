package org.twak.camp.ui;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.twak.camp.Corner;
import org.twak.camp.CornerClone;
import org.twak.camp.Edge;
import org.twak.camp.HeightEvent;
import org.twak.camp.Machine;
import org.twak.camp.Skeleton;
import org.twak.camp.SkeletonCapUpdate;
import org.twak.camp.Tag;
import org.twak.camp.Output.Face;
import org.twak.camp.debug.DebugDevice;
import org.twak.utils.DHash;
import org.twak.utils.LoopL;

public class DirectionHeightEvent implements HeightEvent
{
    protected double height;
    // newAngle is the angle that this edge will turn towards at the above height
    // length is the distance until the next event (only used for horizontal directions)
    public double newAngle;
    Machine machine;
    public Set<Tag> profileFeatures = new HashSet();

    public DirectionHeightEvent( Machine machine, double angle )
    {
        this( machine, 0, angle );
    }

    public double getAngle()
    {
        return newAngle;
    }

    public DirectionHeightEvent( Machine machine, double height, double angle )
    {
        super();
        this.machine = machine;
        this.height = height;
        this.newAngle = angle;
    }

    public double getHeight()
    {
        return height;
    }

    public boolean process( Skeleton skel )
    {
//        System.out.println("machine "+machine.toString()+" at "+height+" setting angle "+newAngle );

        // set the new angle
        machine.currentAngle = newAngle;

        SkeletonCapUpdate update = new SkeletonCapUpdate(skel);

        // add in the output edges for the outgoing face:
        LoopL<Corner> cap = update.getCap(height);
        DebugDevice.dump( "cap", cap );

        CornerClone cc = new CornerClone(cap);

        // preserve corner information for assigning parents, later
        DHash<Corner,Corner> nOCorner = cc.nOCorner.shallowDupe();

        for ( Corner c : cc.output.eIterator() )
        {
            // corners are untouched if neither attached edge has this machine
            if ( c.nextL.machine == machine || c.prevL.machine == machine )
                nOCorner.removeA( c );

            // segments are untouched if they don't contain this machine
            if ( c.nextL.machine == machine )
            {
                // copy over profile features
                // yes, it looks like we add features to an edge we later disable, but it is still referenced in the entire loop and gets used for edge properties.
                c.nextL.profileFeatures = profileFeatures;
                
                cc.nOSegments.removeA( c );
            }
        }

        update.update( cc.output, cc.nOSegments, nOCorner);

        /**
         * Must now update the parent field in output for the new edges, so that
         * we know where a face came from
         */
        for ( Corner c : skel.liveCorners )
        {
            if ( c.nextL.machine == machine )
            {
                Corner old = update.getOldBaseLookup().get( cc.nOCorner.get( c ));
                skel.output.setParent (c.nextL.start, old.nextL.start);
            }
        }

        DebugDevice.dump( "post height "+height, skel);

        machine.findNextHeight( skel );
        return true;
    }
}



//        skel.replaceEdges( toChange, new EdgeCreator()
//        {
//            public List<Corner> getEdges( Edge old, Corner startH, Corner endH )
//            {
//                Edge e = new Edge( startH, endH );
//                e.setAngle( newAngle );
//                e.machine = machine;
//
//                startH.nextL = e;
//                endH.prevL = e;
//
//                List<Corner> out = new ArrayList();
//                out.add( startH );
//                out.add( endH );
//                return out;
//            }

//            public Set<Feature> getFeaturesFor( Edge edgeH )
//            {
//                return profileFeatures;
//            }
//        }, height );
