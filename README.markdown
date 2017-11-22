SSO Client
=============

A Java library for reading from SSO in other apps.

Versioning
-------------

Most development should be done on a -SNAPSHOT version. When ready to release
a new version, commit a change to the non-SNAPSHOT version, deploy that as below,
then commit a change to the next SNAPSHOT version.

Update Maven POMs:

    $ mvn versions:set && mvn versions:commit

You shouldn't need to update `modules/play/build.sbt` as it will read the version
from the base POM.

Pushing a release onto Nexus
-------------

Just go to Bamboo and run the manual deploy task at the end of the build.
    
Generating distributables without pushing to Nexus
-------------

The dist profile will put JARs in a dist directory off the project root:

    $ mvn -Pdist --projects modules/servlet
