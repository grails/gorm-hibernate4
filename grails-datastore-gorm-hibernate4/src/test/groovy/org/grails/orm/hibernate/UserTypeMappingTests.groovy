package org.grails.orm.hibernate

import grails.persistence.Entity
import org.hibernate.HibernateException
import org.hibernate.collection.internal.PersistentSortedSet
import org.hibernate.collection.spi.PersistentCollection
import org.hibernate.engine.spi.SessionImplementor
import org.hibernate.persister.collection.CollectionPersister
import org.hibernate.type.YesNoType
import org.hibernate.usertype.UserCollectionType
import org.junit.Test

import java.util.concurrent.ConcurrentSkipListSet

import static junit.framework.Assert.*

/**
* @author Graeme Rocher
*/
class UserTypeMappingTests extends AbstractGrailsHibernateTests{


    @Test
    void testCustomUserType() {
        def person = UserTypeMappingTestsPerson.newInstance(name:"Fred", weight:Weight.newInstance(200))
        person.addToTelephoneNumbers(new UserTypeMappingTestsTelephoneNumber(person:person, telephoneNumber:"1-555-555-5555"))

        person.save(flush:true)
        session.clear()

        person = UserTypeMappingTestsPerson.get(1)

        assertNotNull person
        assertEquals 200, person.weight.pounds
        assertEquals "1-555-555-5555", person.telephoneNumbers.first().telephoneNumber
        assertEquals person.telephoneNumbers.getClass(), UserTypeMappingTestsSortedSet
    }

    @Test
    void testUserTypeMapping() {


        assertNotNull UserTypeMappingTest.newInstance(active:true).save(flush:true)


        def con
        try {
            con = session.connection()
            def statement = con.prepareStatement("select * from type_test")
            def result = statement.executeQuery()
            assertTrue result.next()
            def value = result.getString('active')

            assertEquals "Y", value
        }
        finally {
            con.close()
        }
    }

    @Test
    void testUserTypePropertyMetadata() {
        def personDomainClass = ga.getDomainClass(UserTypeMappingTestsPerson.name)
        def person = UserTypeMappingTestsPerson.newInstance(name:"Fred", weight:Weight.newInstance(200))

        // the metaClass should report the correct type, not Object
        assertEquals Weight, UserTypeMappingTestsPerson.metaClass.hasProperty(person, "weight").type

        // GrailsDomainClassProperty should not appear to be an association
        def prop = personDomainClass.getPropertyByName("weight")
        assertFalse prop.isAssociation()
        assertFalse prop.isOneToOne()
        assertEquals Weight, prop.type
    }

    @Override
    protected getDomainClasses() {
        [UserTypeMappingTest, UserTypeMappingTestsPerson, UserTypeMappingTestsTelephoneNumber]
    }
}

@Entity
class UserTypeMappingTest {
    Long id
    Long version

    Boolean active

    static mapping = {
        table 'type_test'
        columns {
            active (column: 'active', type: YesNoType)
        }
    }
}

@Entity
class UserTypeMappingTestsPerson {
    Long id
    Long version
    String name
    Weight weight
    SortedSet<UserTypeMappingTestsTelephoneNumber> telephoneNumbers = new ConcurrentSkipListSet<UserTypeMappingTestsTelephoneNumber>()

    static hasMany = [
            telephoneNumbers: UserTypeMappingTestsTelephoneNumber
    ]

    static constraints = {
        name(unique: true)
        weight(nullable: true)
    }

    static mapping = {
        columns {
            weight(type:WeightUserType)
        }
        telephoneNumbers type:UserTypeMappingTestsSortedSetUserCollectionType
    }
}

@Entity
class UserTypeMappingTestsTelephoneNumber implements Comparable<UserTypeMappingTestsTelephoneNumber> {
    Long id
    String telephoneNumber

    static belongsTo = [person:UserTypeMappingTestsPerson]

    @Override
    int compareTo(UserTypeMappingTestsTelephoneNumber o) {
        return telephoneNumber?.compareTo(o.telephoneNumber)
    }
}

class UserTypeMappingTestsSortedSetUserCollectionType implements UserCollectionType {
    @Override
    PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister) throws HibernateException {
        return new UserTypeMappingTestsSortedSet(session)
    }

    @Override
    PersistentCollection wrap(SessionImplementor session, Object collection) {
        return new UserTypeMappingTestsSortedSet(session, (SortedSet) collection)
    }

    @Override
    Iterator getElementsIterator(Object collection) {
        return ((SortedSet) collection).iterator()
    }

    @Override
    boolean contains(Object collection, Object entity) {
        return ((SortedSet) collection).contains(entity)
    }

    @Override
    Object indexOf(Object collection, Object entity) {
        throw new UnsupportedOperationException("indexOf not supported for a set")
    }

    @Override
    @SuppressWarnings("unchecked")
    Object replaceElements(Object original, Object target, CollectionPersister persister, Object owner, Map copyCache, SessionImplementor session) throws HibernateException {
        SortedSet result = (SortedSet) target
        result.clear()
        result.addAll((SortedSet) target)
        return result
    }

    @Override
    Object instantiate(int anticipatedSize) {
        return new ConcurrentSkipListSet()
    }
}

class UserTypeMappingTestsSortedSet extends PersistentSortedSet {
    UserTypeMappingTestsSortedSet(SessionImplementor session) {
        super(session);
    }

    UserTypeMappingTestsSortedSet(SessionImplementor session, SortedSet set) {
        super(session, set);
    }
}
