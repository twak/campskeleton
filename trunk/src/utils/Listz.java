package utils;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author twak
 */
public class Listz {
    public static List union (List a, List b)
    {
        List out = new ArrayList();
        for (Object oa : a)
            if (b.contains (oa))
                out.add(oa);
        return out;
    }
}
