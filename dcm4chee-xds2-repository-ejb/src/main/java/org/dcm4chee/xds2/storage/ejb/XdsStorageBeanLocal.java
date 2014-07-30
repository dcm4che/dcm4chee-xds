package org.dcm4chee.xds2.storage.ejb;

import java.util.List;

import org.dcm4chee.storage.entity.FileSystem;
import org.dcm4chee.xds2.repository.persistence.XdsDocument;
import org.dcm4chee.xds2.repository.persistence.XdsFileRef;

public interface XdsStorageBeanLocal {

	XdsFileRef createFile(String filePath, String mimetype, long size, String digest, FileSystem fs, String docUID);

	XdsDocument createDocument(String docUID, String mimetype, long size, String digest);
	XdsDocument findDocument(String docUID);
	List<XdsFileRef> findFileRefs(String docUID);
	
	List<XdsFileRef> deleteDocument(String... docUID);
	List<XdsFileRef> deleteDocument(List<String> docUIDs);
}