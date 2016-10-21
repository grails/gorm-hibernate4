package grails.gorm.tests

import grails.gorm.annotation.Entity
import grails.transaction.Rollback
import spock.lang.Issue

/**
 * Created by graemerocher on 21/10/16.
 */
class SaveWithExistingValidationErrorSpec extends GormDatastoreSpec{

    @Issue('https://github.com/grails/grails-core/issues/9820')
    void "test saving an object with another invalid object"() {
        when:"An object with a validation error is assigned"
        def testB = new ObjectB()
        testB.save(flush: true) //fails because name is not nullable

        def testA = new ObjectA(test: testB)
        testA.save(flush: true)

        then:"Neither objects were saved"
        ObjectA.count == 0
        ObjectB.count == 0
        testA.errors.getFieldError("test.name")
    }

    @Override
    List getDomainClasses() {
        [ObjectA, ObjectB]
    }
}
@Entity
class ObjectA {

    ObjectB test

    static constraints = {
    }
}
@Entity
class ObjectB {

    String name

    static constraints = {
    }
}