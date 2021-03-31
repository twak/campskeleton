package org.twak.camp.debug;

import javax.swing.GroupLayout;

import org.twak.camp.ui.PointEditor;

/**
 *
 * @author twak
 */
public class CampSkeleton extends javax.swing.JFrame {

    /**
     * -Comments-
     *
     * An intro to the algorithm
     * http://twak.blogspot.com/2009/05/engineering-weighted-straight-skeleton.html
     *
     * Uncomment other constructors below for editors.
     *
     * Important constants: (chosen for input of size 300-600 units)
     * 
     * EdgeCollision - line 150 - overlap when locating a collision
     * Height Collision - line 55 - criteria for co-sited events
     * CollisionQ - line 105 - criteria for co-heighted events
     *
     * Key classes are:
     *
     * PointEditor - start reading here - pls note the elgance with which the skeleton is executed on the awt thread ;)
     * Skeleton
     * CoSitedCollision
     *
     * Key data elements are:
     * 
     * Edge
     * Corner
     *
     * search for "todo" for areas of dubiousness.
     * note use of linkedHashHap/Set (that eat memory) for deterministic debugging...
     * this has never had a profiler run against it. it might be a good place to start for a speedup.
     *
     */

    /** Creates new form Main */
    public CampSkeleton() {
        initComponents();

        PointEditor pe;
        
//        pe = new MedialPointEditor();
        pe = new WeightedPointEditorWithHole();
//        pe = new SkeletonPointEditor();
//        pe = new PartialOffsetPointEditor();
//        pe = new OffsetPointEditor();
        setContentPane(pe);
        pe.setup();
    }

    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 686, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 502, Short.MAX_VALUE)
        );

        pack();
    }

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new CampSkeleton().setVisible(true);
            }
        });
    }
}
