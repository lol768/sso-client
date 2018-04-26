SSO Client
=============

A Java library for reading from SSO in other apps.

Versioning
-------------

Development should be done on a -SNAPSHOT version. When ready to release
a new version, commit a change to the non-SNAPSHOT version, deploy that as below,
then commit a change to the next SNAPSHOT version.

Update Maven POMs:

    $ mvn versions:set && mvn versions:commit

You shouldn't need to update `modules/play/build.sbt` as it will read the version
from the base POM.

Pushing a release onto Nexus
-------------

Go to [Bamboo](https://build.elab.warwick.ac.uk/browse/SSO-CLINT) and run the manual deploy task at the end of the build.

Using a snapshot version locally
-------------

If you are working on a `-SNAPSHOT` version and you want to try it in another app,
run `mvn install` to get it into your local `~/.m2` cache and then update your 
app's dependencies to use that version.

It's important to only do this with snapshots because normal release versions are designed
to be immutable, and you'll find it won't update with your changes without great difficulty.
    
Generating distributables without pushing to Nexus
-------------

You'll rarely need to do this except possibly for old deployments that don't use managed dependencies.

The dist profile will put JARs in a dist directory off the project root:

    $ mvn -Pdist --projects modules/servlet
