package il.co.rotsh.graphql

data class GraphQLRequest(val query: String, val variables: Map<String, Any>?=null, val operationName: String? = null, val ctx:Any?= null)