/*
 * Copyright 2021 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.jdbc.mariadb.impl;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.mariadb.jdbc.MariaDbDataSource;
import org.osgi.service.jdbc.DataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MariaDbDataSourceFactoryTest {

    public static final Logger LOG = LoggerFactory.getLogger(MariaDbDataSourceFactoryTest.class);

    /*
        $ podman run --name pax.jdbc.mariadb -p 3306:3306 -e MYSQL_ROOT_PASSWORD=paxjdbc -d mariadb
     */

    @Rule
    public ExternalResource resource = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            try (Socket socket = new Socket()) {
                InetSocketAddress endpoint = new InetSocketAddress("localhost", 3306);
                socket.connect(endpoint, (int) TimeUnit.SECONDS.toMillis(5));
                Assume.assumeTrue("Maria DB should start and listen on port 3306", true);
            } catch (Exception ex) {
                Assume.assumeTrue("Maria DB should start and listen on port 3306", false);
            }
        }

        @Override
        protected void after() {
        }
    };

    @Test
    public void configuration() throws SQLException {
        Properties properties = new Properties();
        // osgi.jdbc specific property
        properties.setProperty(DataSourceFactory.JDBC_URL, "jdbc:mariadb://localhost:3306/mysql");
        properties.setProperty(DataSourceFactory.JDBC_USER, "root");
        properties.setProperty(DataSourceFactory.JDBC_PASSWORD, "paxjdbc");
        DataSource ds = new MariaDbDataSourceFactory().createDataSource(properties);
        MariaDbDataSource mds = (MariaDbDataSource) ds;
        try (Connection con = ds.getConnection()) {
            DatabaseMetaData md = con.getMetaData();
            LOG.info("DB: {}/{}", md.getDatabaseProductName(), md.getDatabaseProductVersion());

            try (Statement st = con.createStatement()) {
                try (ResultSet rs = st.executeQuery("select SCHEMA_NAME, CATALOG_NAME from INFORMATION_SCHEMA.SCHEMATA")) {
                    while (rs.next()) {
                        LOG.info("Schema: {}, catalog: {}", rs.getString(1), rs.getString(2));
                    }
                }
            }
        }
    }

}
