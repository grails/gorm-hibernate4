/*
 * Copyright 2015 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugin.hibernate.commands

import grails.dev.commands.ApplicationCommand
import grails.dev.commands.ExecutionContext
import grails.util.Environment
import groovy.transform.CompileStatic
import org.grails.build.parsing.CommandLine
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.orm.hibernate.HibernateDatastore
import org.grails.orm.hibernate.HibernateMappingContextSessionFactoryBean
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.hibernate.metamodel.MetadataBuilder
import org.hibernate.metamodel.MetadataSources
import org.hibernate.metamodel.source.MetadataImplementor
import org.hibernate.service.ServiceRegistry
import org.hibernate.tool.hbm2ddl.SchemaExport as HibernateSchemaExport

/**
 * Adds a schema-export command
 *
 * @author Graeme Rocher
 * @since 4.0
 */
@CompileStatic
class SchemaExportCommand implements ApplicationCommand {

    final String description = "Creates a DDL file of the database schema"
    Boolean skipBootstrap = true

    @Override
    boolean handle(ExecutionContext executionContext) {
        CommandLine commandLine = executionContext.commandLine

        String filename = "${executionContext.targetDir}/ddl.sql"
        boolean export = false
        boolean stdout = false

        for (arg in commandLine.remainingArgs) {
            switch(arg) {
                case 'export':   export = true; break
                case 'generate': export = false; break
                case 'stdout':   stdout = true; break
                default:         filename = arg
            }
        }

        def argsMap = commandLine.undeclaredOptions
        String dataSourceName = argsMap.datasource ? argsMap.datasource : ConnectionSource.DEFAULT

        def file = new File(filename)
        file.parentFile.mkdirs()

        HibernateDatastore hibernateDatastore = applicationContext.getBean("hibernateDatastore", HibernateDatastore)
        hibernateDatastore = hibernateDatastore.getDatastoreForConnection(dataSourceName)

        def serviceRegistry = ((SessionFactoryImplementor)hibernateDatastore.sessionFactory).getServiceRegistry()
                                                                                            .getParentServiceRegistry()
        MetadataImplementor metadataImplementor = buildMetadata(serviceRegistry)
        def schemaExport = new HibernateSchemaExport(metadataImplementor)
                .setHaltOnError(true)
                .setOutputFile(file.path)
                .setDelimiter(';')

        String action = export ? "Exporting" : "Generating script to ${file.path}"
        String ds = argsMap.datasource ? "for DataSource '$argsMap.datasource'" : "for the default DataSource"
        println "$action in environment '${Environment.current.name}' $ds"

        schemaExport.execute stdout, export, false, false

        if (schemaExport.exceptions) {
            def e = (Exception)schemaExport.exceptions[0]
            e.printStackTrace()
            return false
        }
        return true
    }

    protected MetadataImplementor buildMetadata(ServiceRegistry serviceRegistry) throws Exception {
        final MetadataSources metadataSources = new MetadataSources( serviceRegistry );
        final MetadataBuilder metadataBuilder = metadataSources.getMetadataBuilder();
        return (MetadataImplementor) metadataBuilder.build();
    }
}
