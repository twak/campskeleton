package org.twak.camp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//A very simple spatial index for edges using a coarse grid. In production you might use a proper k-d tree.
public class EdgeSpatialIndex {
	private Map<Integer, List<Edge>> grid = new HashMap<>();
	private final double cellSize;

	public EdgeSpatialIndex(double cellSize) {
		this.cellSize = cellSize;
	}

	// Convert a point coordinate to grid cell id.
	private int hash(double coord) {
		return (int) Math.floor(coord / cellSize);
	}

	// We assume each edge has a bounding box (or can compute one) covering the
	// edge's current extent.
	// Here we assume edge.getBBox() returns a double[4] = {minX, minY, maxX, maxY}.
	public void insert(Edge e) {
		double[] bbox = e.getBBox();
		int xStart = hash(bbox[0]), xEnd = hash(bbox[2]);
		int yStart = hash(bbox[1]), yEnd = hash(bbox[3]);
		for (int i = xStart; i <= xEnd; i++) {
			for (int j = yStart; j <= yEnd; j++) {
				int key = (i << 16) + j;
				grid.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
			}
		}
	}

	// Remove an edge from the index. (You might want to improve this for
	// performance.)
// public void remove(Edge e) {
//     double[] bbox = e.getBBox(); 
//     int xStart = hash(bbox[0]), xEnd = hash(bbox[2]);
//     int yStart = hash(bbox[1]), yEnd = hash(bbox[3]);
//     for (int i = xStart; i <= xEnd; i++) {
//         for (int j = yStart; j <= yEnd; j++) {
//             int key = (i << 16) + j;
//             List<Edge> list = grid.get(key);
//             if (list != null) list.remove(e);
//         }
//     }
// }

	// Returns candidate edges that lie in a specified search box.
	public Set<Edge> search(double minX, double minY, double maxX, double maxY) {
		Set<Edge> results = new HashSet<>();
		int xStart = hash(minX), xEnd = hash(maxX);
		int yStart = hash(minY), yEnd = hash(maxY);
		for (int i = xStart; i <= xEnd; i++) {
			for (int j = yStart; j <= yEnd; j++) {
				int key = (i << 16) + j;
				List<Edge> list = grid.get(key);
				if (list != null)
					results.addAll(list);
			}
		}
		return results;
	}
}
