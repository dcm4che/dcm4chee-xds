package org.dcm4chee.xds2.ws.repository;

import java.io.File;

import javax.ejb.EJB;

import org.dcm4chee.xds2.ws.registry.SubmitObjReqTest;
import org.dcm4chee.xds2.ws.registry.XDSRegistryBean;
import org.dcm4chee.xds2.ws.registry.XDSTestUtil;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class RepositoryTest {
	

    @Deployment
    public static WebArchive createDeployment() {
       WebArchive web =  XDSTestUtil.createDeploymentArchive(RepositoryTest.class);
        
       web.merge(ShrinkWrap.create(WebArchive.class).as(ExplodedImporter.class)  
    		    .importDirectory("src/test/resources").as(WebArchive.class),  
    		    "/", Filters.includeAll());  
		  
       return web;
    }
    
    @Test
    public void storeAndRetrieve() {
        //todo
    }

}
