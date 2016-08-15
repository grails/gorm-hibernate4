package grails.gorm.tests

import grails.gorm.annotation.Entity
import org.grails.orm.hibernate.GormSpec

/**
 * Created by Jim on 8/15/2016.
 */
class RLikeSpec extends GormSpec {

    void "test rlike works with H2"() {
        given:
        new RlikeFoo(name: "ABC").save(flush: true)
        new RlikeFoo(name: "ABCDEF").save(flush: true)
        new RlikeFoo(name: "ABCDEFGHI").save(flush: true)

        when:
        session.clear()
        List<RlikeFoo> allFoos = RlikeFoo.findAllByNameRlike(".*")

        then:
        allFoos.size() == 3
    }

    @Override
    List getDomainClasses() {
        [RlikeFoo]
    }
}

@Entity
class RlikeFoo {
    String name
}
