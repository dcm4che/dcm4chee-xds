dcm4chee-xds
============
Sources: https://github.com/dcm4che/dcm4chee-xds  
Binaries:   
Issue Tracker: http://www.dcm4che.org/jira/browse/XDSB 

XDS.b Implementation running on JBoss 7 application server


Build
-----
After installation of [Maven 3](http://maven.apache.org):

   for java preferences config profile:
 
      mvn install -Ddb={db2|firebird|h2|mysql|oracle|psql|sqlserver}
      
   for ldap config profile:
  
      mvn install -Ddb={db2|firebird|h2|mysql|oracle|psql|sqlserver} -Pldap -Dldap={apacheds|opends|slapd}

Installation
------------
See [INSTALL.md](https://github.com/dcm4che/dcm4chee-xds/blob/master/INSTALL.md).

License
-------
* [Mozilla Public License Version 1.1](http://www.mozilla.org/MPL/1.1/)
