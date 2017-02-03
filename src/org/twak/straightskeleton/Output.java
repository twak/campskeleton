package org.twak.straightskeleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;

import org.twak.utils.AngleAccumulator;
import org.twak.utils.Cache;
import org.twak.utils.ConsecutiveTriples;
import org.twak.utils.GraphMap;
import org.twak.utils.IdentityLookup;
import org.twak.utils.Loop;
import org.twak.utils.LoopL;
import org.twak.utils.Loopable;
import org.twak.utils.Triple;

/**
 * @author twak
 */
public class Output
{
    // marker for horizontal input edges (other edges can also be horizontal...)
    public static Tag isCreatedHorizontal = new Tag ("horizontal");

    public Map<Corner, Face> faces = new LinkedHashMap<>(); // the faces represented by each edge
    public List<LoopNormal> nonSkelFaces = new ArrayList<>();
    public List<LoopNormal> nonSkelFaces2 = new ArrayList<>();
    public IdentityLookup<SharedEdge> edges = new IdentityLookup<>(); // edge ensure that each output edge only exists once

    public Skeleton skeleton;

    public Output(Skeleton skel)
    {
        this.skeleton = skel;
    }

    /**
     * One edge may start in two locations at the same time. To accomodate this, you call
     * newEdge once per new edge, then new Defining Segment for each corner that references that
     * edge.
     * 
     * @param startCorner The corner at the start of the edge at the base of this edge
     * @param aParentCorner A corner whose
     * @param profileFeatures
     */

    public void newEdge (Edge e, Corner aParentLeadingCorner, Set<Tag> profileFeatures )
    {
        Face face = new Face();

        face.edge = e;
        if ( profileFeatures != null )
            face.profile = profileFeatures;

        if ( aParentLeadingCorner != null ) // an originator - an edge in the plan
        {
            Face parentFace = faces.get( aParentLeadingCorner );
            assert(parentFace != null);
            face.parent = parentFace;
            skeleton.parent( face, parentFace );
        }

        // we assume that start locations for edges are unique!
        assert faces.get( e.start ) == null;

        e.start.nextL = e; // these are always true?! - we rely on them for indexing, below
        e.end.prevL = e;
        faces.put( e.start, face );
    }

    /**
     * see newEdge.
     */
    public void newDefiningSegment ( Corner leadingCorner ) {
        Face face = faces.get ( leadingCorner.nextL.start );

        SharedEdge se= createEdge( leadingCorner, leadingCorner.nextC );
        face.definingSE.add( se );
        se.setLeft( leadingCorner, face );
        face.results.add( se.start, se.end );
        se.features.add( isCreatedHorizontal );

        face.definingCorners.add( leadingCorner );
    }

    public void addOutputSideTo( Tuple3d a, Tuple3d b, Edge... edges )
    {
        addOutputSideTo( false, a, b, edges );
    }
    public void addOutputSideTo( boolean isTop, Tuple3d a, Tuple3d b, Edge... edges ) // a.y == b.yequals(b)
    {
        for ( Edge edge : edges )
        {
            Corner c = edge.start; // assumption: start of edge will always be a leading corner on the face!
            Face f = faces.get( c );
            assert (f != null);
            if (isTop)
                f.topSE.add( createEdge( a, b ) );
            // just check those tuple's aren't corners....
            f.results.add( new Point3d( a ), new Point3d( b ) );
//            System.out.println(">>");
        }
    }

    /**
     * Some faces (such as base-plates for globals) can't really be classified nicely.
     * They live here.
     * 
     * @param geom
     */


    public void addNonSkeletonOutputFace( LoopL<? extends Point3d> points, Vector3d norm )
    {
        nonSkelFaces.add (new LoopNormal(points, norm));
    }
    public void addNonSkeletonOutputFace2( LoopL<Point3d> points, Vector3d norm )
    {
        nonSkelFaces2.add (new LoopNormal(points, norm));
    }

    public void setParent( Corner neu, Corner old )
    {
        Face nF = faces.get( neu );
        Face oF = faces.get( old );
        skeleton.parent (nF, oF);
        nF.parent = oF;
    }

    public static class LoopNormal
    {
        public LoopL<? extends Point3d> loopl;
        public Vector3d norm;
        public LoopNormal (LoopL<? extends Point3d> loopl, Vector3d norm)
        {
            this.loopl = loopl;
            this.norm = norm;
        }
    }

//    public void addNonSkeletonOutputFace ( LoopL<? extends Point3d> geom )
//    {
//        Face f = new Face();
//
//        Loopable<? extends Point3d> eg = geom.get( 0 ).start;
//
//        // assume normal is straight down for now;
//        f.edge = new Edge(eg.get(), eg.getNext().get(), Math.PI);
//
//        SharedEdge se = createEdge( f.edge.start, f.edge.end);
//        f.definingSE.add( se );
//
//        se.setLeft( f.edge.start, f );
//
//        nonSkelFaces.add( f );
//
//        for (Loop<? extends Point3d> loop : geom)
//        {
//            for (Loopable<? extends Point3d> loopable : loop.loopableIterator())
//                f.results.add( loopable.get(), loopable.getNext().get() );
//        }
//    }

    /**
     * Constructs the faces from using the results graph and
     * the points involved with each edge.
     */
    public void calculate( Skeleton skel )
    {
        // todo: reinstante the plan tags somehow
//        for (Face f : faces.values())
//            f.plan = skel.getPlanTags( getOriginator( f ) );

        // collect identical edges in different polygons
        edge:
        for ( Face face :faces.values() ) //e.toString()
        {
//            System.out.println( face.results );
            Set<Point3d> notVisited = new LinkedHashSet<>( face.results.map.keySet() );
            LoopL<Point3d> faceWithHoles = new LoopL<>(); // first entry here is outer boundary
            face.points = faceWithHoles;

            try
            {
            Point3d edgeStart = face.definingSE.iterator().next().getStart( face );

//            System.out.println ("results "+face.results); //face.points.count()

            while ( !notVisited.isEmpty() )
            {
                // associated face input polygon
                Loop<Point3d> poly = new Loop<>();
//                faceWithHoles.add( poly );
                boolean isOuter = notVisited.contains( edgeStart );

                Point3d // first loop should be the outline ie: should satrt at the defining edges
                        start = isOuter ? edgeStart : notVisited.iterator().next(),
                        pos = start,
                        last = face.results.get(start).get(0); // arb. direction

                Point3d first = null, lastAdded = null;

                AngleAccumulator ac = new AngleAccumulator( isOuter, face.edge.getPlaneNormal() );

                int count = 0;

                pointsInLoop:
                do
                {
                    List<Point3d> choice = face.results.get( pos );
                    assert ( choice != null );

                    for ( Point3d c : choice )
                    {
                        if ( count++ > 1000 ) // handbrake turn!
                            continue edge;

                        if ( !last.equals( c ) && !pos.equals( c ) )
                        {
                            if (first == null)
                                first = c;
                            
                            notVisited.remove( c );
                            
                            // remove short edges between the previous corners, and between the current corner and the startstart (bad hack)
                            if ( (lastAdded == null || lastAdded.distance( c ) > 0.01) && (first == c || first.distance( c ) > 0.01))
                            {
                                poly.append( c );
                                ac.add( c );
                                lastAdded = c;
                            }

                            last = pos;
                            pos = c;
                            continue pointsInLoop;
                        }
                    }
                    
                    System.out.println( "didn't find faces on " + face.definingSE );
//                    new GraphMapDebug( face.results );
                    System.out.println( face.results );
                    continue edge; 
                }
                while (pos != start);

                // inner loops go counter clockwise
                if (!ac.correctAngle())
                    poly.reverse();

                removeStraights (poly);

                // as we remove degenerately small polygons *
                if (poly.count() >= 3)
                    faceWithHoles.add( poly );
            }
                        }
            catch (Throwable e)
            {
//                e.printStackTrace();
                continue;
            }

        }

        // * so we remove faces without polygons
        List<Face> nullFaces = new ArrayList<>();
        for (Face f : faces.values())
        {
            if (f.points.size() <= 0)
                nullFaces.add( f );
        }
        
//        assert (nullFaces.size() == 0); // do something sensible!

        for (Face f : faces.values())
            f.findSharedEdges();
    }

    /**
     * Two parallel faces have become consecutive, remove info about toGo,
     * add to toKeep
     * @toKeep leading corner to keep
     * @toGo leading corner to go
     */
    void merge( Corner toKeep, Corner toGo )
    {
        Face 
                toGoFace = faces.get( toGo.nextL.start ),
                toKeepFace = faces.get( toKeep.nextL.start );


        if ( toGoFace == null )
        {
            System.err.println ("three consecutive parallel edges in input?");
//            Thread.dumpStack();
            return;
        }

        toKeepFace.definingSE.addAll( toGoFace.definingSE );
        toKeepFace.results.addEntriesFrom( toGoFace.results );

        toKeepFace.definingCorners.addAll( toGoFace.definingCorners );

        // forward any further face requests to the new one
//        faces.put (toGo.nextL.start, toKeepFace);
        faces.put (toGo, toKeepFace );


    }

    private SharedEdge createEdge ( Tuple3d start, Tuple3d end)
    {
        SharedEdge newEdge = new SharedEdge (new Point3d (start), new Point3d (end ));
        newEdge = edges.get( newEdge ); // identity lookup - only one edge!

        return newEdge;
    }


    public Cache <Corner, Collection<Corner>>  getSegmentOriginator()
    {
        return new Cache<Corner, Collection<Corner>>()
        {
            @Override
            public List<Corner> get( Corner aCorner )
            {
                Face f = faces.get( aCorner.nextL.start );
                while (f.parent != null)
                {
                    f = f.parent;
                }

                return new ArrayList<> ( f.definingCorners );
            }

            @Override
            public List<Corner> create( Corner i )
            {
                throw new UnsupportedOperationException( "Have overridden get(), shouldn't end up here!" );
            }
        };
    }

    public Face getGreatestGrandParent( Face f )
    {
        while (f.parent != null)
            f = f.parent;
        return f;
    }

    private void removeStraights( Loop<Point3d> poly )
    {
        // this will be filtered out later. some of this assumes >= 3 edges.
        if (poly.count() < 3)
            return;

        Set<Loopable<Point3d>> togo = new HashSet<>();
        for ( Triple<Loopable<Point3d>, Loopable<Point3d>, Loopable<Point3d>> trip :
                new ConsecutiveTriples<Loopable<Point3d>>( poly.loopableIterator(), true ) )
        {
            Loopable<Point3d> a = trip.first(),
                    b = trip.second(),
                    c = trip.third();

            Vector3d ab = new Vector3d( b.get() ), bc = new Vector3d( c.get() );
            ab.sub( a.get() );
            bc.sub( b.get() );

            double angle = ab.angle( bc );
            double small = 0.001;
            if ( angle < small || angle > Math.PI - small )
                togo.add( b );
        }
        for ( Loopable<Point3d> lpb : togo )
            poly.remove( lpb );
    }

    public class Face
    {
        // first is outside, others are holes
        public LoopL<Point3d> points = null;

        public Set<Tag> plan = new HashSet<>(), profile = new HashSet<>();

        // bottom edges
        public Set<SharedEdge> definingSE = new LinkedHashSet<>();
        // defining edges of child (top) edges
        public Set<SharedEdge> topSE = new LinkedHashSet<>();

        // face below us in the skeleton - can be traced back to an originator
        public Face parent;
        
        public GraphMap<Point3d> results = new GraphMap<>();

        // a typical edge that defines the plane normal
        public Edge edge;

        // subset of results who are horizontal edges and whose nextL are edge, or similar.
        public Set<Corner> definingCorners = new LinkedHashSet<>();

        public LoopL<SharedEdge> edges = new LoopL<>();

        public LoopL<Point3d> getLoopL()
        {
            return points;
        }

        public int pointCount()
        {
            return points.count();
        }
        // is a defining edges of the above (child) edges
        public boolean isTop (SharedEdge edge)
        {
            return topSE.contains( edge );
        }
        // is a defining edge
        public boolean isBottom(SharedEdge edge)
        {
            return definingSE.contains(  edge );
        }
        // isn't a top or bottom edges
        public boolean isSide (SharedEdge edge)
        {
            return !( isTop( edge ) || isBottom( edge ) );
        }

        /**
         * When caculating an offset, we can assume that all edges add a face at every interval
         * this returns the number of faces below this face.
         * @return
         */
        public int getParentCount()
        {
            int count = -1;
            Face f = this;
            while (f != null)
            {
                count++;
                f = f.parent;
            }
            return count;
        }

        private void findSharedEdges()
        {
            edges = new LoopL<>();
            for (Loop <Point3d> ptLoop : points)
            {
                Loop<SharedEdge> loop = new Loop<>();
                edges.add (loop);
                // start and end points of SharedEdges **aren't** shared
                for (Loopable<Point3d> loopable : ptLoop.loopableIterator())
                {
                    SharedEdge e = createEdge( loopable.get(), loopable.getNext().get() );
                    e.setLeft (loopable.get(), this );

                    loop.append( e );
                }
            }
        }
    }

    public static class SharedEdge
    {
        Point3d start, end;
        public Face left, right;
        Set <Tag> features = new HashSet<>();

        private SharedEdge( Point3d start, Point3d end )
        {
            this.start = start;
            this.end = end;
        }

        public Point3d getStart( Face ref )
        {
            if (ref == left)
                return end;
            else if (ref == right)
                return start;
            throw new Error();
        }

        public Point3d getEnd (Face ref)
        {
            if (ref == left)
                return start;
            else if (ref == right)
                return end;
            throw new Error();
        }

        /**
         * Symetric wrt start, end!
         */
        @Override
        public boolean equals( Object obj )
        {
            if (obj instanceof SharedEdge)
            {
                SharedEdge o = (SharedEdge) obj;
                if (o.start.equals( start ))
                    return o.end.equals( end );
                else if (o.end.equals( start ))
                    return o.start.equals( end );
            }
            return false;
        }

        /**
         * Symetric wrt start, end!
         */
        @Override
        public int hashCode()
        {
            int hash = 7;
            hash += 71 * ( this.start != null ? this.start.hashCode() : 0 );
            hash += 71 * ( this.end != null ? this.end.hashCode() : 0 );
            return hash;
        }

        public Face getOther( Face ref )
        {
            if ( ref == left )
                return right;
            else if ( ref == right )
                return left;
            throw new Error();
        }

        private void setLeft( Point3d start, Face left )
        {
            if (this.start.equals( start) )
                this.left = left;
            else if (this.end.equals( start) )
                this.right = left;
            else
                throw new Error();
        }

        @Override
        public String toString()
        {
            return "{" + start + " to " + end + "}";
        }
    }

    public Output dupeEdgesOnly()
    {
        Output out = new Output( null );


        Cache<Face, Face> fCache = new Cache<Face, Face>()
        {
            @Override
            public Face create( Face old ) {

                Face face = new Face();

                GraphMap<Point3d> outGM = new GraphMap<>();
                outGM.addEntriesFrom( old.results );
                
                face.results = outGM;
                face.parent = old.parent == null ? null : get( old.parent );
                face.edge = new Edge( old.edge.start, old.edge.end );

                face.definingSE = new HashSet<> ();

                for (SharedEdge se : old.definingSE)
                {
                    SharedEdge neu = new SharedEdge( se.start, se.end);
                    neu.setLeft( se.start, face);
                    face.definingSE.add( neu );
                }

                return face;
            }
        };


        for (Corner c : faces.keySet())
        {
            out.faces.put(c, fCache.get( faces.get( c)));
        }

//        for (Face f : nonSkelFaces)
//            out.nonSkelFaces.add( fCache.get( f ) );

        return out;
    }

       /**
     * Calling this announces the creation of a new edge. We store it's defining edge,
     * but do not (can not) calculate it's vertices at this time.
     *
     * @param newEdge the edge we're adding
     * @param parent null if we're an originator, else it's the edge that defines the face
     * below the one we're creating.
     */
//    public void addFace ( Edge newEdge, Edge parent, Set<Feature> profileFeatures )
//    {
//    }
//        Face face = new Face();
//
//        SharedEdge se= getEdge( face, newEdge.start, newEdge.end );
//        face.defining.add( se );
//        se.setLeft( newEdge.start, face );
//        face.results.add( se.start, se.end );
//        se.features.add( isInput );
//
//        face.edge = newEdge;
//        if (profileFeatures != null)
//            face.profile = profileFeatures;
//
//        if ( parent != null ) // an originator - an edge in the plan
//        {
//            Face parentFace = faces.get( parent );
//            assert(parentFace != null);
//            face.parent = parentFace;
//        }
//        else
//            originators.put( newEdge, face );
//
//        faces.put( newEdge, face );
//    }

//    public Edge getOriginator (Face f)
//    {
//        Face parent = f;
//        while (! originators.containsB( parent ))
//            parent = f.parent;
//
//        return originators.teg( f );
//    }
}
