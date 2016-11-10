package org.grails.orm.hibernate

import grails.persistence.Entity
import grails.transaction.Rollback
import org.junit.Test
import org.springframework.transaction.PlatformTransactionManager
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class InheritanceWithAssociationsTests extends Specification {
    @Shared @AutoCleanup HibernateDatastore hibernateDatastore = new HibernateDatastore(InheritanceWithAssociationsA, InheritanceWithAssociationsLink, InheritanceWithAssociationsLinkToA, InheritanceWithAssociationsRoot)
    @Shared PlatformTransactionManager transactionManager = hibernateDatastore.getTransactionManager()



    @Rollback
    void "Test inheritance with association"() {

        when:
        InheritanceWithAssociationsRoot root = new InheritanceWithAssociationsRoot()
        InheritanceWithAssociationsA a = new InheritanceWithAssociationsA()
        InheritanceWithAssociationsLinkToA link = new InheritanceWithAssociationsLinkToA()
        link.a = a
        a.link = link

        a.save(flush:true)

        root.addToLinks(link)
        root.save(flush:true)
        root.discard()

        root = InheritanceWithAssociationsRoot.get(1)

        then:
        root != null
        root.links.size() == 1
        root.links.iterator().next()
        root.links.iterator().next().a
    }

}

@Entity
class InheritanceWithAssociationsRoot {
    static hasMany = [links : InheritanceWithAssociationsLink]
}
@Entity
class InheritanceWithAssociationsLink {
    static belongsTo = [root:InheritanceWithAssociationsRoot]

    static constraints = {
        root(nullable:true)
    }
}
@Entity
class InheritanceWithAssociationsLinkToA extends InheritanceWithAssociationsLink {
    static belongsTo = [a:InheritanceWithAssociationsA]
}

@Entity
class InheritanceWithAssociationsA {
    InheritanceWithAssociationsLinkToA link
}





