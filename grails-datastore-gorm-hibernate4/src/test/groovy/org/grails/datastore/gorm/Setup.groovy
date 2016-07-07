package org.grails.datastore.gorm

import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import grails.core.GrailsDomainClass
import grails.validation.ConstrainedProperty
import grails.validation.ConstraintsEvaluator
import groovy.sql.Sql
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.datastore.mapping.model.MappingContext
import org.grails.orm.hibernate.GrailsHibernateTransactionManager
import org.grails.orm.hibernate.HibernateDatastore
import org.grails.orm.hibernate.cfg.HibernateMappingContextConfiguration
import org.grails.orm.hibernate.connections.HibernateConnectionSource
import org.grails.orm.hibernate.connections.HibernateConnectionSourceFactory
import org.grails.orm.hibernate.connections.HibernateConnectionSourceSettings
import org.grails.orm.hibernate.proxy.HibernateProxyHandler
import org.grails.orm.hibernate.validation.HibernateConstraintsEvaluator
import org.grails.orm.hibernate.validation.HibernateDomainClassValidator
import org.grails.orm.hibernate.validation.PersistentConstraintFactory
import org.grails.orm.hibernate.validation.UniqueConstraint

//import org.codehaus.groovy.grails.plugins.web.api.ControllersDomainBindingApi
import org.h2.Driver
import org.hibernate.SessionFactory
import org.hibernate.cache.ehcache.EhCacheRegionFactory
import org.hibernate.cfg.AvailableSettings
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.DisposableBean
import org.springframework.context.ApplicationContext
import org.springframework.context.support.GenericApplicationContext
import org.springframework.orm.hibernate4.SessionFactoryUtils
import org.springframework.orm.hibernate4.SessionHolder
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.springframework.transaction.support.TransactionSynchronizationManager

class Setup {
    static GrailsApplication grailsApplication
    static HibernateDatastore hibernateDatastore
    static hibernateSession
    static GrailsHibernateTransactionManager transactionManager
    static SessionFactory sessionFactory
    static TransactionStatus transactionStatus
    static HibernateMappingContextConfiguration hibernateConfig
    static ApplicationContext applicationContext

    @CompileStatic
    static destroy() {
        Throwable exception
        if (transactionStatus != null) {
            def tx = transactionStatus
            transactionStatus = null
            try {
                transactionManager.rollback(tx)
            } catch (Throwable e) {
                exception = e
            }
        }
        if (hibernateSession != null) {
            SessionFactoryUtils.closeSession( (org.hibernate.Session)hibernateSession )
        }

        hibernateDatastore.destroy()
        shutdownInMemDb(hibernateDatastore)

        if(hibernateConfig != null) {
            hibernateConfig = null
        }
        grailsApplication = null
        hibernateDatastore = null
        hibernateSession = null
        transactionManager = null
        sessionFactory = null
        if(applicationContext instanceof DisposableBean) {
            applicationContext.destroy()
        }
        applicationContext = null
        if(exception != null) {
            throw exception
        }

    }
    
    static shutdownInMemDb(HibernateDatastore datastore) {
        Sql sql = null
        try {
            HibernateConnectionSource source = datastore.getConnectionSources().defaultConnectionSource

            def url = source.settings.dataSource.url
            sql = Sql.newInstance(url, 'sa', '', Driver.name)
            sql.executeUpdate('SHUTDOWN')
        } catch (e) {
            // already closed, ignore
            println "error shutting down db:$e.message"
        } finally {
            try { sql?.close() } catch (ignored) {}
        }
    }

    static Session setup(List<Class> classes, ConfigObject grailsConfig = new ConfigObject(), boolean isTransactional = true) {
        if(grailsConfig == null) grailsConfig = new ConfigObject()
        grailsConfig.dataSource.dbCreate = "create-drop"


        Properties props = new Properties()
        props.put AvailableSettings.USE_SECOND_LEVEL_CACHE, "true"
        props.put AvailableSettings.USE_QUERY_CACHE, "true"
        props.put AvailableSettings.CACHE_REGION_FACTORY, EhCacheRegionFactory.name
        grailsConfig.merge( new ConfigSlurper().parse(props) )


        def classesArray = classes as Class[]
        grailsApplication = new DefaultGrailsApplication(classesArray, new GroovyClassLoader(Setup.getClassLoader()))
        if(grailsConfig) {
            grailsApplication.config.putAll(grailsConfig)
        }


        def ctx = new GenericApplicationContext()
        ctx.refresh()
        applicationContext = ctx
        grailsApplication.applicationContext = ctx
        grailsApplication.mainContext = ctx
        grailsApplication.initialise()

        HibernateConnectionSourceFactory factory = new HibernateConnectionSourceFactory(classesArray)
        factory.setApplicationContext(applicationContext)
        hibernateDatastore = new HibernateDatastore(DatastoreUtils.createPropertyResolver(grailsConfig), factory)


        HibernateConstraintsEvaluator evaluator = new HibernateConstraintsEvaluator()
        evaluator.setMappingContext(hibernateDatastore.mappingContext)
        ctx.beanFactory.registerSingleton(ConstraintsEvaluator.BEAN_NAME, evaluator)
        def mappingContext = hibernateDatastore.mappingContext
        ConstrainedProperty.registerNewConstraint(UniqueConstraint.UNIQUE_CONSTRAINT,
                new PersistentConstraintFactory(ctx, UniqueConstraint))

        ctx.beanFactory.registerSingleton("sessionFactory", hibernateDatastore.getSessionFactory())
        ctx.beanFactory.registerSingleton("mappingContext", hibernateDatastore.getMappingContext())
        ctx.beanFactory.registerSingleton("grailsApplication", grailsApplication)

        grailsApplication.domainClasses.each { GrailsDomainClass dc ->
            if (dc.abstract) {
                return
            }

            def validator = new HibernateDomainClassValidator()



            validator.mappingContext = mappingContext
            validator.grailsApplication = grailsApplication
            validator.domainClass = dc
            validator.messageSource = ctx
            validator.proxyHandler = new HibernateProxyHandler()
            dc.validator = validator
            def entity = mappingContext.getPersistentEntity(dc.fullName)
            if(entity != null) {
                mappingContext.addEntityValidator(entity, validator)
            }

            dc.metaClass.constructor = { ->
                def obj
                if (ctx.containsBean(dc.fullName)) {
                    obj = ctx.getBean(dc.fullName)
                }
                else {
                    obj = BeanUtils.instantiateClass(dc.clazz)
                }
                obj
            }
        }

        ctx.defaultListableBeanFactory.registerSingleton('hibernateDatastore', hibernateDatastore)
        transactionManager = hibernateDatastore.getTransactionManager()
        sessionFactory = hibernateDatastore.sessionFactory
        if (transactionStatus == null && isTransactional) {
            transactionStatus = transactionManager.getTransaction(new DefaultTransactionDefinition())
        }
        else if(isTransactional){
            throw new RuntimeException("new transaction started during active transaction")
        }
        if(!isTransactional) {
            hibernateSession = sessionFactory.openSession()
            TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(hibernateSession))
        }
        else {
            hibernateSession = sessionFactory.currentSession
        }

        return hibernateDatastore.connect()
    }
}
