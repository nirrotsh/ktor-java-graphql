package il.co.rotsh.graphql

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.default
import io.ktor.http.content.file
import io.ktor.http.content.static
import io.ktor.request.ApplicationRequest
import io.ktor.request.contentType
import io.ktor.request.receive
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.*
import org.slf4j.LoggerFactory

fun Routing.graphql(handler: GraphQLHandler, config: GraphqlConfig = GraphqlConfig()){
    getGraphQL(handler, config.baseUrl)
    postGraphQl(handler, config.baseUrl)
    if (config.graphiql){
    static("/"){
        file("index.html")
        default("index.html")
        static("graphiql") {
            file("index.html")
            default("index.html")
            }
        }
    }
    LoggerFactory.getLogger(this.javaClass).info("GraphQL is listening for ${config.baseUrl}")
}

private fun Routing.getGraphQL(handler:GraphQLHandler, baseUrl:String){
    get(baseUrl){
        try {
            val gqlReq = extractFromQueryParams(call.request)
            handleGraphQlRequest(handler, gqlReq, call)
        }
        catch (ex:Exception){
            call.application.environment.log.error(
                "Illegal GraphQL HTTP/GET request ${call.request.queryParameters}",
                ex
            )
            call.respond(
                HttpStatusCode.BadRequest,
                "Invalid GraphQL HTTP/GET request. See https://graphql.org/learn/serving-over-http/"
            )
        }
    }
}

private fun Routing.postGraphQl(handler:GraphQLHandler, baseUrl:String){
    post(baseUrl){
        if (call.request.queryParameters.contains("query")) {
            //use query from query params
            val query = call.request.queryParameters["query"]?:""
            handleGraphQlRequest(handler, GraphQLRequest(query), call)
        } else if (call.request.contentType()== ContentType.parse("application/graphql")){
            // If the "application/graphql" Content-Type header is present,
            // treat the HTTP POST body contents as the GraphQL query string
            val query = call.receiveText()
            handleGraphQlRequest(handler, GraphQLRequest(query), call)
        } else {
            //Assume body is a JSON containing required fields
            val gqlRequest = call.receive<GraphQLRequest>()
            handleGraphQlRequest(handler, gqlRequest, call)
        }
    }
}

private fun extractFromQueryParams(request: ApplicationRequest):GraphQLRequest{
    val jsonMapper = ObjectMapper()
    val queryParameters = request.queryParameters
    val query = queryParameters["query"]?:""
    val variablesJson = queryParameters["variables"]
    val operationName = queryParameters["operationName"]
    var variables = mapOf<String,Any>()
    if (variablesJson != null){
        variables = jsonMapper.readValue(variablesJson, variables::class.java)
    }
    return GraphQLRequest(query, variables, operationName,null)
}

private suspend fun handleGraphQlRequest(handler:GraphQLHandler, gqlReq: GraphQLRequest, call: ApplicationCall){
    val result = handler.execute(gqlReq)
    call.respond(mapOf("data" to result.getData<Any>()))
}