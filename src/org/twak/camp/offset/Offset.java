package org.twak.camp.offset;


import org.twak.camp.Corner;
import org.twak.utils.LoopL;
import org.twak.utils.ManyManyMap;

/**
 *
 * @author twak
 */
public class Offset
{
    // the shape of the offset at the given height
    public LoopL<Corner> shape;
    // Contains a map between the input edges and the edges in shape, above.
    public ManyManyMap<Corner, Corner> nOSegments;

    public Offset(LoopL<Corner> shape, ManyManyMap<Corner, Corner> nOSegments)
    {
        this.shape = shape;
        this.nOSegments = nOSegments;
    }
}
