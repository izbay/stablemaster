stablemaster
============

Bringing mount management to your server!

To Import to Eclipse, Choose import existing maven project from import project in Eclipse. Point to the main directory of the contents on this repo. 

To compile, make sure you are using Maven. Then just download the contents of this repo and run

mvn clean package

from a command line.

You will need to supply your own copy of CraftBukkit 1.7.9+, place it into the lib directory, and edit the pom.xml to point to your jar.
