/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package straightskeleton.debug;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.MouseEvent;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import straightskeleton.Edge;
import straightskeleton.Machine;
import straightskeleton.Output;
import straightskeleton.Output.Face;
import straightskeleton.Skeleton;
import straightskeleton.ui.Bar;
import straightskeleton.ui.PointEditor;
import utils.LContext;
import utils.Loop;
import utils.LoopL;
import utils.Loopable;

/**
 *
 * @author twak
 */
public class SkeletonPointEditor extends PointEditor
{
    boolean changed = true;

    @Override
    public void movePoint(LContext<Bar> ctx, Point2d pt, Point2d location, MouseEvent evt) {
        changed = true;
        super.movePoint(ctx, pt, location, evt);
    }

    @Override
    public void remove(LContext<Bar> ctx, Point2d dragged) {

        changed = true;

        super.remove(ctx, dragged);
    }


    public void addBetween( LContext<Bar> ctx, Point l )
    {
        changed = true;
        super.addBetween(ctx, l);
    }


    boolean busy = false;
    Output output;
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

        final LoopL <Edge> out = new LoopL();


        /**
         * Start of skeleton code
         */

        // controls the gradient of the edge
        Machine machine = new Machine (Math.PI/4);

        for ( Loop<Bar> lb : edges )
        {
            Loop<Edge> loop = new Loop();
            out.add( loop );

            for ( Bar bar : lb )
            {
                // 3D representation of 2D ui input
                Edge e = new Edge(
                        new Point3d( bar.start.x, bar.start.y, 0 ),
                        new Point3d( bar.end.x, bar.end.y, 0 ),
                        Math.PI / 4 );

                e.machine = machine;

                loop.append( e );
            }

            // the points defining the start and end of a loop must be the same object
            for ( Loopable<Edge> le : loop.loopableIterator() )
                le.get().end = le.getNext().get().start;
        }

        if (!busy && changed) {
            busy = true;
            changed = false;

            new Thread() {
                @Override
                public void run() {
                    try {

                        DebugDevice.reset();
                        Skeleton skeleton = new Skeleton(out, true);
                        skeleton.skeleton();
                        SkeletonPointEditor.this.output = skeleton.output;
                    } finally {
                        busy = false;
                        SkeletonPointEditor.this.repaint(); // erk
                    }
                };
            }.start();
        }

        /**
         * End of skeleton code...well we just have to paint it:
         */

         paintMultiColourOutput( g2);
    }

    public void paintMultiColourOutput(Graphics2D g2) {

        if ( output != null && output.faces != null )
        {
            int faceCount = output.faces.size();
            g2.setStroke( new BasicStroke( 1 ) );

            int faceIndex = 0;

            for ( Face face : output.faces.values() )
            {
                LoopL<Point3d> loopl = face.getLoopL();
                int loopIndex = 0;
                faceIndex++;

                /**
                 * First loop is the outer. Most skeleton faces will only have this.
                 * Second+ loops are the holes in the face (if you need this, you're
                 * a long way down a rabbit hole)
                 */
                for ( Loop<Point3d> loop : loopl )
                {
                    Polygon pg = new Polygon();
                    for ( Point3d p : loop )
                        pg.addPoint( ma.toX( p.x ), ma.toY( p.y ) );

                    if ( pg.npoints > 2 )
                    {
                        if ( loopIndex == 0 ) // outer loop
                            g2.setColor( new Color ( Color.HSBtoRGB( (faceIndex / (float) faceCount), 0.5f, 1f)) ); // new Color ( (33 * faceIndex) % 255,200,150)
                        else
                            g2.setColor( Color.white ); // hole..?

                        g2.fillPolygon( pg );

                        g2.setColor( Color.black );
                        g2.drawPolygon( pg );
                    }
                    loopIndex++;
                }
            }
        }
    }





}
