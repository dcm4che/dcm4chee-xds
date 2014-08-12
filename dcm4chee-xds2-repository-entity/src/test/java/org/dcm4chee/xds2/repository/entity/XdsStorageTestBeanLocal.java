package org.dcm4chee.xds2.repository.entity;

import java.util.List;

import org.dcm4chee.xds2.repository.persistence.XdsDocument;
import org.dcm4chee.xds2.repository.persistence.XdsFileRef;

public interface XdsStorageTestBeanLocal {

    XdsFileRef createFile(String groupID, String filesystemID, String docUID, String filePath, String mimetype, long size, String digest);

    XdsDocument createDocument(String docUID, String mimetype, long size, String digest);
    XdsDocument findDocument(String docUID);
    List<XdsFileRef> findFileRefs(String docUID);
    
    void deleteGroup(String groupID);
}