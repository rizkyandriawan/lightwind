package dev.kakrizky.lightwind.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.jboss.logging.Logger;

/**
 * CDI producer for Elasticsearch {@link RestClient} and {@link ElasticsearchClient}.
 *
 * <p>Clients are lazily initialized on first access and configured
 * from {@link SearchConfig}.</p>
 */
@ApplicationScoped
public class ElasticsearchClientProducer {

    private static final Logger LOG = Logger.getLogger(ElasticsearchClientProducer.class);

    private final SearchConfig config;

    private volatile RestClient restClient;
    private volatile ElasticsearchClient esClient;

    @Inject
    public ElasticsearchClientProducer(SearchConfig config) {
        this.config = config;
    }

    /**
     * Produces a low-level Elasticsearch {@link RestClient}.
     */
    @Produces
    @ApplicationScoped
    public RestClient restClient() {
        if (restClient == null) {
            synchronized (this) {
                if (restClient == null) {
                    restClient = createRestClient();
                }
            }
        }
        return restClient;
    }

    /**
     * Produces a high-level Elasticsearch {@link ElasticsearchClient}.
     */
    @Produces
    @ApplicationScoped
    public ElasticsearchClient elasticsearchClient() {
        if (esClient == null) {
            synchronized (this) {
                if (esClient == null) {
                    RestClientTransport transport = new RestClientTransport(
                            restClient(), new JacksonJsonpMapper());
                    esClient = new ElasticsearchClient(transport);
                }
            }
        }
        return esClient;
    }

    private RestClient createRestClient() {
        HttpHost[] hosts = config.hosts().stream()
                .map(this::parseHost)
                .toArray(HttpHost[]::new);

        RestClientBuilder builder = RestClient.builder(hosts);

        if (config.username().isPresent() && config.password().isPresent()) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(
                            config.username().get(),
                            config.password().get()));
            builder.setHttpClientConfigCallback(
                    httpClientBuilder -> httpClientBuilder
                            .setDefaultCredentialsProvider(credentialsProvider));
        }

        LOG.infov("Elasticsearch client configured with hosts: {0}", config.hosts());
        return builder.build();
    }

    private HttpHost parseHost(String hostString) {
        String[] parts = hostString.split(":");
        String hostname = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9200;
        return new HttpHost(hostname, port, "http");
    }
}
