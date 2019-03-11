package il.co.rotsh.graphql

data class GraphqlConfig(
    var baseUrl: String = "/graphql",
    var graphiql:Boolean = true
)