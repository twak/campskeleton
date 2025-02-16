package org.twak.camp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.tinspin.index.rtree.RTree;
import org.tinspin.index.rtree.RTreeEntry;

public class EdgeSpatialIndex {

	private final RTree<Edge> tree;
	private final int size;

	public EdgeSpatialIndex(Collection<Edge> edges) {
		tree = RTree.createRStar(2);
		size = edges.size();
		RTreeEntry<Edge>[] boxes = new RTreeEntry[size];
		int i = 0;

		for (Edge e : edges) {
			double[] bb = e.getBBox();
			RTreeEntry<Edge> box = RTreeEntry.createBox(new double[] { bb[0], bb[1] }, new double[] { bb[2], bb[3] }, e);
			boxes[i++] = box;
		}

		tree.load(boxes);
	}

	/**
	 * Searches for edges that intersect with the specified bounding box.
	 */
	public List<Edge> search(double minX, double minY, double maxX, double maxY) {
		double[] min = new double[] { minX, minY };
		double[] max = new double[] { maxX, maxY };
		List<Edge> out = new ArrayList<>();
		tree.queryIntersect(min, max).forEachRemaining(e -> out.add(e.value()));
		return out;
	}

	/**
	 * Searches for the k nearest edges to the specified point.
	 */
	public List<Edge> search(double cX, double cY, int k) {
		double[] center = new double[] { cX, cY };
		List<Edge> out = new ArrayList<>();

		if (k >= size) {
			tree.iterator().forEachRemaining(e -> out.add(e.value()));
		} else {
			tree.queryKnn(center, k).forEachRemaining(e -> out.add(e.value()));
		}
		return out;
	}
}
