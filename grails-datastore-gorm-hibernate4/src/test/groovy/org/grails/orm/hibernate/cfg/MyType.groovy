package org.grails.orm.hibernate.cfg

/**
 * Example custom type used for testing.
 */
class MyType implements Serializable {
	String name

	String toString() { name }
}
