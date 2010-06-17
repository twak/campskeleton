package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;

/**
 * .obj face output
 * @author twak
 */
public class ObjDump {
/**
 * Really simple anchor that aggregates the points and then outputs the results
 * to a abject file
 *
 * @author twak
 *
 */
	public String name;

	public Set<List<Integer>> tris;

	public Map<Tuple3d, Integer> vertexToNo;

	public List<Tuple3d> order;

	public ObjDump()
	{
		// reset hashes
		tris = new HashSet<List<Integer>>();
		vertexToNo = new LinkedHashMap<Tuple3d, Integer>();
		order = new ArrayList<Tuple3d>();
		// ask for file name, bootstraping for sity frame
	}

    public void allDone( File output )
	{
		try
		{
                    if (output.getParentFile() != null)
                        output.getParentFile().mkdirs();
			BufferedWriter out = new BufferedWriter(new FileWriter(output));
			for (Tuple3d v: order)
				out.write("v "+v.x+" "+v.y+" "+v.z+"\n");
            
			for (List<Integer> t : tris)
            {
                out.write("f ");
                for (Integer i : t)
                    out.write(i+" ");
                out.write("\n");
            }
			out.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

    /**
     * Extension hook
     */
    public Tuple3d convertVertex(Tuple3d pt)
    {
        return pt;
    }

	public void addFace(List<Point3d> lv)
	{
        List<Integer> face = new ArrayList();

        int count = 0;
        for ( Tuple3d uv : lv )
        {
            Tuple3d v = convertVertex( uv );

            if ( vertexToNo.containsKey( v ) )
                face.add( vertexToNo.get( v ) );
            else
            {
                int number = order.size() + 1; // size will be next index
                face.add( number );
                order.add( v );
                vertexToNo.put( v, number );
            }
            count++;
        }
        tris.add( face );
	}

    public void addAll( LoopL<Point3d> faces )
    {
        for (Loop<Point3d> loop : faces)
        {
            List<Point3d> face = new ArrayList();
            
            for (Point3d p : loop)
                face.add( p );

            addFace( face );
        }
    }

    public void addAll( List<List<Point3d>> faces )
    {
        for (List<Point3d> face : faces)
            addFace( face );
    }

}
