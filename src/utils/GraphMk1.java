package utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Undirected graph
 *
 * @author twak
 */
public class GraphMk1 <E>
{
    Set<E> nodes = new HashSet();
    MultiMap<E,E> edges = new MultiMap();
    
    public GraphMk1(Collection<E> nodes)
    {
        this.nodes.addAll(nodes);
    }

    public void addEdge (E one, E two)
    {
        assert (nodes.contains (one));
        assert (nodes.contains (two));
        edges.put( one, two);
        edges.put( two, one);
    }

    public Set<Set<E>> allSubgraphs()
    {
        Set<Set<E>> out = new HashSet();
        Set<E> toVisit = new HashSet( nodes );

        while (!toVisit.isEmpty())
        {
            Set<E> adjacent = new HashSet();
            Set<E> localToVisit = new HashSet();
            localToVisit.add (toVisit.iterator().next());
            toVisit.removeAll( localToVisit );

            while (!localToVisit.isEmpty())
            {
                E current = localToVisit.iterator().next();
                localToVisit.remove( current );
                
                adjacent.add(current);
                
                for (E dest : edges.get( current ))
                    if (toVisit.contains( dest ))
                    {
                        localToVisit.add(dest);
                        toVisit.remove( dest );
                    }
                
            }
            if (adjacent.size() > 1)
                out.add(adjacent);
        }
        
        return out;
    }
}
