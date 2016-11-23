package straightskeleton.debug;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.vecmath.Point2d;
import straightskeleton.Corner;
import straightskeleton.Edge;
import straightskeleton.Machine;
import straightskeleton.OffsetSkeleton;
import straightskeleton.ui.Bar;
import straightskeleton.ui.PointEditor;
import org.twak.utils.Cache;
import org.twak.utils.ConsecutivePairs;
import org.twak.utils.LContext;
import org.twak.utils.Loop;
import org.twak.utils.LoopL;
import org.twak.utils.Pair;

/**
 *
 * @author twak
 */
public class PartialOffsetPointEditor extends PointEditor
{
    public PartialOffsetPointEditor()
    {
        super();
        MouseAdapter m = new EdgeClickListener();
        addMouseListener(m);
        addMouseMotionListener(m);
    }

    Map<Bar, Boolean> offset = new HashMap();

    @Override
    protected void createInitial() {
//        createCircularPoints( 5, 200, 200, 150);

        // cross shape:
        Loop<Bar> loop = new Loop();
        edges.add(loop);

        for (Pair<Point2d, Point2d> pair : new ConsecutivePairs<Point2d>(Arrays.asList(
                new Point2d(250, 100),
                new Point2d(350, 100),
                new Point2d(350, 250),
                new Point2d(500, 250),
                new Point2d(500, 350),
                new Point2d(350, 350),
                new Point2d(350, 500),
                new Point2d(250, 500),
                new Point2d(250, 350),
                new Point2d(100, 350),
                new Point2d(100, 250),
                new Point2d(250, 250)), true)) {
            Bar bar = new Bar(pair.first(), pair.second());
            offset.put(bar, Math.random() > 0.5 );
            loop.append(bar);
        }
    }


    public class EdgeClickListener extends MouseAdapter
    {
        @Override
        public void mouseClicked(MouseEvent e) {
                LContext<Bar> place = getNearest( ma.from(e), 10 );
                if (place == null)
                    return;
                offset.put (place.get(), !offset.get(place.get()));
                changed = true;
                repaint();
        }
    }

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
        offset.put ( ctx.loopable.getNext().get(), true );
    }


    boolean busy = false;
    LoopL<Corner> output;
    public void paintPointEditor(Graphics2D g2)
    {
        // override me!

        for (Loop<Bar> loop : edges)
        {
            for (Bar bar : loop)
            {
                g2.setColor( offset.get( bar ) ? Color.red : Color.black );
                g2.drawLine(
                        ma.toX( bar.start.x ),
                        ma.toY( bar.start.y ),
                        ma.toX( bar.end.x ),
                        ma.toY( bar.end.y ) );
            }
        }

        g2.setColor( Color.orange );
        for (Bar bar : edges.eIterator())
            drawPixel( g2, bar.start );

        final LoopL <Corner> out = new LoopL();

        Cache <Point2d, Corner> cCache = new Cache <Point2d, Corner> () {

            @Override
            public Corner create(Point2d i) {
                return new Corner (i.x, i.y);
            }
        };

        // controls the gradient of the edge
        final Machine machineO = new Machine (Math.PI/4), machineN = new Machine (Math.PI/4);

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

                s.nextL.machine = offset.get (bar) ? machineO : machineN;

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
                        OffsetSkeleton<Machine> os = new OffsetSkeleton<Machine> (out, 50);
                        os.registerProfile(machineN, 0, 1);
                        os.registerProfile(machineO, Math.PI/4, 1);

                        OffsetSkeleton.Offset out = (OffsetSkeleton.Offset) os.getResults().get(0);

                        PartialOffsetPointEditor.this.output = out.shape;
                    } finally {
                        busy = false;
                        PartialOffsetPointEditor.this.repaint(); // erk
                    }
                };
            }.start();
        }

        /**
         * End of skeleton code...well we just have to paint it:
         */
        if ( output != null )
        {
            for (Loop<Corner> lc : output)
            {
                Polygon pg = new Polygon();
                for (Corner p : lc)
                    pg.addPoint( ma.toX( p.x ), ma.toY( p.y ) );
                g2.setColor(Color.blue);
                g2.drawPolygon(pg);
            }
        }
    }
}
