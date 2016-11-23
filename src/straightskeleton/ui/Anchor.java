package straightskeleton.ui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import straightskeleton.Tag;
import javax.vecmath.Point2d;

/**
 * marks the locatino of a feature on a Bar
 * @author twak
 */
public class Anchor extends Point2d
{
    public Tag feature;
    public Bar bar;

    public Map<String, Object> properties = new HashMap();
    public final static String TYPE = "type";

    // none of this is searilized?
    public transient Set<F2> features = new HashSet();

    public enum Type
    {
        AbsStart ("absolute from start"), AbsEnd ("absolute from end"), Rel  ("relative");
        String name;
        
        Type(String name)
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }
    
    public Anchor (Tag feature)
    {
        this.feature = feature;
        properties.put( TYPE, Type.Rel);
    }
}
