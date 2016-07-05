package org.grails.orm.hibernate.connections;

import org.grails.datastore.mapping.core.connections.ConnectionSource;
import org.grails.datastore.mapping.core.exceptions.ConfigurationException;
import org.grails.orm.hibernate.cfg.HibernateMappingContext;
import org.grails.orm.hibernate.cfg.HibernateMappingContextConfiguration;
import org.grails.orm.hibernate.jdbc.connections.DataSourceSettings;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.sql.DataSource;

/**
 * Constructs {@link SessionFactory} instances from a {@link HibernateMappingContext}
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public class HibernateConnectionSourceFactory extends AbstractHibernateConnectionSourceFactory implements ApplicationContextAware {

    protected final HibernateMappingContext mappingContext;
    private ApplicationContext applicationContext;

    public HibernateConnectionSourceFactory(HibernateMappingContext mappingContext) {
        this.mappingContext = mappingContext;
    }

    @Override
    public ConnectionSource<SessionFactory, HibernateConnectionSourceSettings> create(String name, ConnectionSource<DataSource, DataSourceSettings> dataSourceConnectionSource, HibernateConnectionSourceSettings settings) {
        boolean isDefault = ConnectionSource.DEFAULT.equals(name);

        HibernateConnectionSourceSettings.HibernateSettings hibernateSettings = settings.getHibernate();
        Class<? extends Configuration> configClass = hibernateSettings.getConfigClass();

        HibernateMappingContextConfiguration configuration;
        if(configClass != null) {
            if( !HibernateMappingContextConfiguration.class.isAssignableFrom(configClass) ) {
                throw new ConfigurationException("The configClass setting must be a subclass for [HibernateMappingContextConfiguration]");
            }
            else {
                configuration = (HibernateMappingContextConfiguration) BeanUtils.instantiate(configClass);
            }
        }
        else {
            configuration = new HibernateMappingContextConfiguration();
        }

        configuration.setApplicationContext(this.applicationContext);
        configuration.setHibernateMappingContext(mappingContext);
        configuration.setDataSourceName(name);
        configuration.setSessionFactoryBeanName(isDefault ? "sessionFactory" : "sessionFactory_" + name);
        configuration.setProperties(settings.toProperties());
        SessionFactory sessionFactory = configuration.buildSessionFactory();
        return new HibernateConnectionSource(name, sessionFactory, dataSourceConnectionSource, settings);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
