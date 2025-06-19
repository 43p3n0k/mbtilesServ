package com.example.mbtilesServ.Config;

import com.example.mbtilesServ.Model.Tile;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Objects;

@Configuration
@EnableJpaRepositories(
        basePackages = {"com.example.mbtilesServ.Repository.Vector"},
        entityManagerFactoryRef = "vectorEntityManagerFactory",
        transactionManagerRef = "vectorTransactionManager"
)
@EnableTransactionManagement
public class VectorTilesDataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.vector")
    public DataSourceProperties vectorDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.vector.configuration")
    public DataSource vectorDataSource() {
        return vectorDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "vectorEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean vectorEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            DataSource dataSource)
    {
        return builder
                .dataSource(vectorDataSource())
                .packages(Tile.class)
                .build();
    }

    @Bean
    public PlatformTransactionManager vectorTransactionManager(
            @Qualifier("vectorEntityManagerFactory")
            LocalContainerEntityManagerFactoryBean vectorEntityManagerFactory
    ) {
        return new JpaTransactionManager(
                Objects.requireNonNull(
                        vectorEntityManagerFactory.getObject()
                )
        );
    }
}
