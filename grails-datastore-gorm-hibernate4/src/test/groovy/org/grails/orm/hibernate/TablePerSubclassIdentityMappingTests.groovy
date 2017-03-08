package org.grails.orm.hibernate

import grails.gorm.annotation.Entity
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Mar 18, 2008
 */
class TablePerSubclassIdentityMappingTests extends AbstractGrailsHibernateTests {


    @Test
    void testMappedIdentityForSubclass() {

         def con
         try {
             con = session.connection()
             def statement = con.prepareStatement("select STATION_EVENT_ID from plate_event")
             statement.execute()
             statement = con.prepareStatement("select STATION_EVENT_ID from station_event")
             statement.execute()

         } finally {
             con.close()
         }
    }

    @Override
    protected getDomainClasses() {
        [StationEvent, PlateEvent]
    }
}

@Entity
class StationEvent {
    Long id
    Long version
    String station

    static mapping = {
        id column: 'STATION_EVENT_ID'
        tablePerSubclass true
    }
}

@Entity
class PlateEvent extends StationEvent {
    String plate
    static mapping = {
        id column: "STATION_EVENT_ID"
        version false
    }
}
