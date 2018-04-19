/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.twak.camp;

import org.twak.utils.Cache;
import org.twak.utils.collections.DHash;
import org.twak.utils.collections.Loop;
import org.twak.utils.collections.LoopL;
import org.twak.utils.collections.SetCorrespondence;

/**
 *
 * @author twak
 */
public class CornerClone
{
    public LoopL<Corner> output = new LoopL();
    public SetCorrespondence<Corner, Corner> nOSegments = new SetCorrespondence(); 
    public DHash<Corner, Corner> nOCorner = new DHash();

    /**
     * Clones a new set of corners, edges.
     *
     * Edges machines are not cloned
     *
     * @param input the input to be cloned. Result dumped into variables.
     */
    public CornerClone(LoopL<Corner> input)
    {

        final Cache<Corner, Corner> cornerCache = new Cache<Corner, Corner>() {
            @Override
            public Corner create(Corner i) {
                return new Corner (i);
            }
        };

         Cache<Edge, Edge> edgeCache = new Cache<Edge, Edge>()
         {
            @Override
            public Edge create( Edge i )
            {
                Edge edge = new Edge ( cornerCache.get( i.start ), cornerCache.get ( i.end ) );


                edge.setAngle( i.getAngle() );
                edge.machine = i.machine; // nextL is null when we have a non root global
//              edge.profileFeatures = new LinkedHashSet<Feature>(current.nextL.profileFeatures);
//              edgeMap.put( edge, current.nextL );

                for (Corner c : i.currentCorners)
                    edge.currentCorners.add(cornerCache.get(c));

                return edge;
            }
         };

         for (Loop<Corner> inputLoop : input)
         {

             Loop<Corner> loop = new Loop();

            output.add( loop );

            for (Corner current : inputLoop)
            {
                Corner s = cornerCache.get( current ),
                       e = cornerCache.get( current.nextC );

                // one edge may have two segments, but the topology will not change between old and new,
                // so we may store the leading corner to match segments
                nOSegments.put(s, current);
                nOCorner.put(s, current);

                Edge edge = edgeCache.get( current.nextL );

                loop.append( s );
                s.nextC = e;
                e.prevC = s;
                s.nextL = edge;
                e.prevL = edge;
            }
         }
    }

    public boolean addSegment()
    {
        return true;
    }

    public boolean addCorner()
    {
        return true;
    }
}
