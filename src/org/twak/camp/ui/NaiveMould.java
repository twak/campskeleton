/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.twak.camp.ui;

import java.util.ArrayList;
import java.util.List;
import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;
import org.twak.utils.Line;

/**
 *
 * @author twak
 */
public class NaiveMould extends Mould {

    List<PMarker> generators = new ArrayList();

    public List<Marker> getAnchorsForEditing(Bar b, Point2d relStart, Point2d relEnd, Point2d... additional) 
    {
        Point2d absStart = relStart, absEnd = relEnd;
        // for now we assume this means that the points are the abs. end points (originating edge points)
        if (additional.length >=2)
        {
            absStart = additional[0];
            absEnd   = additional[1];
        }

        List<Marker> out = new ArrayList();

        add (out, relStart, relEnd, b, true);
        add (out, absStart, absEnd, b, false);

        return out;
    }


    private void add(List<Marker> out, Point2d relStart, Point2d relEnd, Bar b, boolean rel)
    {
        Vector2d delta = new Vector2d(relEnd);
        delta.sub(relStart);


        for (PMarker pm : generators)
        {
            if (pm.rel == rel)
            {
                Vector2d d2 = new Vector2d(delta);
                d2.scale(pm.param);
                d2.add(relStart);
                Marker o = new Marker();
                o.bar = b;
                o.set(d2);


                if (b != null)
                {
                    o.bar = b;
                }

                out.add(o);
                o.generator = pm;
            }
        }
    }

    public Object remove( Marker m )
    {
        generators.remove( (PMarker) m.generator);
        return m.generator;
    }

    public void create( Marker m, Object generator ) {
        if (generator == null)
        {
            generator = new PMarker(m);
        }
        else
            ((PMarker)generator).set(m);
        generators.add((PMarker) generator);
    }

    public static class PMarker {
        // memo to self: don't hash param! - system ident should be used for indexing
        double param = 0.5;
        public boolean rel = true;

        public PMarker(double param) {
            this.param = 0.5;
        }

        public PMarker(Marker m) {
            set(m);
        }

        public void set(Marker m) {
            Line line = new Line(m.bar.start, m.bar.end);
            param = line.findPPram(m); //line.distance( m )
            m.set(line.project(m, true));
            m.generator = this;
        }

        public void setOffLine(Marker m) {
            Line line = new Line(m.bar.start, m.bar.end);
            param = line.findPPram(m);
            m.generator = this;
        }
    }

    public void clear()
    {
        generators.clear();
    }
}
