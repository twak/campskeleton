package utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * When you have a many -> many correspondence, you want this class!
 * 
 * @author twak
 */
public class ManyManyMap <A,B>
{   
    private MultiMap<A,B> forwards = new MultiMap();
    private MultiMap<B,A> backwards = new MultiMap();

    public void addForwards (A from, B to)
    {
        forwards.put( from, to );
        backwards.put( to, from );
    }

    public List<B> getNext(A from)
    {
        return forwards.get( from );
    }

    public List<A> getPrev(B to)
    {
        return backwards.get( to );
    }

    /**
     * Given a specified cache will convert everything that is an A to C. That
     * is the element type in the from will go from A to C.
     * @param <C> the replacement for A
     */

    public class ConvertInputCollection <C>
    {
        Cache<A, Collection<C>> converter;
        public ConvertInputCollection( Cache<A, Collection<C>> converter )
        {
            this.converter = converter;
        }

        public ManyManyMap<C, B> get()
        {
            ManyManyMap<C, B> out = new ManyManyMap<C, B>();

            for ( Map.Entry<A, List<B>> es : forwards.map.entrySet())
                for (C c : converter.get( es.getKey() ))
                    out.forwards.putAll( c, new ArrayList( es.getValue() ), true );

            for ( Map.Entry<B, List<A>> es : backwards.map.entrySet())
                for ( A a : es.getValue() )
                    out.backwards.putAll( es.getKey(), converter.get( a ), true );

            return out;
        }
    }


    public class ConvertOutputCollection <C>
    {
        Cache<B, Collection<C>> converter;
        public ConvertOutputCollection( Cache<B, Collection<C>> converter )
        {
            this.converter = converter;
        }

        public ManyManyMap<A, C> get()
        {
            ManyManyMap<A, C> out = new ManyManyMap<A, C>();

            for ( Map.Entry<A, List<B>> es : forwards.map.entrySet())
                for (B b : es.getValue())
                    out.forwards.putAll(es.getKey(), converter.get(b), true);

            for ( Map.Entry<B, List<A>> es : backwards.map.entrySet())
                for ( C c : converter.get(es.getKey()) )
                    out.backwards.putAll( c, new ArrayList (es.getValue()), true);

            return out;
        }
    }


//    public class ConvertInput <C>
//    {
//        Cache<A, C> converter;
//        public ConvertInput( Cache<A, C> converter )
//        {
//            this.converter = converter;
//        }
//
//        public ManyManyMap<C, B> get()
//        {
//            ManyManyMap<C, B> out = new ManyManyMap<C, B>();
//
//            for ( Map.Entry<A, List<B>> es : forwards.map.entrySet())
//                out.forwards.putAll( converter.get( es.getKey() ), new ArrayList( es.getValue() ), false );
//
//            for ( Map.Entry<B, List<A>> es : backwards.map.entrySet())
//                for ( A a : es.getValue() )
//                {
//                    out.backwards.put( es.getKey(), converter.get( a ), false );
//                }
//
//            return out;
//        }
//    }

    public Set<A> getSrcSet()
    {
        return forwards.keySet();
    }

    public Set<A> getDestSet()
    {
        return forwards.keySet();
    }

    public SetCorrespondence<A,B> toSetCorrespondence()
    {
        SetCorrespondence<A,B> out = new SetCorrespondence<A, B>();

        for (A a : forwards.keySet())
        {
            for (B b : forwards.get(a))
                out.put(a, b);
        }

        for (B b : backwards.keySet())
        {
            for (A a : backwards.get(b))
                out.put(a, b);
        }

        return out;
    }

    public ManyManyMap<B, A> getFlipShallow() {
        ManyManyMap<B, A> out = new ManyManyMap<B, A>();
        out.backwards = forwards;
        out.forwards = backwards;
        return out;
    }
}
