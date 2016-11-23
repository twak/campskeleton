/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package straightskeleton.debug;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.vecmath.Point2d;
import straightskeleton.ui.Bar;
import straightskeleton.ui.PointEditor;
import org.twak.utils.Intersector;
import org.twak.utils.Intersector.Collision;
import org.twak.utils.LContext;
import org.twak.utils.Line;
import org.twak.utils.Loop;

/**
 *
 * @author twak
 */
public class IntersectorDebugger extends PointEditor
{

    public IntersectorDebugger() {
        this.barSelected = new BarSelected() {

            @Override
            public void barSelected(LContext<Bar> ctx) {
                edges.remove(ctx.loop);
                repaint();
            }
        };
    }



    @Override
    public void paintPointEditor(Graphics2D g2) {
                g2.setColor(Color.black);

                List<Line> lines = new ArrayList();

        for (Loop<Bar> loop : edges) {
            for (Bar bar : loop) {
                lines.add(new Line (bar.start, bar.end));
                g2.drawLine(
                        ma.toX(bar.start.x),
                        ma.toY(bar.start.y),
                        ma.toX(bar.end.x),
                        ma.toY(bar.end.y));
            }
        }

        g2.setColor(Color.red);

        Intersector insec = new Intersector();
        for (Collision c : insec.intersectLines(lines))
        {
            drawPixel(g2, c.location);
        }

    }


    @Override
    protected void createInitial() {
        Line[] lines = new Line[]{
            new Line(new Point2d(-53.24221344555228, 0.0), new Point2d(0.0, 0.0)),
            new Line(new Point2d(0.0, 0.0), new Point2d(-5.889175337144792, 35.925099917433926)),
            new Line(new Point2d(-5.889175337144792, 35.925099917433926), new Point2d(-63.703401386350826, 30.032545418712743)),
            new Line(new Point2d(-63.703401386350826, 30.032545418712743), new Point2d(-53.24221344555228, 0.0)),
            new Line(new Point2d(-64.70340138635083, -2.0), new Point2d(-64.70340138635083, 37.925099917433926)),
            new Line(new Point2d(-62.1763474868758, -2.0), new Point2d(-62.1763474868758, 37.925099917433926)),
            new Line(new Point2d(-59.64929358740077, -2.0), new Point2d(-59.64929358740077, 37.925099917433926)),
            new Line(new Point2d(-57.122239687925735, -2.0), new Point2d(-57.122239687925735, 37.925099917433926)),
            new Line(new Point2d(-54.595185788450706, -2.0), new Point2d(-54.595185788450706, 37.925099917433926)),
            new Line(new Point2d(-52.06813188897567, -2.0), new Point2d(-52.06813188897567, 37.925099917433926)),
            new Line(new Point2d(-49.541077989500636, -2.0), new Point2d(-49.541077989500636, 37.925099917433926)),
            new Line(new Point2d(-47.01402409002561, -2.0), new Point2d(-47.01402409002561, 37.925099917433926)),
            new Line(new Point2d(-44.48697019055058, -2.0), new Point2d(-44.48697019055058, 37.925099917433926)),
            new Line(new Point2d(-41.959916291075544, -2.0), new Point2d(-41.959916291075544, 37.925099917433926)),
            new Line(new Point2d(-39.43286239160051, -2.0), new Point2d(-39.43286239160051, 37.925099917433926)),
            new Line(new Point2d(-36.90580849212548, -2.0), new Point2d(-36.90580849212548, 37.925099917433926)),
            new Line(new Point2d(-34.378754592650445, -2.0), new Point2d(-34.378754592650445, 37.925099917433926)),
            new Line(new Point2d(-31.851700693175417, -2.0), new Point2d(-31.851700693175417, 37.925099917433926)),
            new Line(new Point2d(-29.32464679370038, -2.0), new Point2d(-29.32464679370038, 37.925099917433926)),
            new Line(new Point2d(-26.797592894225353, -2.0), new Point2d(-26.797592894225353, 37.925099917433926)),
            new Line(new Point2d(-24.270538994750318, -2.0), new Point2d(-24.270538994750318, 37.925099917433926)),
            new Line(new Point2d(-21.743485095275283, -2.0), new Point2d(-21.743485095275283, 37.925099917433926)),
            new Line(new Point2d(-19.216431195800254, -2.0), new Point2d(-19.216431195800254, 37.925099917433926)),
            new Line(new Point2d(-16.68937729632522, -2.0), new Point2d(-16.68937729632522, 37.925099917433926)),
            new Line(new Point2d(-14.16232339685019, -2.0), new Point2d(-14.16232339685019, 37.925099917433926)),
            new Line(new Point2d(-11.635269497375155, -2.0), new Point2d(-11.635269497375155, 37.925099917433926)),
            new Line(new Point2d(-9.108215597900127, -2.0), new Point2d(-9.108215597900127, 37.925099917433926)),
            new Line(new Point2d(-6.581161698425092, -2.0), new Point2d(-6.581161698425092, 37.925099917433926)),
            new Line(new Point2d(-4.0541077989500565, -2.0), new Point2d(-4.0541077989500565, 37.925099917433926)),
            new Line(new Point2d(-1.5270538994750282, -2.0), new Point2d(-1.5270538994750282, 37.925099917433926)),
            new Line(new Point2d(1.0, -2.0), new Point2d(1.0, 37.925099917433926)),
            new Line(new Point2d(-65.70340138635083, -1.0), new Point2d(2.0, -1.0)),
            new Line(new Point2d(-65.70340138635083, 2.7925099917433926), new Point2d(2.0, 2.7925099917433926)),
            new Line(new Point2d(-65.70340138635083, 6.585019983486785), new Point2d(2.0, 6.585019983486785)),
            new Line(new Point2d(-65.70340138635083, 10.377529975230178), new Point2d(2.0, 10.377529975230178)),
            new Line(new Point2d(-65.70340138635083, 14.17003996697357), new Point2d(2.0, 14.17003996697357)),
            new Line(new Point2d(-65.70340138635083, 17.962549958716963), new Point2d(2.0, 17.962549958716963)),
            new Line(new Point2d(-65.70340138635083, 21.755059950460357), new Point2d(2.0, 21.755059950460357)),
            new Line(new Point2d(-65.70340138635083, 25.547569942203747), new Point2d(2.0, 25.547569942203747)),
            new Line(new Point2d(-65.70340138635083, 29.34007993394714), new Point2d(2.0, 29.34007993394714)),
            new Line(new Point2d(-65.70340138635083, 33.13258992569053), new Point2d(2.0, 33.13258992569053) )};


        for (Line l :lines)
        {
            Loop<Bar> loop = new Loop();
            edges.add(loop);
            loop.append(new Bar (l.start, l.end));
        }
    }


    public static void main (String[] args)
    {
        JFrame frame = new JFrame("argh");
        IntersectorDebugger tc = new IntersectorDebugger();
        tc.setup();
        frame.setContentPane(tc);
        frame.setSize(800, 800);
        frame.setVisible(true);
    }

}
