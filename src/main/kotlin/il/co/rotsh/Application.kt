package il.co.rotsh

import il.co.rotsh.graphql.graphql
import graphql.schema.DataFetcher
import graphql.schema.StaticDataFetcher
import il.co.rotsh.graphql.GraphQLHandler
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.features.*
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.withCharset
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.Routing

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
//@kotlin.jvm.JvmOverloads
fun Application.main(){
    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        jackson {
        }
    }
    install(AutoHeadResponse)
    install(StatusPages) {
        status(HttpStatusCode.NotFound) {
            call.respond(TextContent("${it.value} ${it.description}", ContentType.Text.Plain.withCharset(Charsets.UTF_8), it))
        }
    }
    log.info("Application setup complete")
    val schema ="""type Query{
        |answer: Int
        |hello(what:String="world"): String
        |}""".trimMargin()

    val fetchers = mapOf("Query" to listOf(
        "hello" to DataFetcher {env -> "Hello " + env.getArgument("what")},
        "answer" to StaticDataFetcher(42)
    ))

    val handler = GraphQLHandler(schema, fetchers)
    log.info("GraphQL Schema setup complete")
    install (Routing){
       graphql(handler)
    }
}