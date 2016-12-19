package org.grails.orm.hibernate.cfg;

import junit.framework.TestCase;
import org.grails.core.support.GrailsDomainConfigurationUtil;


public class GrailsDomainConfigurationUtilTests extends TestCase {

	public void testGetMappingFileName() {
		assertEquals("org/grails/orm/hibernate/HibernateMappedClass.hbm.xml",
				GrailsDomainConfigurationUtil.getMappingFileName("org.grails.orm.hibernate.HibernateMappedClass"));
	}
}
