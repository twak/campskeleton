# Src to skeleton a polygon without the UI #

Define Points counter clockwise.
Output is in 3d, discard the last coordinate (z) to project back to the 2d input space.
This example is for the unweighted skeleton. To change this you will want to create a new Machine for every Edge, and use the constructor Machine (angle)

```
        Corner 
        	c1 = new Corner ( 0,0), 
        	c2 = new Corner (100,-100 ), 
        	c3 = new Corner (100,0 );
        
        Machine directionMachine = new Machine ();
        
        loop1.append(new Edge ( c1,c2 ) );
        loop1.append(new Edge ( c2, c3 ) );
        loop1.append(new Edge ( c3, c1 ) );
        
        for (Edge e : loop1)
        	e.machine = directionMachine;
        
        
        Skeleton skel = new Skeleton (out, true);
        skel.skeleton();
        
        for ( Face face : skel.output.faces.values() )
        {
            System.out.println("face:");
            for (Loop  lp3 : face.points)
            	for (Point3d pt : lp3)
            		System.out.println(pt);
        }
```