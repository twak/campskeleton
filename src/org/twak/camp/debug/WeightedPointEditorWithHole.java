package org.twak.camp.debug;

import org.twak.camp.ui.Bar;
import org.twak.utils.Pair;
import org.twak.utils.collections.ConsecutivePairs;
import org.twak.utils.collections.Loop;

import javax.vecmath.Point2d;
import java.util.Arrays;

public class WeightedPointEditorWithHole extends WeightedPointEditor {

	@Override
	protected void createInitial()
	{
		//        giggidyStar( 0 );

		// cross shape:
		Loop<Bar> loop = new Loop();
		edges.add( loop );

		for ( Pair<Point2d, Point2d> pair : new ConsecutivePairs<Point2d>( Arrays.asList(
				new Point2d( 250, 100 ),
				new Point2d( 350, 100 ),
				new Point2d( 350, 250 ),
				new Point2d( 500, 250 ),
				new Point2d( 500, 350 ),
				new Point2d( 350, 350 ),
				new Point2d( 350, 500 ),
				new Point2d( 250, 500 ),
				new Point2d( 250, 350 ),
				new Point2d( 100, 350 ),
				new Point2d( 100, 250 ),
				new Point2d( 250, 250 ) ), true ) )
		{
			Bar b = new Bar( pair.first(), pair.second() );
			loop.append( b );
			setup(b, 0.7);
		}

		Loop<Bar> loop2 = new Loop();
		edges.add( loop2 );

		for ( Pair<Point2d, Point2d> pair : new ConsecutivePairs<Point2d>( Arrays.asList(
				new Point2d( 330, 330),
				new Point2d( 330, 270 ),
				new Point2d( 270, 270 ),
				new Point2d( 270, 330 )
		), true ) )
		{
			Bar b = new Bar( pair.first(), pair.second() );
			loop2.append( b );
			setup(b, 0.7);
		}
	}
}
