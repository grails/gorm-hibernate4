package org.grails.orm.hibernate

import grails.persistence.Entity
import grails.transaction.Rollback
import org.grails.orm.hibernate.cfg.GrailsDomainBinder
import org.hibernate.type.YesNoType
import org.springframework.transaction.PlatformTransactionManager
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class MappingDefaultsTests extends Specification {


    protected ConfigObject getConfig() {
        new ConfigSlurper().parse('''
dataSource.dbCreate = 'create-drop'
grails.gorm.default.mapping = {
//   cache true
   id generator:'sequence'
   'user-type'(type: org.hibernate.type.YesNoType, class: Boolean)

}
grails.gorm.default.constraints = {
   '*'(nullable:true, size:1..20)
   test blank:false
   another email:true
}
''')
    }

    @Shared @AutoCleanup HibernateDatastore hibernateDatastore = new HibernateDatastore(config, MappingDefaults)
    @Shared PlatformTransactionManager transactionManager = hibernateDatastore.getTransactionManager()

    @Rollback
    void testGlobalUserTypes() {
        given:
        def domain = hibernateDatastore.mappingContext.getPersistentEntity(MappingDefaults.name)

        when:
        def mapping = new GrailsDomainBinder().getMapping(domain)

        then:
        YesNoType == mapping.userTypes[Boolean]

        when:
        def i = domain.javaClass.newInstance(name:"helloworld", test:true)
        then:"should have saved instance"
        i.save(flush:true)

        when:
        def session = hibernateDatastore.sessionFactory.currentSession
        session.clear()
        def rs = session.connection().prepareStatement("select test from mapping_defaults").executeQuery()
        rs.next()
        then:
        "Y" == rs.getString("test")
    }

}

@Entity
class MappingDefaults {
    Long id
    Long version

    String name
    Boolean test

    static constraints = {
        name(shared:"test")
    }
}