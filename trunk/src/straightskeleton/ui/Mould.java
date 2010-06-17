package straightskeleton.ui;

import java.util.ArrayList;
import java.util.List;
import javax.vecmath.Point2d;

/**
 * A mould does run-time generation of anchors given an instance of a bar
 * @author twak
 */
public class Mould
{   
    public List<Marker> getAnchors( Point2d start, Point2d end, Point2d ... additional )
    {
        return new ArrayList(); 
    }

}
