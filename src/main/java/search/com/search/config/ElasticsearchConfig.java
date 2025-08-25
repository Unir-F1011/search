package search.com.search.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

// import javax.annotation.PostConstruct; // Comentado temporalmente
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import search.com.search.model.entities.Items;

@Configuration
@EnableElasticsearchRepositories(basePackages = "search.com.search.repository") // ← CORREGIDO
public class ElasticsearchConfig {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchConfig.class);

    @Value("${elasticsearch.host}")
    private String clusterEndpoint;

    @Value("${elasticsearch.credentials.user}")
    private String username;

    @Value("${elasticsearch.credentials.password}")
    private String password;

    private ElasticsearchOperations elasticsearchOperations;

    @Bean
    public ElasticsearchOperations elasticsearchTemplate() {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));

        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost(clusterEndpoint, 443, "https"))
                        .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                            @Override
                            public HttpAsyncClientBuilder customizeHttpClient(
                                    HttpAsyncClientBuilder httpClientBuilder) {
                                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                            }
                        }));

        this.elasticsearchOperations = new ElasticsearchRestTemplate(client);
        return this.elasticsearchOperations;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initializeElasticsearch() {
        try {
            // Dar tiempo para que el bean se inicialice
            Thread.sleep(2000);

            if (elasticsearchOperations != null) {
                createIndexWithMapping();
            } else {
                log.warn("ElasticsearchOperations not initialized yet, skipping index creation");
            }
        } catch (Exception e) {
            log.error("Error initializing Elasticsearch", e);
        }
    }

    private void createIndexWithMapping() {
        try {
            boolean indexExists = elasticsearchOperations.indexOps(Items.class).exists();

            if (!indexExists) {
                log.info("Items index does not exist. Creating...");

                // Crear índice
                boolean indexCreated = elasticsearchOperations.indexOps(Items.class).create();

                if (indexCreated) {
                    log.info("✅ Items index created successfully");

                    // Crear mapping basado en las anotaciones de la entidad
                    boolean mappingCreated = elasticsearchOperations.indexOps(Items.class).putMapping();

                    if (mappingCreated) {
                        log.info("✅ Items mapping created successfully");
                        logCurrentMapping();
                    } else {
                        log.error("❌ Failed to create Items mapping");
                    }
                } else {
                    log.error("❌ Failed to create Items index");
                }
            } else {
                log.info("Items index already exists");
                verifyMapping();
            }
        } catch (Exception e) {
            log.error("Error creating index and mapping", e);
        }
    }

    private void verifyMapping() {
        try {
            log.info("Verifying existing mapping...");
            logCurrentMapping();
        } catch (Exception e) {
            log.error("Error verifying mapping", e);
        }
    }

    private void logCurrentMapping() {
        try {
            var mapping = elasticsearchOperations.indexOps(Items.class).getMapping();
            log.info("Current Items mapping: {}", mapping);

            // Log específico de campos importantes
            if (mapping.containsKey("properties")) {
                var properties = (java.util.Map<String, Object>) mapping.get("properties");
                log.info("Field mappings:");
                properties.forEach((field, config) ->
                        log.info("  - {}: {}", field, config));
            }
        } catch (Exception e) {
            log.error("Error retrieving mapping", e);
        }
    }
}