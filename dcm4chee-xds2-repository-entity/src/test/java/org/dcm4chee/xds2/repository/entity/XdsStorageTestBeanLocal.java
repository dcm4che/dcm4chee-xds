package org.dcm4chee.xds2.repository.entity;

import org.dcm4chee.storage.entity.Availability;
import org.dcm4chee.storage.entity.FileSystem;
import org.dcm4chee.storage.entity.FileSystemStatus;
import org.dcm4chee.xds2.repository.persistence.XdsFileRef;

public interface XdsStorageTestBeanLocal {

	FileSystem createFileSystem(String grpName, String fileURL, Availability availability, FileSystemStatus status);
	void linkFileSystems(FileSystem... filesystems);
	XdsFileRef createFile(String filePath, String mimetype, long size, String digest, FileSystem fs);
	void deleteFsGroup(String grpName);
}