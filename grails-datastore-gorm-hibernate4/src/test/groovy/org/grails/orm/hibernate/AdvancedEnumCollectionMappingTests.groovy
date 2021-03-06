package org.grails.orm.hibernate

import grails.persistence.Entity

import org.junit.Test

import static junit.framework.Assert.*
/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Feb 10, 2009
 */
class AdvancedEnumCollectionMappingTests extends AbstractGrailsHibernateTests {

    protected getDomainClasses() {
        [EnumCollectionMappingUser]
    }

    @Override
    protected void onSetUp() {
        super.onSetUp()
//        ConstrainedProperty.registerNewConstraint("unique", new UniqueConstraintFactory(new SimpleMapDatastore()))
    }

    @Test
    void testAdvancedEnumCollectionMapping() {
        def User = EnumCollectionMappingUser
        def Role = EnumCollectionMappingRole

        def user = User.newInstance(name:"Fred")
        user.primaryRole = Role.EMPLOYEE

        assertNotNull "user should have saved", user.save(flush:true)

        user.addToRoles(Role.EMPLOYEE)
        user.save(flush:true)

        session.clear()

        user = User.get(1)

        assertEquals 1, user.roles.size()
        def role = user.roles.iterator().next()

        assertEquals Role.EMPLOYEE, role

        def conn = session.connection()

    }
}

@Entity
class EnumCollectionMappingUser {

    Long id
    Long version
    Set roles
    static hasMany = [roles: EnumCollectionMappingRole]
    EnumCollectionMappingRole primaryRole

    String name

    static constraints = {
        name(blank: false, matches: "[a-zA-Z]+", maxSize: 20, unique: true)
    }

    String toString() { name}
}

enum EnumCollectionMappingRole {
    ADMIN("0"), MANAGER("2"), EMPLOYEE("4")
    EnumCollectionMappingRole(String id) { this.id = id }
    final String id
}
