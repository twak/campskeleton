
package utils;

import java.util.List;

/**
 * Two directional multi map
 *
 * 
 *
 * @author twak
 */
public class DMultiMap<A,B>
{
    public MultiMap <A,B> ab = new MultiMap();
    public MultiMap <B,A> ba = new MultiMap();

    public void clear()
    {
        ab.clear();
        ba.clear();
    }
    
    public void put (A a, B b)
    {
        ab.put(a,b);
        ba.put(b,a);
    }
    
    public List<B> get (A a)
    {
        return ab.get(a);
    }
    
    public List<A> teg (B b)
    {
        return ba.get(b);
    }

    public boolean containsKeyA(A a)
    {
        return ab.containsKey( a );
    }
    
    public boolean containsKeyB(B b)
    {
        return ba.containsKey( b );
    }

    // haven't thought about these yet...
//    @Override
//    public int hashCode()
//    {
//        return ab.hashCode() + ba.hashCode();
//    }
//
//    @Override
//    public boolean equals( Object obj )
//    {
//        if (obj instanceof DMultiMap)
//        {
//            DMultiMap o = (DMultiMap)obj;
//            return o.ab.equals( ab ) && o.ba.equals( ba );
//        }
//        return false;
//    }
}
