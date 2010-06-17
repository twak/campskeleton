package utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Data type form mappings between two sets of sets.
 * @author twak
 */
public class SetCorrespondence <A extends Object,B extends Object>
{
    Map<A, Set<A>>whichA = new LinkedHashMap();
    Map<B, Set<B>>whichB = new LinkedHashMap();
    DHash<Set<A>,Set<B>>setToSet = new  DHash();

    /**
     * If either a or b belongs to an existing set, both a and b collapse into that a/b set pair.
     * Symmetrical operation.
     * 
     * @param a the A
     * @param b the B
     */
    public void put(A a, B b)
    {
        Set<A> aSet = getSet(a, whichA);
        Set<B> bSet = getSet(b, whichB);

        Set shouldBeA = setToSet.teg( bSet );
        Set shouldBeB = setToSet.get( aSet );

        if (shouldBeA != null && shouldBeA != aSet) // existing B set already references another A set
        {
            assert(aSet.size() == 1);
            shouldBeA.addAll( aSet );
            for (A a2 : aSet)
                whichA.put(a2, shouldBeA);
            aSet = shouldBeA;
        }
        else
        {
            whichA.put(a, aSet); // either new set, or
        }

        if (shouldBeB != null && shouldBeB != bSet) // a references another b (not bSet)
        {
            assert(bSet.size() == 1);
            shouldBeB.addAll(bSet);
            for (B b2 : bSet)
                whichB.put(b2, shouldBeB);
            bSet = shouldBeB;
        }
        else
        {
            whichB.put(b, bSet);
        }


        setToSet.put( aSet, bSet );
    }


    public Set<B> getSetA (A a)
    {
        Set<A> aSet = getSet (a, whichA);
        Set<B> bSet = setToSet.get( aSet );
        if (bSet == null)
            return new HashSet();
        return bSet;
    }
    
    public Set<A> getSetB (B b)
    {
        Set<B> bSet = getSet (b, whichB);
        Set<A> aSet = setToSet.teg( bSet );
        if (aSet == null)
            return new HashSet();
        return aSet;
    }

    public Set getSet(Object o, Map set)
    {
        Set res = (Set)set.get(o);
        if (res == null)
        {
            res = new LinkedHashSet();
            res.add(o);
            set.put( o, res );
        }
        assert (res.contains(o));
        return res;
    }

    public void removeA(A a)
    {
        Set<A> aSet = getSet (a, whichA);
        aSet.remove( a );
        whichA.remove( a );
        if (aSet.isEmpty())
            setToSet.removeA(aSet);
    }


    public Cache<A, Collection<B>> asCache ()
    {
        return new Cache<A, Collection<B>>()
        {
            @Override
            public Collection<B> create(A i) {
                return getSetA(i);
            }
        };
    }

    public class ConvertB<C>
    {
        SetCorrespondence<B, C> lookup;
        
        public ConvertB ( SetCorrespondence<B, C> lookup )
        {
            this.lookup = lookup;
        }
        
        public SetCorrespondence<A, C> convert()
        {
            SetCorrespondence<A, C> out = new SetCorrespondence();

            out.whichA = new LinkedHashMap<A, Set<A>>( whichA );
            out.whichB = new LinkedHashMap<C, Set<C>>( lookup.whichB );

            for ( Set<A> sa : setToSet.ab.keySet() )
                for ( A a : sa )
                    for ( B b : getSetA( a ) )
                         for ( C c : lookup.getSetA( b ) )
                             out.put( a, c );

            return out;
        }
    }
}
