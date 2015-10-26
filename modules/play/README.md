# SSO Client for Play Framework

## How to use

Add the dependency to your `build.sbt`:

    resolvers += "nexus" at "https://mvn.elab.warwick.ac.uk/nexus/content/groups/public"
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
    
    libraryDependencies += "uk.ac.warwick.sso" %% "sso-client-play" % CurrentVersion
    
    // https://bugs.elab.warwick.ac.uk/browse/SSO-1653
    dependencyOverrides += "xml-apis" % "xml-apis" % "1.4.01"
    
Add the controllers to your `routes`:

    ->         /sso                 sso.Routes
    
Add config to your private `application.conf`, e.g:

    sso-client {
      mode = new
      cluster.enabled = true 
      shire {
        sscookie {
          name = "SSC-Appname"
          domain = "example.warwick.ac.uk"
          path = "/"
        }
        location = "https://example.warwick.ac.uk/sso/acs"
        providerid = "urn:example.warwick.ac.uk:servicename:service"
      }
      logout.location = "https://example.warwick.ac.uk/sso/logout"
      credentials {
        certificate = "file:///var/warwick/ssl/example.warwick.ac.uk.crt"
        key = "file:///var/warwick/ssl/example.warwick.ac.uk.key"
        chain = "file:///var/warwick/ssl/intermediates/terena256.pem"
      }
    }
    
Add the Guice module to your default conf, e.g.:

    play.modules.enabled += warwick.sso.SSOClientModule
    
You'll need to create a database table called `objectcache`. If you are using evolutions, the evolution
script will look like this:

    # --- !Ups
    CREATE TABLE objectcache (
        "KEY" nvarchar2 (100) NOT NULL,
        "OBJECTDATA" BLOB,
        "CREATEDDATE" TIMESTAMP (6),
        CONSTRAINT "OBJECTCACHE_PK" PRIMARY KEY ("KEY")
    );
     
     
    # --- !Downs
    DROP TABLE objectcache;%
    
Then use Guice injection to get access to one of the provided beans. `SsoClient` provides some `Action`
builders that you can use in your controllers to get the current `User`. `UserLookupService` is just a
Scala-friendly interface to UserLookup, letting you find users by usercode and University ID.

