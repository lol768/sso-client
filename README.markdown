SSO Client
=============

A Java library for reading from SSO in other apps.

Pushing a release onto Nexus
-------------

Examples are for command line in UNIX.

First, set the version number:

    $ mvn versions:set -DnewVersion=`date +%Y%m%d`
    $ mvn versions:commit
    
This should set <version>the-current-date</version> in all the pom.xml files.

You _can_ then just run the deploy goal from the command line, but it's better to use the deploy *profile* as
this may trigger other changes:

    $ mvn -Pdeploy
    
Multiple versions on the same day
-------------

If you get a message like this:

    [ERROR] Failed to execute goal [...] Failed to deploy artifacts: Could not transfer artifact [...] from/to nexus [...] Return code is: 400, ReasonPhrase:Bad Request
    
It probably means that the artifact already exists on Nexus, and Nexus doesn't support overwriting of artifacts
with the same version. The easiest way to fix this is to add an hour/minute component to your versioning:

    $ mvn versions:set -DnewVersion=`date +%Y%m%d-%H%M`
    $ mvn versions:commit

You should then be able to deploy.    
    
Generating distributables without pushing to Nexus
-------------

The dist profile will put JARs in a dist directory off the project root:

    $ mvn -Pdist