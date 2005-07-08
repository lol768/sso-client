1) Include sso-client.jar in your deploy directory and your project classpath in Eclipse
2) Add the following snippets to your web.xml:

	<context-param>
		<param-name>ssoclient.config</param-name>
		<param-value>/myapp-sso-config.xml</param-value>
	</context-param>
	
	<filter>
    	<filter-name>SSOClientFilter</filter-name>
        <filter-class>uk.ac.warwick.sso.client.SSOClientFilter</filter-class>
    </filter>	
	
	<!-- map this filter over everything that will be protected by SSO -->
	<filter-mapping>
        <filter-name>SSOClientFilter</filter-name>
        <url-pattern>/*.spr</url-pattern>
    </filter-mapping>
		
	<listener>
    	<listener-class>uk.ac.warwick.sso.client.SSOConfigLoader</listener-class>
	</listener>
	
	<servlet>
    	<servlet-name>ShireServlet</servlet-name>
	    <display-name>Shire</display-name>
    	<servlet-class>uk.ac.warwick.sso.client.ShireServlet</servlet-class>
	</servlet>

	<servlet>
    	<servlet-name>LogoutServlet</servlet-name>
	    <display-name>Shire</display-name>
    	<servlet-class>uk.ac.warwick.sso.client.LogoutServlet</servlet-class>
	</servlet>
	
	<servlet-mapping>
    	<servlet-name>ShireServlet</servlet-name>
	    <url-pattern>/shire</url-pattern>
	</servlet-mapping>
	
	<servlet-mapping>
    	<servlet-name>LogoutServlet</servlet-name>
	    <url-pattern>/logout</url-pattern>
	</servlet-mapping>
	
	
3) The "/myapp-sso-config.xml" should be a file in your JBoss conf directory, but renamed 
to be more specific to your web app, eg. "/blogs-sso-config.xml"

4) Copy and rename the example "sso-config.xml" file from the sso-client dist directory to your JBoss conf directory

5) Edit this file to get all of the right settings for your service

6) Alter your code where you get the logged in user to simply get the User object out of the request from the SSO_USER key

7) Alter your login links to look something like this:

<c:choose>
	<c:when test="${SSO_USER != null && SSO_USER.loggedIn == true}">
		<p><a href="<sso:logoutlink target="" />">Sign out</a></p>
	</c:when>
	<c:otherwise>
		<p><a href="<sso:loginlink target="" />">Sign in</a></p>
	</c:otherwise>
</c:choose>

8) Generating a CSR for your app:

# keytool -keystore <your_domain_name>.keystore -alias <your_domain_name> -genkey -keyalg RSA
# keytool -certreq -alias <your_domain_name> -keystore <your_domain_name>.keystore -file <your_domain_name>.csr
# java -classpath . ExportPriv <your_domain_name>.keystore <your_domain_name> <keystore_password>

9) Give the <your_domain_name>.csr file to the SSO administrator, they will generate a certificate for you.

10) When you get the certificate <your_domain_name>.crt.der, import it back into the keystore, along with the Root CRT if needs be:

# keytool -import -keystore <your_domain_name>.keystore -alias testsso-ca -file testsso-ca.crt.der
# keytool -import -keystore <your_domain_name>.keystore -alias <your_domain_name> -file <your_domain_name>.crt.der 

11) Configure Apache/Jboss to run SSL with the right certificates...hard.