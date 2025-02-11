/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.twak.camp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;

import org.twak.camp.debug.DebugDevice;
import org.twak.utils.geom.LinearForm3D;

/**
 *
 * @author twak
 */
public class CollisionQ {
    // Using PriorityQueue with an initial capacity based on liveCorners can help reduce re–allocations.
	   private PriorityQueue<EdgeCollision> faceEvents;
	    private PriorityQueue<HeightEvent> miscEvents;
	    Skeleton skel;
	    private Set<EdgeCollision> seen = new HashSet<>();

	    // The spatial index for live edges:
	    private EdgeSpatialIndex edgeIndex;
	    
	    final double gridCell;

	    public CollisionQ(Skeleton skel) {
	    	this(skel, 1000);
	    }
	    
	    public CollisionQ(Skeleton skel, double cellSize) {
	    	gridCell = cellSize;
	        this.skel = skel;
	        int initSize = Math.max(3, skel.liveCorners.size());
	        faceEvents = new PriorityQueue<>(initSize, HeightEvent.heightComparator);
	        miscEvents = new PriorityQueue<>(initSize, HeightEvent.heightComparator);
	        // Choose a reasonable cell size based on your model’s scale.
	        edgeIndex = new EdgeSpatialIndex(cellSize/2);
	        
	        // Insert all live edges into the index once.
	        for (Edge e : skel.liveEdges) {
	            edgeIndex.insert(e);
	        }
	    }
    
    /**
     * Returns the next event, giving priority to (virtual) simultaneous collisions.
     */
    private HeightEvent nextEvent() {
        EdgeCollision ec;
        while (true) {
            ec = faceEvents.poll();
            if (ec == null) break;
            // Only process events not previously “seen” and at a height above (or just above) current
            if (!skel.seen.contains(ec) && ec.loc.z >= skel.height - 0.001)
                break;
        }
        
        HeightEvent he = miscEvents.peek();
        if (ec == null)
            return miscEvents.poll();
        if (he == null) {
            skel.seen.add(ec);
            return ec;
        }
        if (he.getHeight() <= ec.getHeight()) {
            faceEvents.add(ec);
            return miscEvents.poll();
        } else {
            skel.seen.add(ec);
            return ec;
        }
    }
    
    HeightCollision currentCoHeighted = null;
    
    public HeightEvent poll() {
        currentCoHeighted = null;
        HeightEvent next = nextEvent();
        if (next instanceof EdgeCollision) {
            List<EdgeCollision> coHeighted = new ArrayList<>();
            EdgeCollision ec = (EdgeCollision) next;
            coHeighted.add(ec);
            double height = ec.getHeight();
            
            // Gather all face events whose height is within a narrow tolerance.
            while (true) {
                EdgeCollision higher = faceEvents.peek();
                if (higher == null)
                    break;
                if (Math.abs(higher.getHeight() - height) < 0.00001) {
                    faceEvents.poll();
                    if (skel.seen.contains(higher))
                        continue;
                    height = higher.getHeight();
                    skel.seen.add(higher);
                    coHeighted.add(higher);
                } else break;
            }
            currentCoHeighted = new HeightCollision(coHeighted);
            return currentCoHeighted;
        } else {
            return next;
        }
    }
    
    public void add(HeightEvent he) {
        if (he instanceof EdgeCollision)
            faceEvents.add((EdgeCollision) he);
        else
            miscEvents.add(he);
    }
    
    /**
     * Add collisions for a new corner.
     * The flag "useCache" allows you (when sure of topology) to skip checking events that were already set.
     */
    public void addCorner(Corner toAdd, HeightCollision postProcess) {
        addCorner(toAdd, postProcess, false);
    }
    
    public void addCorner(Corner toAdd, HeightCollision postProcess, boolean useCache) {
        if (!skel.preserveParallel && toAdd.prevL.sameDirectedLine(toAdd.nextL)) {
            removeCorner(toAdd);
            return;
        }
        // Loop–of–two dissolve rule
        if (toAdd.prevL == toAdd.nextC.nextL) {
            skel.output.addOutputSideTo(toAdd, toAdd.nextC, toAdd.prevL, toAdd.nextL);
            toAdd.nextL.currentCorners.remove(toAdd);
            toAdd.nextL.currentCorners.remove(toAdd.nextC);
            toAdd.prevL.currentCorners.remove(toAdd);
            toAdd.prevL.currentCorners.remove(toAdd.nextC);
            if (toAdd.nextL.currentCorners.isEmpty())
                skel.liveEdges.remove(toAdd.nextL);
            if (toAdd.prevL.currentCorners.isEmpty())
                skel.liveEdges.remove(toAdd.prevL);
            skel.liveCorners.remove(toAdd);
            skel.liveCorners.remove(toAdd.nextC);
            return;
        }
        
        if (!skel.preserveParallel && toAdd.prevL.isCollisionNearHoriz(toAdd.nextL)) {
            if (toAdd.nextL.direction().angle(toAdd.prevL.direction()) < 0.01)
                postProcess.newHoriz(toAdd);
            return;
        }

        // Instead of iterating over all liveEdges, query only those in the relevant search region.
        // Determine a search box based on the corner’s approximate location and spread.
        double searchRadius = gridCell; // This value may need tuning.
        double minX = toAdd.x - searchRadius, minY = toAdd.y - searchRadius;
        double maxX = toAdd.x + searchRadius, maxY = toAdd.y + searchRadius;

        Set<Edge> candidateEdges = edgeIndex.search(minX, minY, maxX, maxY);
        for (Edge e : candidateEdges) {
            EdgeCollision ex = new EdgeCollision(null, toAdd.prevL, toAdd.nextL, e);
            if (!useCache || !seen.contains(ex)) {
                seen.add(ex);
                cornerEdgeCollision(toAdd, e);
            }
        }
    }
    
    private static final double COS_THRESHOLD = Math.cos(0.0001);

    public static boolean isParallel(Edge a, Edge b) {
        return isAligned(a.uphill, b.uphill) && isAligned(a.direction(), b.direction());
    }

    private static boolean isAligned(Vector3d v1, Vector3d v2) {
    	
        // Avoid division by zero
        double lenSq1 = v1.lengthSquared();
        double lenSq2 = v2.lengthSquared();
        if (lenSq1 == 0.0 || lenSq2 == 0.0) {
            return false;
        }

        // Compare squared quantities to avoid square root
        double dotProduct = v1.dot(v2);
        double squaredDot = dotProduct * dotProduct;
        double threshold = COS_THRESHOLD * COS_THRESHOLD * lenSq1 * lenSq2;

        return squaredDot >= threshold;
    }
    
    public static boolean isParallel2(Edge a, Edge b) {
        return angleBetween(a.uphill, b.uphill) < 0.0001 &&
               angleBetween(a.direction(), b.direction()) < 0.0001;
    }
    
    private static double angleBetween(Vector3d v1, Vector3d v2) {
        double vDot = v1.dot(v2);
        double lenSq1 = v1.lengthSquared();
        double lenSq2 = v2.lengthSquared();
        vDot = Math.max(-1.0, Math.min(1.0, vDot / Math.sqrt(lenSq1 * lenSq2)));
        return Math.acos(vDot);
    }
    
    private void cornerEdgeCollision(Corner corner, Edge edge) {
        if (skel.preserveParallel) {
            if (isParallel(edge, corner.prevL) && isParallel(edge, corner.nextL))
                return;
            if (corner.nextL == edge || corner.prevL == edge)
                return;
        } else {
            if (isParallel(edge, corner.prevL) || isParallel(edge, corner.nextL))
                return;
        }
        Tuple3d res = null;
        try {
            if (corner.prevL.linearForm.hasNaN() || corner.nextL.linearForm.hasNaN() || edge.linearForm.hasNaN())
                throw new Error();
            if (skel.preserveParallel && isParallel(corner.nextL, corner.prevL)) {
                LinearForm3D fake = new LinearForm3D(corner.nextL.direction(), corner);
                res = edge.linearForm.collide(fake, corner.prevL.linearForm);
            } else {
                res = edge.linearForm.collide(corner.prevL.linearForm, corner.nextL.linearForm);
            }
        } catch (Throwable f) {
            // [Fallback collision computations...]
        }
        if (res != null) {
            if (res.z < corner.z || res.z < edge.start.z)
                return;
            EdgeCollision ec = new EdgeCollision(new Point3d(res), corner.prevL, corner.nextL, edge);
            if (!skel.seen.contains(ec))
                faceEvents.offer(ec);
        }
    }
    
    boolean holdRemoves = false;
    List<Corner> removes = new ArrayList<>();
    
    public void holdRemoves() {
        removes.clear();
        holdRemoves = true;
    }
    
    public void resumeRemoves() {
        holdRemoves = false;
        for (Corner c : removes)
            if (skel.liveCorners.contains(c))
                removeCorner(c);
        removes.clear();
    }
    
    private void removeCorner(Corner toAdd) {
        if (holdRemoves) {
            removes.add(toAdd);
            return;
        }
        DebugDevice.dump("about to delete " + toAdd, skel);
        toAdd.prevC.nextC = toAdd.nextC;
        toAdd.nextC.prevC = toAdd.prevC;
        toAdd.nextC.prevL = toAdd.prevL;
        skel.liveCorners.remove(toAdd);
        for (Corner lc : skel.liveCorners) {
            if (lc.nextL == toAdd.nextL)
                lc.nextL = toAdd.prevL;
            if (lc.prevL == toAdd.nextL)
                lc.prevL = toAdd.prevL;
        }
        if (toAdd.prevL != toAdd.nextL) {
            skel.liveEdges.remove(toAdd.nextL);
            for (Corner c : toAdd.nextL.currentCorners)
                toAdd.prevL.currentCorners.add(c);
            skel.output.merge(toAdd.prevC, toAdd);
            skel.refindAllFaceEventsLater();
        }
        toAdd.prevL.currentCorners.remove(toAdd);
    }
    
    public void dump() {
        int i = 0;
        for (EdgeCollision ec : faceEvents)
            System.out.println(String.format("%d : %s ", i++, ec));
    }
    
    public void clearFaceEvents() { faceEvents.clear(); }
    public void clearOtherEvents() { miscEvents.clear(); }
}

