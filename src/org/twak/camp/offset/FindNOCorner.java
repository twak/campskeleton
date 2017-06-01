package org.twak.camp.offset;

import java.util.Set;

import org.twak.camp.Corner;
import org.twak.utils.DHash;
import org.twak.utils.LoopL;
import org.twak.utils.SetCorrespondence;

/**
 * The output of an offset surface only knows about corresponding edges, not corresponding corners. Given
 * that you know if an old corner will have moved (did you assign a weight to it's machine?) this class recovers
 * the map.
 *
 * Unmoved corners end up in nOCorner.
 * Equivilent edges end up in nOSegment (this may not be what you want)
 *
 */
public abstract class FindNOCorner
{
    // results end up here
    public SetCorrespondence<Corner, Corner> nOSegmentsUpdate;
    public DHash<Corner, Corner> nOCorner = new DHash<Corner, Corner>();

    public FindNOCorner(Offset offset, LoopL<Corner> cap)
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
        for (Corner oldC : cap.eIterator())
            // only if both edges are 0-speed (don't use our machine)
            if (didThisOldCornerRemainUnchanged(oldC))
            {
                // two edges in the
                Set<Corner> second = nOSegmentsUpdate.getSetB(oldC);
                Set<Corner> first = nOSegmentsUpdate.getSetB(oldC.prevC);
                Corner neuC = findAdjacent(first, second);
                if (neuC != null)
                    nOCorner.put(neuC, oldC);
            }
    }

    /**
     * Given two sets of leading corners, find the corner (if any) that lies on
     * both edges
     *
     * @return
     */
    private Corner findAdjacent(Set<Corner> first, Set<Corner> second)
    {
        for (Corner c : first)
            if (second.contains(c.nextC))
                return c.nextC;
        return null;
    }

    public abstract boolean didThisOldCornerRemainUnchanged(Corner oldCorner);
}
