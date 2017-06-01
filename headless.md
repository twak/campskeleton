# Headless example #

[src](https://github.com/twak/campskeleton/blob/master/src/org/twak/camp/debug/Example.java). Define Points counter clockwise.
Output is in 3d, discard the last coordinate (z) to project back to the 2d input space.

```
       Corner c1 = new Corner( 0, 0 ), 
	      c2 = new Corner( 100, -100 ), 
              c3 = new Corner( 100, 0 );

		Machine speed1 = new Machine(Math.PI/4),
				speed2 = new Machine(Math.PI/3);

		Loop<Edge> loop1 = new Loop<Edge>();

		
		Edge e1 = new Edge( c1, c2 ),
			 e2 = new Edge( c2, c3 ),
			 e3 = new Edge( c3, c1 );
		
		loop1.append( e1 );
		loop1.append( e2 );
		loop1.append( e3 );

		e1.machine = speed1;
		e2.machine = speed1;
		e3.machine = speed2;

		Skeleton skel = new Skeleton( loop1.singleton(), true );
		skel.skeleton();

		for ( Face face : skel.output.faces.values() ) {
			System.out.println( "face:" );
			for ( Loop<Point3d> lp3 : face.points )
				for ( Point3d pt : lp3 )
					System.out.println( pt );
		}
```
