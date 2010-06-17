package utils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hashset doesn't let youget the item being looked up :(
 * @author twak
 */
public class IdentityLookup <E>
{
    public Map <E,E> map = new LinkedHashMap();

    public void put (E e)
    {
        map.put (e,e);
    }

    public E get(E e)
    {
        E out = map.get( e );
        if ( out == null )
        {
            put( e );
            return e;
        }
        else
            return out;
    }


}
