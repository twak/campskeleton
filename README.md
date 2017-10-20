weighted straight skeleton implementation in java.

allows negative weights for offsetting in either direction. implementation of [Felkel's](http://www.dma.fi.upm.es/mabellanas/tfcs/skeleton/html/documentacion/Straight%20Skeletons%20Implementation.pdf) algo with robustness - described [here](http://twak.blogspot.com/2009/05/engineering-weighted-straight-skeleton.html).

[![](http://farm5.static.flickr.com/4006/4709590538_76e5c9ce6f.jpg)](http://www.flickr.com/photos/twak/4709590538/)

run the [jar](https://github.com/twak/campskeleton/blob/master/campskeleton-0.0.1-SNAPSHOT-jar-with-dependencies.jar?raw=true) with

```
java -jar siteplan-0.0.1-SNAPSHOT-jar-with-dependencies.jar
```

there's a primative gui interface. [video](http://www.youtube.com/watch?v=2twcln3_7Y8). use left mouse button to move points and control-click to add new points.

to build, requires [my jutils](https://github.com/twak/jutils) to be installed (with 'mvn compile install'). then the command 'mvn package' should build a jar.

main UI class is [org.twak.camp.debug.CampSkeleton](https://github.com/twak/campskeleton/blob/master/src/org/twak/camp/debug/CampSkeleton.java). 

[example headless code](https://github.com/twak/campskeleton/blob/wiki/headless.md)

This is a [research project](http://twak.blogspot.com/2011/04/interactive-architectural-modeling-with.html) - if you use it, please cite us:

<pre>
@article{kelly2011interactive,
  title={Interactive architectural modeling with procedural extrusions},
  author={Kelly, Tom and Wonka, Peter},
  journal={ACM Transactions on Graphics (TOG)},
  volume={30},
  number={2},
  pages={14},
  year={2011},
  publisher={ACM}
}
</pre>
