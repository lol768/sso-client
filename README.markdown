SSO Client
=============

A Java library for reading from SSO in other apps.

Versioning
-------------

Most development should be done on a -SNAPSHOT version. When ready to release
a new version, commit a change to the non-SNAPSHOT version, deploy that as below,
then commit a change to the next SNAPSHOT version.

Pushing a release onto Nexus
-------------

Just go to Bamboo and run the manual deploy task at the end of the build.
    
Generating distributables without pushing to Nexus
-------------

The dist profile will put JARs in a dist directory off the project root:

    $ mvn -Pdist
