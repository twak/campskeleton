package straightskeleton.debug;

import straightskeleton.Corner;
import straightskeleton.Edge;
import straightskeleton.Machine;
import straightskeleton.OffsetSkeleton;
import utils.Loop;
import utils.LoopL;

public class Main2 {

	public static void main(String[] args) {
		Loop<Edge> loop1 = new Loop<Edge>();
		Corner c1 = new Corner ( 0,0), 
				c2 = new Corner (100,0 ), 
				c3 = new Corner (100,100 );
		
		Machine directionMachine = new Machine ();
		
		loop1.append(new Edge ( c1,c2 ) );
		loop1.append(new Edge ( c2, c3 ) );
		loop1.append(new Edge ( c3, c1 ) );
		for (Edge e : loop1) e.machine = directionMachine;

		LoopL<Edge> a = new LoopL<Edge>(loop1);
		LoopL<Corner> output = OffsetSkeleton.shrink(a, 5);
	}

}
