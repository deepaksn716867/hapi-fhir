package ca.uhn.fhir.jpa.demo;

import java.sql.SQLException;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import ca.uhn.fhir.jpa.config.BaseJavaConfigDstu2;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.demo.dao.DAOFactory;
import ca.uhn.fhir.jpa.util.SubscriptionsRequireManualActivationInterceptorDstu2;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;
import org.mitre.openid.connect.client.service.impl.StaticServerConfigurationService;
import org.mitre.openid.connect.client.service.impl.StaticClientConfigurationService;

import org.mitre.openid.connect.config.ServerConfiguration;
import org.mitre.oauth2.model.RegisteredClient;

import ca.uhn.fhir.rest.server.security.OpenIdConnectBearerTokenServerInterceptor;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import java.sql.SQLException;
import java.util.HashMap;

@Configuration
@EnableTransactionManagement()
//@Import(WebsocketDstu2Config.class)
public class FhirServerConfig extends BaseJavaConfigDstu2 {

	//The members to access the properties file.
	Properties props = null;

	public FhirServerConfig()
	{
		props = DAOFactory.getDAOProperties();
	}

	/**
	 * Configure FHIR properties around the the JPA server via this bean
	 */
	@Bean()
	public DaoConfig daoConfig() {
		DaoConfig retVal = new DaoConfig();
		retVal.setSubscriptionEnabled(true);
		retVal.setSubscriptionPollDelay(5000);
		retVal.setSubscriptionPurgeInactiveAfterMillis(DateUtils.MILLIS_PER_HOUR);
		retVal.setAllowMultipleDelete(true);
		return retVal;
	}

	/**
	 * @throws SQLException
	 * @throws DAOException
	 * The following bean configures the database connection. The 'url' property value of "jdbc:derby:directory:jpaserver_derby_files;create=true" indicates that the server should save resources in a
	 * directory called "jpaserver_derby_files".
	 *
	 * A URL to a remote database could also be placed here, along with login credentials and other properties supported by BasicDataSource.
	 * @throws
	 */
	@Bean(destroyMethod = "close")
	public DataSource dataSource() throws SQLException {
		BasicDataSource retVal = new BasicDataSource();

		try
		{
			retVal.setDriver(new com.mysql.jdbc.Driver());
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw e;
		}

		retVal.setUrl(props.getProperty("jdbc.url"));
		retVal.setUsername(props.getProperty("jdbc.user"));
		retVal.setPassword(props.getProperty("jdbc.passwd"));
		return retVal;
	}

	@Bean()
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() throws SQLException {
		LocalContainerEntityManagerFactoryBean retVal = new LocalContainerEntityManagerFactoryBean();
		retVal.setPersistenceUnitName("HAPI_PU");
		retVal.setDataSource(dataSource());
		retVal.setPackagesToScan("ca.uhn.fhir.jpa.entity");
		retVal.setPersistenceProvider(new HibernatePersistenceProvider());
		retVal.setJpaProperties(jpaProperties());
		return retVal;
	}

	private Properties jpaProperties() {
		Properties extraProperties = new Properties();
		//extraProperties.put("hibernate.dialect", org.hibernate.dialect.DerbyTenSevenDialect.class.getName());
		extraProperties.put("hibernate.dialect",org.hibernate.dialect.MySQL5Dialect.class.getName());
		extraProperties.put("hibernate.format_sql", "true");
		extraProperties.put("hibernate.show_sql", "false");
		extraProperties.put("hibernate.hbm2ddl.auto", "update");
		extraProperties.put("hibernate.jdbc.batch_size", "20");
		extraProperties.put("hibernate.cache.use_query_cache", "false");
		extraProperties.put("hibernate.cache.use_second_level_cache", "false");
		extraProperties.put("hibernate.cache.use_structured_entries", "false");
		extraProperties.put("hibernate.cache.use_minimal_puts", "false");
		extraProperties.put("hibernate.search.default.directory_provider", "filesystem");
		extraProperties.put("hibernate.search.default.indexBase", "target/lucenefiles");
		extraProperties.put("hibernate.search.lucene_version", "LUCENE_CURRENT");
		return extraProperties;
	}

	/**
	 * Do some fancy logging to create a nice access log that has details about each incoming request.
	 */
	public IServerInterceptor loggingInterceptor() {
		LoggingInterceptor retVal = new LoggingInterceptor();
		retVal.setLoggerName("fhirtest.access");
		retVal.setMessageFormat(
				"Path[${servletPath}] Source[${requestHeader.x-forwarded-for}] Operation[${operationType} ${operationName} ${idOrResourceName}] UA[${requestHeader.user-agent}] Params[${requestParameters}] ResponseEncoding[${responseEncodingNoDefault}]");
		retVal.setLogExceptions(true);
		retVal.setErrorMessageFormat("ERROR - ${requestVerb} ${requestUrl}");
		return retVal;
	}
	@Bean(autowire = Autowire.BY_TYPE)
	public IServerInterceptor myCorsInterceptor() {
			CorsInterceptor retVal = new CorsInterceptor();
			return retVal;
	}

	@Bean(autowire = Autowire.BY_TYPE)
	public StaticServerConfigurationService srv() {
			StaticServerConfigurationService sr = new StaticServerConfigurationService();
			sr.setServers(new HashMap<String, ServerConfiguration>());
			ServerConfiguration srvCfg = new ServerConfiguration();
			srvCfg.setIssuer("http://id.healthcreek.org/");
			srvCfg.setJwksUri("http://id.healthcreek.org/jwk");
			srvCfg.setTokenEndpointUri("http://id.healthcreek.org/token");
			srvCfg.setAuthorizationEndpointUri("http://id.healthcreek.org/authorize");

			sr.getServers().put("http://id.healthcreek.org/", srvCfg);
			sr.afterPropertiesSet();

			return sr;
	}
	@Bean()
	public StaticClientConfigurationService cli() {
			StaticClientConfigurationService cl = new StaticClientConfigurationService();
			cl.setClients(new HashMap<String, RegisteredClient>());
			cl.getClients().put("http://id.healthcreek.org/", new RegisteredClient());
			return cl;
	}

	@Bean(autowire = Autowire.BY_TYPE)
	public IServerInterceptor securityInterceptor() {
			return new OpenIdConnectBearerTokenServerInterceptor();
	}

	/**
	 * This interceptor adds some pretty syntax highlighting in responses when a browser is detected
	 */
	@Bean(autowire = Autowire.BY_TYPE)
	public IServerInterceptor responseHighlighterInterceptor() {
		ResponseHighlighterInterceptor retVal = new ResponseHighlighterInterceptor();
		return retVal;
	}

	@Bean(autowire = Autowire.BY_TYPE)
	public IServerInterceptor subscriptionSecurityInterceptor() {
		SubscriptionsRequireManualActivationInterceptorDstu2 retVal = new SubscriptionsRequireManualActivationInterceptorDstu2();
		return retVal;
	}

	@Bean()
	public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
		JpaTransactionManager retVal = new JpaTransactionManager();
		retVal.setEntityManagerFactory(entityManagerFactory);
		return retVal;
	}

}
