package com.example.mbtilesServ.Config;

import com.example.mbtilesServ.Model.Tile;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Objects;

@Configuration
@EnableJpaRepositories(
        basePackages = {"com.example.mbtilesServ.Repository.Raster"},
        entityManagerFactoryRef = "rasterEntityManagerFactory",
        transactionManagerRef = "rasterTransactionManager"
)
@EnableTransactionManagement
public class RasterTilesDataSourceConfig {

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.raster")
    public DataSourceProperties rasterDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.raster.configuration")
    public DataSource rasterDataSource() {
        return rasterDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Primary
    @Bean(name = "rasterEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean rasterEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            DataSource dataSource)
    {
        return builder
                .dataSource(rasterDataSource())
                .packages(Tile.class)
                .build();
    }

    @Primary
    @Bean
    public PlatformTransactionManager rasterTransactionManager(
            @Qualifier("rasterEntityManagerFactory")
            LocalContainerEntityManagerFactoryBean rasterEntityManagerFactory
    ) {
        return new JpaTransactionManager(
                Objects.requireNonNull(
                        rasterEntityManagerFactory.getObject()
                )
        );
    }
}
