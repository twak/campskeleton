package org.twak.camp.ui;

import java.util.ArrayList;
import java.util.List;
import javax.vecmath.Point2d;

/**
 * A mould does run-time generation of anchors given an instance of a bar
 * @author twak
 */
public abstract class Mould
{   
    // use this for reading the anchors
    public List<Marker> getAnchorsReadOnly(Point2d start, Point2d end, Point2d... additional)
    {
        return getAnchorsForEditing(null, start, end, additional);
    }
    // and htis for the editors
    public abstract List<Marker> getAnchorsForEditing(Bar b, Point2d start, Point2d end, Point2d... additional);
    public abstract Object remove(Marker m);
    public abstract void create(Marker m, Object generator);
    
    public List<Marker> markersOn(Bar b)
    {
        return getAnchorsReadOnly( b.start, b.end );
    }

    public void clear() {
    }
}
