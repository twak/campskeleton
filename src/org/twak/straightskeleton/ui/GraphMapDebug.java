package org.twak.straightskeleton.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.vecmath.Point3d;
import org.twak.utils.GraphMap;

/**
 *
 * @author twak
 */
public class GraphMapDebug extends JComponent
{
    GraphMap<Point3d> map;
    public GraphMapDebug (GraphMap<Point3d> map)
    {
        this.map = map;
        JFrame frame = new JFrame();
        frame.setContentPane( this );
        frame.setSize (500,500);
        frame.setVisible(true);
    }

    @Override
    public void paint (Graphics g)
    {
        Map <Point3d, List<Point3d>> pts = map.map;

        for (Point3d p1 : pts.keySet())
        {
            for (Point3d p2 : pts.get( p1 ))
            {
                if (pts.get( p2 ).contains( p1 ))
                    g.setColor( Color.green );
                else
                    g.setColor( Color.red );
                
                g.drawLine( (int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y );
            }
            g.setColor( Color.pink );
            g.fillRect( (int) ( p1.x - 2 ), (int) ( p1.y - 2 ), 4, 4 );
            g.drawString( p1.toString(), (int) p1.x, (int) p1.y + 10 );
        }
    }
}
