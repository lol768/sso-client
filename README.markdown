SSO Client
=============

A Java library for reading from SSO in other apps.

In early 2019, SSO Client was changed to use the SBT build system. The instructions below have been updated

Versioning
-----------

Development should be done on a -SNAPSHOT version. When ready to release
a new version, commit a change to the non-SNAPSHOT version, deploy that as below,
then commit a change to the next SNAPSHOT version.

Update the version by editing `build.sbt` and modifying the `libraryVersion`. This will
be inherited for all sub-projects - that is, you only need to change the version here.

You shouldn't need to update `modules/play/build.sbt`.

Pushing a release onto Nexus
-------------

Go to [Bamboo](https://build.elab.warwick.ac.uk/browse/SSO-CLINT) and run the manual deploy task at the end of the build.

Using a snapshot version locally
-------------

If you are working on a `-SNAPSHOT` version and you want to try it in another app,
run `./sbt publishM2 publishLocal` to get it into your local `~/.m2` and `~/.ivy2/` cache and then update your 
app's dependencies to use that version.

It's important to only do this with snapshots because normal release versions are designed
to be immutable, and you'll find it won't update with your changes without great difficulty.

##### Snapshot sso-client-play

The above step should be sufficient, because all sub-projects will be installed to the local Maven repository
and Ivy cache with the provided command.
