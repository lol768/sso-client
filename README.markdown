SSO Client
=============

A Java library for reading from SSO in other apps.

Pushing a release onto Nexus
-------------

Just go to Bamboo and run the manual deploy task at the end of the build.
    
Generating distributables without pushing to Nexus
-------------

The dist profile will put JARs in a dist directory off the project root:

    $ mvn -Pdist