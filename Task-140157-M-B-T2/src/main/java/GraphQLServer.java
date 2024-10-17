import graphql.GraphQL;
import graphql.ExecutionResult;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class GraphQLServer {
    public static void main(String[] args) {
        // Step 1: Define the GraphQL schema
        String schema = "type Query { hello(name: String): String }";

        // Step 2: Build the GraphQL schema
        GraphQLSchema graphQLSchema = buildSchema(schema);

        // Step 3: Create a DataLoader for the "hello" field
        BatchLoader<String, String> batchLoader = keys -> CompletableFuture.supplyAsync(() ->
                keys.stream().map(key -> "Hello, " + key + "!").collect(Collectors.toList())
        );

        DataLoader<String, String> helloLoader = DataLoader.newDataLoader(batchLoader);

        // Step 4: Create the DataLoader registry and bind the loader
        DataLoaderRegistry registry = new DataLoaderRegistry();
        registry.register("helloLoader", helloLoader);

        // Step 5: Create a GraphQL instance with the schema
        GraphQL graphQL = GraphQL.newGraphQL(graphQLSchema).build();

        // Execute GraphQL queries using the configured DataLoader
        String query = "query { hello(name: \"Alice\") }";
        ExecutionResult result = graphQL.execute(query);
        //System.out.println(result.getData());
    }

    private static GraphQLSchema buildSchema(String schema) {
        // Step 1: Parse the schema
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(schema);

        // Step 2: Create runtime wiring with the DataFetcher
        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", typeWiring -> typeWiring
                        .dataFetcher("hello", dataFetchingEnvironment -> {
                            String name = dataFetchingEnvironment.getArgument("name");
                            return "Hello, " + name + "!";
                        }))
                .build();

        // Step 3: Build the GraphQL schema using the typeRegistry and runtimeWiring
        return new SchemaGenerator().makeExecutableSchema(typeRegistry, runtimeWiring);
    }
}
