/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2014
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
package org.dcm4chee.xds2.conf.misc;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SchemaConverter {

    static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return encoding.decode(ByteBuffer.wrap(encoded)).toString();
    }

    public static void main(String[] args) throws IOException {

        Path currentRelativePath = Paths.get("");
        String s = currentRelativePath.toAbsolutePath().toString();
        System.out.println("Current relative path is: " + s);
        
        String data = readFile("src/main/config/ldap/schema/dcm4chee-xds.schema", StandardCharsets.UTF_8);

        // remove comments
        data = data.replaceAll("\\#.*\\r\\n", "");

        // add header
        String header = String.format("%s\n%s\n%s\n%s\n%s\n", 
                "# dcm4chee-xds extensions of the DICOM Application Configuration Data Model Hierarchy LDAP Schema",
                "dn: cn=schema", "objectClass: top", "objectClass: ldapSubentry", "objectClass: subschema");
        data = header+data;
        
        // remove whitespaces between linebrakes
        data = data.replaceAll("\\r\\n\\s+\\r\\n", "\r\n\r\n");
        
        // leave only one linebreak in a row
        data = data.replaceAll("[\\r\\n]+", "\r\n");
        
        // open ds format 
        data = data.replaceAll("attributetype", "attributeTypes:");
        data = data.replaceAll("objectclass", "objectClasses:");
        
        PrintWriter out = new PrintWriter("src/main/config/ldap/opends/20-dcm4chee-xds.ldif");

        out.write(data);
        out.close();
        
    }

}
