import grails.orm.bootstrap.HibernateDatastoreSpringInitializer
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsClass
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.config.GormProperties
import org.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.grails.orm.hibernate.support.AbstractMultipleDataSourceAggregatePersistenceContextInterceptor
import org.grails.orm.hibernate.validation.HibernateDomainClassValidator
import org.grails.orm.hibernate.validation.PersistentConstraintFactory
import org.grails.orm.hibernate4.support.GrailsOpenSessionInViewInterceptor
import org.springframework.context.*
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.codehaus.groovy.grails.commons.*
import org.grails.orm.hibernate.SessionFactoryHolder
import org.grails.orm.hibernate.cfg.GrailsDomainBinder
import org.grails.orm.hibernate.cfg.HibernateMappingContext
import org.grails.orm.hibernate.cfg.Mapping

import org.grails.orm.hibernate.validation.UniqueConstraint
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.ApplicationContext
import org.springframework.validation.Validator

class Hibernate4GrailsPlugin {
    def version = "6.1.0.RC2" // added by Gradle
    def license = "Apache 2.0 License"
    def organization = [name: "Grails", url: "http://grails.org/"]
    def developers = [
            [name: "Graeme Rocher", email: "graeme@grails.org"]]
    def issueManagement = [system: "Github", url: "https://github.com/grails/grails-data-mapping"]
    def scm = [url: "https://github.com/grails/grails-data-mapping"]

    def grailsVersion = "2.5.0 > *"
    def observe = ['services', 'domainClass']
    def loadAfter = ['domainClass']
    def author = "Graeme Rocher"
    def authorEmail = "graeme@grails.org"
    def title = "GORM for Hibernate 4"
    def description = 'Provides integration between Grails and Hibernate 4 through GORM'

    def documentation = "http://grails.github.io/grails-doc/latest/guide/GORM.html"

    def pluginExcludes = [
            'grails-app/domain/**'
    ]

    Set<String> dataSourceNames = []

    def doWithSpring = {
        ConstrainedProperty.registerNewConstraint(UniqueConstraint.UNIQUE_CONSTRAINT,
                new PersistentConstraintFactory(springConfig.getUnrefreshedApplicationContext(),
                        UniqueConstraint))

        def domainClasses = application.getArtefacts(DomainClassArtefactHandler.TYPE)
                            .findAll() { GrailsDomainClass cls -> cls.mappingStrategy != "none" && cls.mappingStrategy == GrailsDomainClass.GORM}
                            .collect() { GrailsClass cls -> cls.clazz }
        def initializer = new HibernateDatastoreSpringInitializer(application.config, domainClasses)
        initializer.registerApplicationIfNotPresent = false
        initializer.grailsPlugin = true
        initializer.enableReload = grails.util.Environment.isDevelopmentMode()
        dataSourceNames.addAll( initializer.dataSources )
        def definitions = initializer.getBeanDefinitions((BeanDefinitionRegistry) springConfig.getUnrefreshedApplicationContext())
        definitions.delegate = delegate
        definitions.call()

        def currentSpringConfig = getSpringConfig()
        if (manager?.hasGrailsPlugin("controllers")) {
            hibernateOpenSessionInViewInterceptor(GrailsOpenSessionInViewInterceptor) {
                sessionFactory = ref("sessionFactory")
            }
            if (currentSpringConfig.containsBean("controllerHandlerMappings")) {
                controllerHandlerMappings.interceptors << ref("hibernateOpenSessionInViewInterceptor")
            }
            if (currentSpringConfig.containsBean("annotationHandlerMapping")) {
                if (annotationHandlerMapping.interceptors) {
                    annotationHandlerMapping.interceptors << ref("hibernateOpenSessionInViewInterceptor")
                }
                else {
                    annotationHandlerMapping.interceptors = [ref("hibernateOpenSessionInViewInterceptor")]
                }
            }
        }
    }

    def onShutdown = { event ->
        ConstrainedProperty.removeConstraint(UniqueConstraint.UNIQUE_CONSTRAINT, UniqueConstraint)
    }



}
