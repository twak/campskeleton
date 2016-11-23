/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package straightskeleton.debug;

import java.awt.Color;
import java.awt.Graphics2D;
import javax.vecmath.Point2d;
import straightskeleton.Corner;
import straightskeleton.Edge;
import straightskeleton.Machine;
import straightskeleton.Skeleton;
import straightskeleton.ui.Bar;
import org.twak.utils.Cache;
import org.twak.utils.Loop;
import org.twak.utils.LoopL;

/**
 *
 * @author twak
 */
public class OffsetPointEditor extends SkeletonPointEditor
{

    @Override
    public void paintPointEditor(Graphics2D g2)
    {
        // override me!

        g2.setColor( Color.red );
        for (Loop<Bar> loop : edges)
        {
            for (Bar bar : loop)
                g2.drawLine(
                        ma.toX( bar.start.x ),
                        ma.toY( bar.start.y ),
                        ma.toX( bar.end.x ),
                        ma.toY( bar.end.y ) );
            g2.setColor( Color.orange );
        }

        for (Bar bar : edges.eIterator())
            drawPixel( g2, bar.start );

        final LoopL<Corner> out = new LoopL();

        /**
         * Start of skeleton code
         */
        Cache<Point2d, Corner> cCache = new Cache<Point2d, Corner>() {

            @Override
            public Corner create(Point2d i) {
                return new Corner(i.x, i.y);
            }
        };


        // controls the gradient of the edge
        final Machine machineO = new Machine (Math.PI/4);

        for ( Loop<Bar> lb : edges )
        {
            Loop<Corner> loop = new Loop();
            out.add( loop );

            for ( Bar bar : lb )
            {
                // 3D representation of 2D ui input
                Corner s = cCache.get (bar.start);
                Corner e = cCache.get (bar.end);

                s.nextL = new Edge( s,e );
                e.prevL = s.nextL;

                s.nextC = e;
                e.prevC = s;

                s.nextL.machine = machineO;

                loop.append( s );
            }
        }

        if (!busy && changed) {
            busy = true;
            changed = false;

            new Thread() {
                @Override
                public void run() {
                    try {

                        DebugDevice.reset();
                        Skeleton skeleton = new Skeleton(out, 50, true);
                        skeleton.skeleton();
                        OffsetPointEditor.this.output = skeleton.output;
                    } finally {
                        busy = false;
                        OffsetPointEditor.this.repaint(); // erk
                    }
                };
            }.start();
        }

        /**
         * End of skeleton code...well we just have to paint it:
         */

         paintMultiColourOutput( g2);
    }
    
}
