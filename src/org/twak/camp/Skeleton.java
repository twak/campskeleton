
package org.twak.camp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;

import org.twak.camp.Output.Face;
import org.twak.camp.debug.DebugDevice;
import org.twak.utils.Cache;
import org.twak.utils.collections.CloneConfirmIterator;
import org.twak.utils.collections.DHash;
import org.twak.utils.collections.Loop;
import org.twak.utils.collections.LoopL;
import org.twak.utils.collections.Loopable;
import org.twak.utils.collections.Loopz;
import org.twak.utils.collections.ManyManyMap;
import org.twak.utils.collections.SetCorrespondence;
import org.twak.utils.geom.LinearForm3D;

/**
 * @author twak
 */
public class Skeleton {
	
    public boolean preserveParallel = false;
    public boolean volumeMaximising = true;
    public Set<Corner> liveCorners = new HashSet<>();  // order not essential in production
    public Set<Edge> liveEdges = new HashSet<>();
    public CollisionQ qu;
    public double height = 0;
    
    // we store triplets of faces already created to prevent duplicates (order–insensitive)
    public Set<EdgeCollision> seen = new HashSet<>();
    
    // output data
    public Output output = new Output(this);
    
    // debug
    public List<CoSitedCollision> debugCollisionOrder = new ArrayList<>();
    
    public Map<Edge, Set<Tag>> planFeatures = new HashMap<>();
    
    // for debugging
    public String name = "?";
    
    // lazy flag for re–finding all face events. true so we run it once at start
    boolean refindFaceEvents = true;
    
    // (temporaries used in capCopy)
    public DHash<Corner, Corner> cornerMap;
    public ManyManyMap<Corner, Corner> segmentMap;
    
    public Skeleton() {}
    
    public Skeleton(LoopL<Corner> corners) {
        setup(corners);
    }
    
    double cellSize = Double.MAX_VALUE;
    
    /**
     * Deprecated – given a loop of edges convert to corners.
     */
	public Skeleton(LoopL<Edge> input, double cellSize) {
		this.cellSize = cellSize;
		setupForEdges(input);
	}

	public Skeleton(LoopL<Edge> input, boolean javaGenericsAreABigPileOfShite) {
		setupForEdges(input);
	}

	public Skeleton(LoopL<Corner> input, double cap, boolean javaGenericsAreABigPileOfShite) {
		setup(input);
		capAt(cap);
    }
    
//    public Skeleton(LoopL<Edge> input, final double cap) {
//        setupForEdges(input);
//        capAt(cap);
//    }
    
    /**
     * Converts loops of edges (BAD!) to loops of corners (GOOD!)
     */
    public void setupForEdges(LoopL<Edge> input) {
        LoopL<Corner> corners = new LoopL<>();
        // Pre–allocate the inner loops where possible.
        for (Loop<Edge> le : input) {
            Loop<Corner> lc = new Loop<>();
            corners.add(lc);
            for (Edge e : le) {
                lc.append(e.start);
                e.start.nextL = e;
                e.end.prevL = e;
                e.start.nextC = e.end;
                e.end.prevC = e.start;
            }
        }
        setup(corners);
    }
    
    /**
     * Sanitize input
     */
    public void setup(LoopL<Corner> input) {
        height = 0;
        liveCorners.clear();
        liveEdges.clear();
        
        // Use a HashMap instead of a MultiMap if order isn’t essential (faster access)
        Map<Edge, List<Corner>> allEdges = new HashMap<>();
        
        for (Corner c : input.eIterator()) {
            List<Corner> list = allEdges.get(c.nextL);
            if (list == null) {
                list = new ArrayList<>();
                allEdges.put(c.nextL, list);
            }
            list.add(c);
        }
        
        // Combine shared edges (each edge appears once in output)
        for (Edge e : allEdges.keySet()) {
            e.currentCorners.clear();
            List<Corner> corners = allEdges.get(e);
            Corner first = corners.get(0);
            output.newEdge(first.nextL, null, new HashSet<>());
            
            // Merge all definitions
            for (int i = 1; i < corners.size(); i++) {
                output.merge(first, corners.get(i));
            }
            
            liveEdges.add(e);
        }
        
        for (Corner c : input.eIterator()) {
            if (c.z != 0 || c.nextL == null || c.prevL == null)
                throw new Error("Error in input");
            output.newDefiningSegment(c);
            liveCorners.add(c);
            c.nextL.currentCorners.add(c);
            c.prevL.currentCorners.add(c);
        }
        
        qu = new CollisionQ(this, cellSize);
        
        // Add edges to their machines (accelerated structure)
        for (Edge e : allEdges.keySet()) {
            e.machine.addEdge(e, this);
        }
        
        refindFaceEventsIfNeeded();
    }
    
    /**
     * Execute the skeleton algorithm.
     */
    public void skeleton() {
        validate();
        HeightEvent he;
        int i = 0;
        DebugDevice.dump("main " + String.format("%4d", ++i), this);
        while ((he = qu.poll()) != null) {
            try {
                if (he.process(this)) {
                    height = he.getHeight();
                    DebugDevice.dump("main " + height + " " + String.format("%4d", ++i), this);
                    validate();
                }
                refindFaceEventsIfNeeded();
            } catch (Throwable t) {
                t.printStackTrace();
                if (t.getCause() != null) {
                    System.out.println(" caused by:");
                    t.getCause().printStackTrace();
                }
            }
        }
        DebugDevice.dump("after main " + String.format("%4d", ++i), this);
        
        output.calculate(this);
    }
    
    /**
     * Returns a set of edges representing a horizontal slice through the skeleton at the specified height.
     * Non–destructive.
     */
    public LoopL<Corner> capCopy(double height) {
        segmentMap = new ManyManyMap<>();
        cornerMap = new DHash<>();
        
        LinearForm3D ceiling = new LinearForm3D(0, 0, 1, -height);
        
        // Use a standard for–each; minimal allocation per corner.
        for (Corner c : liveCorners) {
            try {
                Tuple3d t;
                if (height == c.z)
                    t = new Point3d(c);
                else {
                    if (preserveParallel && CollisionQ.isParallel(c.prevL, c.nextL)) {
                        Vector3d d = c.nextL.direction();
                        d.normalize(d);
                        LinearForm3D parallel = new LinearForm3D(d, c);
                        t = ceiling.collide(c.prevL.linearForm, parallel);
                    } else {
                        t = ceiling.collide(c.prevL.linearForm, c.nextL.linearForm);
                    }
                }
                cornerMap.put(new Corner(t), c);
            } catch (RuntimeException e) {
                cornerMap.put(new Corner(c.x, c.y, height), c);
            }
        }
        
        // Cache to re–use elevated edges
        Cache<Corner, Edge> edgeCache = new Cache<Corner, Edge>() {
            Map<Edge, Edge> lowToHighEdge = new HashMap<>();
            @Override
            public Edge create(Corner i) {
                // re–use edge if possible
                Edge edge = new Edge(cornerMap.teg(i), cornerMap.teg(i.nextC));
                lowToHighEdge.put(i.nextL, edge);
                edge.setAngle(i.nextL.getAngle());
                edge.machine = i.nextL.machine;
                return edge;
            }
        };
        
        LoopL<Corner> out = new LoopL<>();
        Set<Corner> workingSet = new HashSet<>(liveCorners);
        while (!workingSet.isEmpty()) {
            Loop<Corner> loop = new Loop<>();
            out.add(loop);
            Corner current = workingSet.iterator().next();
            do {
                Corner s = cornerMap.teg(current), e = cornerMap.teg(current.nextC);
                segmentMap.addForwards(current, s);
                Edge edge = edgeCache.get(current);
                loop.append(s);
                s.nextC = e;
                e.prevC = s;
                s.nextL = edge;
                e.prevL = edge;
                workingSet.remove(current);
                current = current.nextC;
            } while (workingSet.contains(current));
        }
        return out;
    }
    
    public Cache<Corner, Collection<Corner>> getSegmentOriginator() {
        return output.getSegmentOriginator();
    }
    
    public void parent(Face child, Face parent) {
        // override me if needed
    }
    
    public static class SEC {
        Corner start, end;
        Edge nextL, edge, prevL;
        public SEC(Corner start, Edge edge) {
            this.start = start;
            end = start.nextC;
            prevL = start.prevL;
            nextL = end.nextL;
            this.edge = edge;
        }
    }
    
    public interface HeresTheArea {
        public void heresTheArea(double area);
    }
    
    public void capAt(double cap) {
        capAt(cap, null);
    }
    
    public void capAt(double cap, HeresTheArea hta) {
        qu.add(new HeightEvent() {
            public double getHeight() {
                return cap;
            }
            public boolean process(Skeleton skel) {
                SkeletonCapUpdate capUpdate = new SkeletonCapUpdate(skel);
                LoopL<Corner> flatTop = capUpdate.getCap(cap);
                capUpdate.update(new LoopL<>(), new SetCorrespondence<Corner, Corner>(), new DHash<Corner, Corner>());
                LoopL<Point3d> togo = flatTop.new Map<Point3d>() {
                    @Override
                    public Point3d map(Loopable<Corner> input) {
                        return new Point3d(input.get().x, input.get().y, input.get().z);
                    }
                }.run();
                skel.output.addNonSkeletonOutputFace(togo, new Vector3d(0, 0, 1));
                if (hta != null)
                    hta.heresTheArea(Loopz.area3(togo));
                
                DebugDevice.dump("post cap dump", skel);
                skel.qu.clearFaceEvents();
                skel.qu.clearOtherEvents();
                return true;
            }
        });
    }
    
    public void refindAllFaceEventsLater() {
        refindFaceEvents = true;
    }
    
    private void refindFaceEventsIfNeeded() {
        if (!refindFaceEvents)
            return;
        
        HeightCollision context = new HeightCollision();
        for (Corner lc : new CloneConfirmIterator<>(liveCorners))
            qu.addCorner(lc, context, true);
        context.processHoriz(this);
        refindFaceEvents = false;
    }
    
    public void validate() {
        if (false) {
            // same debug routine as before…
        }
    }
    
    public void setPlanTags(Edge edge, Set<Tag> features) {
        planFeatures.put(edge, features);
    }
    
    public Set<Tag> getPlanTags(Edge originator) {
        return planFeatures.get(originator);
    }
    
    public Comparator<Edge> getHorizontalComparator() {
        return (o1, o2) -> {
            if (volumeMaximising)
                return Double.compare(o1.getAngle(), o2.getAngle());
            else
                return Double.compare(o2.getAngle(), o1.getAngle());
        };
    }
    
    public LoopL<Corner> findLoopLive() {
        LoopL<Corner> out = new LoopL<>();
        Set<Corner> togo = new HashSet<>(liveCorners);
        while (!togo.isEmpty()) {
            Loop<Corner> loop = new Loop<>();
            out.add(loop);
            Corner start = togo.iterator().next();
            Corner current = start;
            int handbrake = 0;
            do {
                togo.remove(current);
                loop.append(current);
                current = current.nextC;
                handbrake++;
            } while (current != start && handbrake < 1000);
            
            if (handbrake >= 1000) {
                System.err.println("broken loops in findLiveLoop");
                Thread.dumpStack();
            }
        }
        return out;
    }
}
       