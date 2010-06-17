package straightskeleton.ui;

import java.util.HashMap;
import java.util.Map;
import straightskeleton.Feature;
import javax.vecmath.Point2d;

/**
 * marks the locatino of a feature on a Bar
 * @author twak
 */
public class Marker extends Point2d
{
    public Feature feature;
    public Bar bar;

    public Map<String, Object> properties = new HashMap();
    public final static String TYPE = "type";

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
    
    public Marker (Feature feature)
    {
        this.feature = feature;
        properties.put( TYPE, Type.Rel);
    }
}
