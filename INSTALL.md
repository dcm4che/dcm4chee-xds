Getting Started with DCM4CHEE XDS 2.0.0
==================================================

Requirements
------------
-   Java SE 6 or later - tested with [OpenJDK](http://openjdk.java.net/)
    and [Oracle JDK](http://java.com/en/download)

-   [JBoss Application Server 7.1.1.Final](http://www.jboss.org/jbossas/downloads)

-   Supported SQL Database:
    - [MySQL 5.5](http://dev.mysql.com/downloads/mysql)
    - [PostgreSQL 9.2.1](http://www.postgresql.org/download/)
    - [Firebird 2.5.1](http://www.firebirdsql.org/en/firebird-2-5-1/)
    - [DB2 10.1](http://www-01.ibm.com/software/data/db2/express/download.html)
    - [Oracle 11g](http://www.oracle.com/technetwork/products/express-edition/downloads/)
    - [Microsoft SQL Server](http://www.microsoft.com/en-us/download/details.aspx?id=29062)
      (not yet tested!)

-   LDAP Server - tested with
    - [OpenDJ 2.4.6](http://www.forgerock.org/opendj.html),
    - [OpenLDAP 2.4.33](http://www.openldap.org/software/download/) and
    - [Apache DS 2.0.0-M8](http://directory.apache.org/apacheds/2.0/downloads.html).

    *Note*: DCM4CHEE XDS 2.x also supports using Java Preferences as configuration
    backend. But because DCM4CHEE XDS does not yet contain a
    configuration front-end, you would have to edit configuration entries in the
    Java Preferences back-end manually, which is quit cumbersome and therefore not
    further described here.

-   LDAP Browser - [Apache Directory Studio 1.5.3](http://directory.apache.org/studio/)

    *Note*: Because DCM4CHEE XDS 2.0.0 does not yet contain a specific
    configuration front-end, the LDAP Browser is needed to modify the XDS
    configuration.


Download and extract binary distribution package
------------------------------------------------
DCM4CHEE XDS 2.x binary distributions for different databases can be obtained
from [Sourceforge](https://sourceforge.net/projects/dcm4che/files/dcm4chee-xds2/).
Extract (unzip) your chosen download to the directory of your choice.


Initialize Database
-------------------

### MySQL

1. Enable remote access by commenting out `skip-networking` in configuration file `my.conf`.

2. Create database and grant access to user

        > mysql -u root -p<root-password>
        mysql> CREATE DATABASE <database-name>;
        mysql> GRANT ALL ON <database-name>.* TO '<user-name>' IDENTIFIED BY '<user-password>';
        mysql> quit

3. Create tables and indexes
       
        > mysql -u <user-name> -p<user-password> < $DCM4CHEE_XDS2/sql/create-table-mysql.ddl
        > mysql -u <user-name> -p<user-password> < $DCM4CHEE_XDS2/sql/create-index.ddl


### PostgreSQL

1. Create user with permission to create databases 

        > createuser -U postgres -P -d <user-name>
        Enter password for new role: <user-password> 
        Enter it again: <user-password> 

2. Create database

        > createdb -U <user-name> <database-name>

3. Create tables and indexes
       
        > psql -U <user-name> < $DCM4CHEE_XDS2/sql/create-table-psql.ddl
        > psql -U <user-name> < $DCM4CHEE_XDS2/sql/create-index.ddl


### Firebird

1. Define database name in configuration file `aliases.conf`:

        <database-name> = <database-file-path>

2. Create user

        > gsec -user sysdba -password masterkey \
          -add <user-name> -pw <user-password>

3. Create database, tables and indexes

        > isql 
        Use CONNECT or CREATE DATABASE to specify a database
        SQL> CREATE DATABASE 'localhost:<database-name>'
        CON> user '<user-name>' password '<user-password>';
        SQL> IN $DCM4CHEE_XDS2/sql/create-table-firebird.ddl;
        SQL> IN $DCM4CHEE_XDS2/sql/create-index.ddl;
        SQL> EXIT;

        
### DB2

1. Create database and grant authority to create tables to user
   (must match existing OS user)

        > sudo su db2inst1
        > db2
        db2 => CREATE DATABASE <database-name> PAGESIZE 16 K
        db2 => connect to <database-name>
        db2 => GRANT CREATETAB ON DATABASE TO USER <user-name>
        db2 => terminate
 
2. Create tables and indexes

        > su <user-name>
        Password: <user-password>
        > db2 connect to <database-name>
        > db2 -t < $DCM4CHEE_XDS2/sql/create-table-db2.ddl
        > db2 -t < $DCM4CHEE_XDS2/sql/create-index.ddl
        > db2 terminate
        

### Oracle 11g 

TODO


Setup LDAP Server
-----------------

### OpenDJ

1.  Copy LDAP schema files for OpenDJ from DCM4CHEE XDS 2.x distribution to
    OpenDJ schema configuration directory:

        > cp $DCM4CHEE_XDS2/ldap/opendj/* $OPENDJ_HOME/config/schema/ [UNIX]
        > copy %DCM4CHEE_XDS2%\ldap\opendj\* %OPENDJ_HOME%\config\schema\ [Windows]

2.  Run OpenDJ GUI based setup utility

        > $OPENDJ_HOME/setup
    
    Log the values choosen for
    -  LDAP Listener port (1389)
    -  Root User DN (cn=Directory Manager)
    -  Root User Password (secret)
    -  Directory Base DN (dc=example,dc=com)

    needed for the LDAP connection configuration of DCM4CHEE XDS 2.x.

4. After initial setup, you may start and stop OpenDJ by

        > $OPENDJ_HOME/bin/start-ds
        > $OPENDJ_HOME/bin/stopt-ds


### OpenLDAP

OpenLDAP binary distributions are available for most Linux distributions and
for [Windows](http://www.userbooster.de/en/download/openldap-for-windows.aspx).

OpenLDAP can be alternatively configured by

- [slapd.conf configuration file](http://www.openldap.org/doc/admin24/slapdconfig.html)
- [dynamic runtime configuration](http://www.openldap.org/doc/admin24/slapdconf2.html)

See also [Converting old style slapd.conf file to cn=config format][1]

[1]: http://www.openldap.org/doc/admin24/slapdconf2.html#Converting%20old%20style%20{{slapd.conf}}%285%29%20file%20to%20{{cn=config}}%20format

#### OpenLDAP with slapd.conf configuration file

1.  Copy LDAP schema files for OpenLDAP from DCM4CHEE XDS 2.x distribution to
    OpenLDAP schema configuration directory:

        > cp $DCM4CHEE_XDS2/ldap/schema/* /etc/openldap/schema/ [UNIX]
        > copy %DCM4CHEE_XDS2%\ldap\schema\* \Program Files\OpenLDAP\schema\ [Windows]

2.  Add references to schema files in `slapd.conf`, e.g.:

        include         /etc/openldap/schema/core.schema
        include         /etc/openldap/schema/dicom.schema
        include         /etc/openldap/schema/dcm4che.schema
        include         /etc/openldap/schema/dcm4che-hl7.schema
        include         /etc/openldap/schema/dcm4chee-xds2.schema

3.  You may also change the default values for 

        suffix          "dc=my-domain,dc=com"
        rootdn          "cn=Manager,dc=my-domain,dc=com"
        rootpw          secret
   
    in `slapd.conf`.


#### OpenLDAP with dynamic runtime configuration

1.  Import LDAP schema files for OpenLDAP runtime configuration, binding as
    root user of the config backend, using OpenLDAP CL utility ldapadd, e.g.:

        > ldapadd -xW -Dcn=config -f $DCM4CHEE_XDS2/ldap/slapd/dicom.ldif
        > ldapadd -xW -Dcn=config -f $DCM4CHEE_XDS2/ldap/slapd/dcm4che.ldif
        > ldapadd -xW -Dcn=config -f $DCM4CHEE_XDS2/ldap/slapd/dcm4che-hl7.ldif
        > ldapadd -xW -Dcn=config -f $DCM4CHEE_XDS2/ldap/slapd/dcm4chee-xds2.ldif

    If you don't know the root user and its password of the config backend, you may
    look into `/etc/openldap/slap.d/cn=config/olcDatabase={0}config.ldif`:

        olcRootDN: cn=config
        olcRootPW:: VmVyeVNlY3JldA==

    and decode the base64 decoded password, e.g:

        > echo -n VmVyeVNlY3JldA== | base64 -d
        VerySecret

    or specify a new password in plan text, e.g:

        olcRootPW: VerySecret

2.  Directory Base DN and Root User DN can be modified by changing the values of
    attributes

        olcSuffix: dc=my-domain,dc=com
        olcRootDN: cn=Manager,dc=my-domain,dc=com

    of object `olcDatabase={1}bdb,cn=config` by specifing the new values in a 
    LDIF file (e.g. `modify-baseDN.ldif`)

        dn: olcDatabase={1}bdb,cn=config
        changetype: modify
        replace: olcSuffix
        olcSuffix: dc=example,dc=com
        -
        replace: olcRootDN
        olcRootDN: cn=Manager,dc=example,dc=com
        -

    and applying it using OpenLDAP CL utility ldapmodify, e.g.:

        > ldapmodify -xW -Dcn=config -f modify-baseDN.ldif


### Apache DS 2.0

1.  Install [Apache DS 2.0.0-M8](http://directory.apache.org/apacheds/2.0/downloads.html)
    on your system and start Apache DS.

2.  Install [Apache Directory Studio 1.5.3](http://directory.apache.org/studio/) and
    create a new LDAP Connection with:

        Network Parameter:
            Hostname: localhost
            Port:     10398
        Authentication Parameter:
            Bind DN or user: uid=admin,ou=system
            Bind password:   secret

3.  Import LDAP schema files for Apache DS:

        $DCM4CHEE_XDS2/ldap/apacheds/dicom.ldif
        $DCM4CHEE_XDS2/ldap/apacheds/dcm4che.ldif
        $DCM4CHEE_XDS2/ldap/apacheds/dcm4che-hl7.ldif
        $DCM4CHEE_XDS2/ldap/apacheds/dcm4chee-xds2.ldif

    using the LDIF import function of Apache Directory Studio LDAP Browser.

4.  You may modify the default Directory Base DN `dc=example,dc=com` by changing
    the value of attribute 

        ads-partitionsuffix: dc=example,dc=com`

    of object

        ou=config
        + ads-directoryServiceId=default
          + ou=partitions
              ads-partitionId=example
    
    using Apache Directory Studio LDAP Browser.


Import sample configuration into LDAP Server
--------------------------------------------  

1.  If not alread done, install
    [Apache Directory Studio 1.5.3](http://directory.apache.org/studio/) and create
    a new LDAP Connection corresponding to your LDAP Server configuration, e.g:

        Network Parameter:
            Hostname: localhost
            Port:     1398
        Authentication Parameter:
            Bind DN or user: cn=Directory Manager
            Bind password:   secret
        Browser Options:
            Base DN: dc=example,dc=com

2.  If you configured a different Directory Base DN than`dc=example,dc=com`,
    you have to replace all occurrences of `dc=example,dc=com` in LDIF files
 
        $DCM4CHEE_XDS2/ldap/init-baseDN.ldif
        $DCM4CHEE_XDS2/ldap/init-config.ldif
        $DCM4CHEE_XDS2/ldap/sample-config.ldif

    by your Directory Base DN, e.g.:

        > cd $DCM4CHEE_XDS2/ldap
        > sed -i s/dc=example,dc=com/dc=my-domain,dc=com/ init-baseDN.ldif
        > sed -i s/dc=example,dc=com/dc=my-domain,dc=com/ init-config.ldif
        > sed -i s/dc=example,dc=com/dc=my-domain,dc=com/ sample-config.ldif

3.  If there is not already a base entry in the directory data base, import
    `$DCM4CHEE_XDS2/ldap/init-baseDN.ldif` using the LDIF import function of
    Apache Directory Studio LDAP Browser.

4.  If there are not already DICOM configuration root entries in the directory
    data base, import `$DCM4CHEE_XDS2/ldap/init-config.ldif` using the LDIF import
    function of Apache Directory Studio LDAP Browser.  

5.  Import `$DCM4CHEE_XDS2/ldap/sample-config.ldif` using the LDIF import function
    of Apache Directory Studio LDAP Browser.  

6.  By default configuration, the HL7 service of DCM4CHEE XDS2 registry does not accept remote connections.
    To enable remote connections, replace the value of attribute

        dicomHostname=localhost
    
    of the 2 `dicomNetworkConnection` objects

        dc=example,dc=com
        + cn=XDS Configuration
          + cn=Devices
            + dicomDeviceName=dcm4chee-xds2-registry
                cn=hl7
                cn=hl7-tls

    by the actual hostname of your system, using Apache Directory Studio LDAP Browser. 



Setup JBoss AS 7
----------------

1.  Adjust DCM4CHEE XDS2 LDAP Connection configuration file
    `$DCM4CHEE_ARC/configuration/dcm4chee-xds2/ldap.properties`:

        java.naming.factory.initial=com.sun.jndi.ldap.LdapCtxFactory
        java.naming.ldap.attributes.binary=dicomVendorData
        java.naming.provider.url=ldap://localhost:1389/dc=example,dc=com
        java.naming.security.principal=cn=Directory Manager
        java.naming.security.credentials=secret

    to your LDAP Server configuration.

2.  Install required libraries as JBoss AS 7 modules:
    
    Install DCM4CHE 3.2.1 libraries as JBoss AS 7 module:
    ```
        cd  $JBOSS_HOME
        unzip $DCM4CHEE_XDS2/jboss-module/dcm4che-jboss-modules-3.2.1.zip
    ```
    Install QueryDSL 2.8.1 libraries as JBoss AS 7 module:
    ```
        cd  $JBOSS_HOME
        unzip $DCM4CHEE_XDS2/jboss-module/querydsl-jboss-modules-3.2.3.zip
    ```
3.  Install XDS command line tools

    Install DCM4CHEE XDS2 tools libraries as JBoss AS 7 module:
    ```
        cd  $JBOSS_HOME
        cp -r $DCM4CHEE_XDS2/modules .
    ```
    Install shellscripts:
    ```
        cd  $JBOSS_HOME
        cp -r $DCM4CHEE_XDS2/bin .
    ```

4.  Install JDBC Driver. DCM4CHEE XDS 2.x binary distributions do not include
    a JDBC driver for the database for license issues. You may download it from:
    -   [MySQL](http://www.mysql.com/products/connector/)
    -   [PostgreSQL]( http://jdbc.postgresql.org/)
    -   [Firebird](http://www.firebirdsql.org/en/jdbc-driver/)
    -   [DB2](http://www-306.ibm.com/software/data/db2/java/), also included in DB2 Express-C
    -   [Oracle](http://www.oracle.com/technology/software/tech/java/sqlj_jdbc/index.htm),
        also included in Oracle 11g XE)
    -   [Microsoft SQL Server](http://msdn.microsoft.com/data/jdbc/)

    The JDBC driver can be installed either as a deployment or as a core module.
    [See](https://docs.jboss.org/author/display/AS71/Developer+Guide#DeveloperGuide-InstalltheJDBCdriver)
    
    Installation as deployment is limited to JDBC 4-compliant driver consisting of **one** JAR.

    For installation as a core module, `$DCM4CHEE_XDS2/jboss-module/jdbc-jboss-modules-1.0.0-<database>.zip`
    already provides a module definition file `module.xml`. You just need to extract the ZIP file into
    $JBOSS_HOME and copy the JDBC Driver file(s) into the sub-directory, e.g.:

        > cd $JBOSS_HOME
        > unzip $DCM4CHEE_XDS2/jboss-module/jdbc-jboss-modules-1.0.0-db2.zip
        > cd $DB2_HOME/java
        > cp db2jcc4.jar db2jcc_license_cu.jar $JBOSS_HOME/modules/com/ibm/db2/main/

    Verify, that the actual JDBC Driver file(s) name matches the path(s) in the provided
    `module.xml`, e.g.:

         <?xml version="1.0" encoding="UTF-8"?>
         <module xmlns="urn:jboss:module:1.1" name="com.ibm.db2">
             <resources>
                 <resource-root path="db2jcc4.jar"/>
                 <resource-root path="db2jcc_license_cu.jar"/>
             </resources>
         
             <dependencies>
                 <module name="javax.api"/>
                 <module name="javax.transaction.api"/>
             </dependencies>
         </module>


5.  Start JBoss AS 7 in standalone mode with the Java EE 6 Full Profile configuration.
    To preserve the original JBoss AS 7 configuration you may copy the original
    configuration file for JavaEE 6 Full Profile:

        > cd $JBOSS_HOME/standalone/configuration/
        > cp standalone-full.xml dcm4chee-xds2.xml

    and start JBoss AS 7 specifying the new configuration file:
        
        > $JBOSS_HOME/bin/standalone.sh -c dcm4chee-xds2.xml [UNIX]
        > %JBOSS_HOME%\bin\standalone.bat -c dcm4chee-xds2.xml [Windows]
   
    Verify, that JBoss AS 7 started successfully, e.g.:

        =========================================================================

          JBoss Bootstrap Environment

          JBOSS_HOME: /home/gunter/jboss7

          JAVA: /usr/lib/jvm/java-6-openjdk/bin/java

          JAVA_OPTS:  -server -XX:+UseCompressedOops -XX:+TieredCompilation ...

        =========================================================================

        13:01:48,788 INFO  [org.jboss.modules] JBoss Modules version 1.1.1.GA
        13:01:48,926 INFO  [org.jboss.msc] JBoss MSC version 1.0.2.GA
        13:01:48,969 INFO  [org.jboss.as] JBAS015899: JBoss AS 7.1.1.Final "Brontes" starting
        :
        13:01:51,239 INFO  [org.jboss.as] (Controller Boot Thread) JBAS015874: JBoss AS 7.1.1.Final "Brontes" started ...
                
    Running JBoss AS 7 in domain mode should work, but was not yet tested.

6.  Add JDBC Driver into the server configuration using JBoss AS 7 CLI in a new console window:

        > $JBOSS_HOME/bin/jboss-cli.sh -c [UNIX]
        > %JBOSS_HOME%\bin\jboss-cli.bat -c [Windows]
        [standalone@localhost:9999 /] /subsystem=datasources/jdbc-driver=<driver-name>:add(driver-module-name=<module-name>)

    You may choose any `<driver-name>` for the JDBC Driver, `<module-name>` must match the name
    defined in the module definition file `module.xml` of the JDBC driver, e.g.:

        [standalone@localhost:9999 /] /subsystem=datasources/jdbc-driver=db2:add(driver-module-name=com.ibm.db2)

7.  Create and enable a new Data Source bound to JNDI name `java:/xdsDS` using JBoss AS 7 CLI:

        [standalone@localhost:9999 /] data-source add --name=xdsDS \
        >     --driver-name=<driver-name> \
        >     --connection-url=<jdbc-url> \
        >     --jndi-name=java:/xdsDS \
        >     --user-name=<user-name> \
        >     --password=<user-password>
        [standalone@localhost:9999 /] data-source enable --name=xdsDS

    The format of `<jdbc-url>` is JDBC Driver specific, e.g.:
    -  MySQL: `jdbc:mysql://localhost:3306/<database-name>`
    -  PostgreSQL: `jdbc:postgresql://localhost:5432/<database-name>`
    -  Firebird: `jdbc:firebirdsql:localhost/3050:<database-name>`
    -  DB2: `jdbc:db2://localhost:50000/<database-name>`
    -  Oracle: `jdbc:oracle:thin:@localhost:1521:<database-name>`
    -  Microsoft SQL Server: `jdbc:sqlserver://localhost:1433;databaseName=<database-name>`

8. At default, DCM4CHEE XDS 2.x will look for the LDAP connection configuration file at

        $JBOSS_HOME/standalone/configuration/dcm4chee-xds2/ldap.properties

    You may specify a different location by system property `org.dcm4chee.xds.ldapPropertiesURL`
    using JBoss AS 7 CLI:

        [standalone@localhost:9999 /] /system-property=org.dcm4chee.xds.ldapPropertiesURL:add(value=<url>)

    If DCM4CHEE XDS 2.x cannot find the LDAP connection configuration on the specified location, it
    will try to fetch the Archive configuration from Java Preferences.

9. At default, DCM4CHEE XDS 2.x will assume `dcm4chee-xds2-registry` as its Device Name, used to find its
    configuration in the configuration backend (LDAP Server or Java Preferences). You may specify a different
    Device Name by system property `org.dcm4chee.xds.deviceName` using JBoss AS 7 CLI:

        [standalone@localhost:9999 /] /system-property=org.dcm4chee.xds.deviceName:add(value=<device-name>)

10. Deploy DCM4CHEE XDS 2.x using JBoss AS 7 CLI, e.g.:

        [standalone@localhost:9999 /] deploy $DCM4CHEE_XDS2/deploy/dcm4chee-xds2-ear-2.0.0-<database-name>.ear

    Verify that DCM4CHEE XDS was deployed and started successfully, e.g.:


11. You may undeploy DCM4CHEE XDS at any time using JBoss AS 7 CLI, e.g.:

        [standalone@localhost:9999 /] undeploy dcm4chee-xds2-ear-2.0.0-<database-name>.ear

12. Initialize XDS Registry with ebXML Classifications and Associations defined by XDS:

    a) Default XDS configuration on default local installation
    ```
    > $JBOSS_HOME/bin/xdsinit.sh [UNIX]
    > %JBOSS_HOME%\bin\xdsinit.bat [Windows]
    ```
    b) Default XDS configuration on user specific (different port) or remote installation
    ```
    e.g.: initialize XDS Registry on host 'xdsserver' port 8180 with defaults:
    > $JBOSS_HOME/bin/xdsinit.sh -wsdl http://xdsserver:8180/XDSbRegistry?wsdl [UNIX]
    > %JBOSS_HOME%\bin\xdsinit.bat -wsdl http://xdsserver:8180/XDSbRegistry?wsdl [Windows]
    ```
    c) Add new ebXML Classifications and Associations given by XML files (SubmitObjectRequest)
    ```
    > $JBOSS_HOME/bin/xdsinit.sh <filename1> [<filename2> [..]] [UNIX]
    > %JBOSS_HOME%\bin\xdsinit.bat <filename1> [<filename2> [..]] [Windows]
    ```

Testing DCM4CHEE XDS 2.x
----------------------------

