package grails.test.mixin.hibernate

import grails.gorm.annotation.Entity
import grails.test.mixin.TestMixin
import grails.test.mixin.gorm.Domain
import spock.lang.Specification

/**
 * Created by graemerocher on 15/07/2016.
 */
@Domain(Book)
@TestMixin(HibernateTestMixin)
class HibernateTestMixinSpec extends Specification {

    void "Test hibernate test mixin"() {
        expect:
        new Book(title:"The Stand").save(flush:true)
        Book.count() == 1
    }
}
@Entity
class Book {
    String title
}
