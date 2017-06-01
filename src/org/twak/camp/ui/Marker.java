package org.twak.camp.ui;

import java.util.HashMap;
import java.util.Map;

import javax.vecmath.Point2d;

import org.twak.camp.Tag;

/**
 * marks the locatino of a feature on a Bar
 * @author twak
 */
public class Marker extends Point2d
{
    @Deprecated
    public Tag feature;
    
    public Bar bar;

    // what created this marker?
    public Object generator;

    @Deprecated
    public Map<String, Object> properties = new HashMap();
    
    @Deprecated
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

    @Deprecated
    public Marker(Tag feature) {
        this.feature = feature;
        properties.put(TYPE, Type.Rel);
    }

    public Marker() {
    }
}
