package straightskeleton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface EdgeCreator
{

    /**
     * Returns a list of edges as replacement for the old edge. The old edge
     * extruded to the current height had start startH and end endH.
     *
     * The edges are expected to have valid start and end corners and Machines,
     * all nextC and prevC. All other pointers are derrived.
     * 
     * the first corner in the ordered chain must be startH, the last endH
     */
    public List<Corner> getEdges( Edge old, Corner startH, Corner endH );

    public Set<Feature> getFeaturesFor( Edge edgeH );

    public static class DefaultEdgeCreator implements EdgeCreator
    {
        public List<Corner> getEdges( Edge old, Corner startH, Corner endH )
        {
            List<Corner> out = new ArrayList();
            startH.nextL = old;
            endH.prevL = old;
//            e.angle = Math.PI / 4;
//            e.machine = old.machine; //?
            out.add( startH );
            out.add( endH );
            return out;
        }
        
        public Set<Feature> getFeaturesFor( Edge edgeH )
        {
            return new HashSet();
        }
    }
}


