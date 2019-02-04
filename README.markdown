SSO Client
=============

A Java library for reading from SSO in other apps.

User documentation is available at [warwick.ac.uk/sso](https://warwick.ac.uk/sso). This README concerns only how to build and release the project.

In early 2019, SSO Client was changed to use the SBT build system. The instructions below have been updated. A wrapper is included so you don't need to install SBT, just have Java installed and use `./sbt`.

Versioning
-----------

Development should be done on a -SNAPSHOT version.

To get a new release ready:
  * Create a `release/*` branch
  * Bump its version to the next non-SNAPSHOT
  * PR and merge it to `master`
  * Deploy as below
  * Update `develop` to the next SNAPSHOT version.

Update the version by editing `build.sbt` and modifying the `version` property. This will
be inherited for all sub-projects - that is, you only need to change the version here.

Pushing a release onto Nexus
-------------

Go to [Bamboo](https://build.elab.warwick.ac.uk/browse/SSO-CLINT) and run the manual deploy task at the end of the build.

Using a snapshot version locally
-------------

If you are working on a `-SNAPSHOT` version and you want to try it in another app,
run `./sbt localSnapshot` to get it into your local `~/.m2` and `~/.ivy2/` cache. Then update your 
app's dependencies to use that version while testing.

This task will only work with snapshots because normal release versions are designed
to be immutable, and you'll find it won't update with your changes without great difficulty.

