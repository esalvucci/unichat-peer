/**
 * Web-Chat
 * - The Web-Chat application consists of an HTTP service functioning a central hub for multiple clients chatting by means of __chat rooms__  - Clients can register themselves using unique usernames, addresses, and passwords     + Upon registration, they are endowed with an [Universally unique identifier](https://it.wikipedia.org/wiki/Universally_unique_identifier) (UUID)     + Clients can either be `admin`s or `user`s     + Unlogged clients can chat too, but they have no explicit identity or role     + A logged client can be referenced by means of its username, its address, or its UUID  - Clients can create chat rooms -- thus becoming their __owners__ --, whereas other clients may __join__ -- thus becoming __members__ --, or leave such chat rooms     + Chat rooms can be referenced by means of their name, which is assumed to be unique     + The members of a chat room can see the whole sequence of messages published within that chat room or just a sub-sequence     + The members of a chat room can see the whole set of members of the chat room  - Owners can create their chat rooms with three different access levels:     + __public__ chat rooms can be read and written by anyone, there including unlogged users: membership is unimportant here     + __open__ chat rooms can be read and written only by their members, but any logged user can join them     + conversely, in __private__ chat rooms only the owner can assign memberships  - `admin`s can inspect and manage the list of registered users     - a registered `user` can be removed either by an `admin` or by him/her self  - The list of currently existing chat rooms is publicly available  - Here we consider a _very trivial and **insecure**_ authentication and authorization schema:     * upon registration, clients are assumed to provide an identifier and a password (i.e., their __credentials__), which are stored on the server side         + the server should prevent the same identifier from being registered twice         + clients are assumed to be registered as `user`s         + `admin`s are __hardcoded__ into the provided implementation stub      * when performing an HTTP request, clients are assumed to provide their credentials as a __JSON string__ contained within the __`Authorization`__ header of the request.     The JSON string, should have one of the following forms:         + `{ `__`\"id\"`__`: \"uuid here\", `__`\"password\"`__`: \"password here\" }`         + or `{ `__`\"username\"`__`: \"username here\", `__`\"password\"`__`: \"password here\" }`         + or `{ `__`\"email\"`__`: \"email here\", `__`\"password\"`__`: \"password here\" }`      * when performing an HTTP request, clients are considered __authenticated__ if they credentials match the ones which are stored on the server side      * when performing an HTTP request, clients are considered __authorized__ if they are authenticated _and_ they their role is enabled to perform the requested operation 
 *
 * OpenAPI spec version: 1.0.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */
package io.swagger.client.core

sealed trait ResponseState

object ResponseState {

  case object Success extends ResponseState

  case object Error extends ResponseState

}

case class ApiRequest[U](
  // required fields
  method: ApiMethod,
  basePath: String,
  operationPath: String,
  contentType: String,

  // optional fields
  responses: Map[Int, (Manifest[_], ResponseState)] = Map.empty,
  bodyParam: Option[Any] = None,
  formParams: Map[String, Any] = Map.empty,
  pathParams: Map[String, Any] = Map.empty,
  queryParams: Map[String, Any] = Map.empty,
  headerParams: Map[String, Any] = Map.empty,
  credentials: Seq[Credentials] = List.empty) {

  def withCredentials(cred: Credentials): ApiRequest[U] = copy[U](credentials = credentials :+ cred)

  def withApiKey(key: ApiKeyValue, keyName: String, location: ApiKeyLocation): ApiRequest[U] = withCredentials(ApiKeyCredentials(key, keyName, location))

  def withSuccessResponse[T](code: Int)(implicit m: Manifest[T]): ApiRequest[U] = copy[U](responses = responses + (code -> (m, ResponseState.Success)))

  def withErrorResponse[T](code: Int)(implicit m: Manifest[T]): ApiRequest[U] = copy[U](responses = responses + (code -> (m, ResponseState.Error)))

  def withDefaultSuccessResponse[T](implicit m: Manifest[T]): ApiRequest[U] = withSuccessResponse[T](0)

  def withDefaultErrorResponse[T](implicit m: Manifest[T]): ApiRequest[U] = withErrorResponse[T](0)

  def responseForCode(statusCode: Int): Option[(Manifest[_], ResponseState)] = responses.get(statusCode) orElse responses.get(0)

  def withoutBody(): ApiRequest[U] = copy[U](bodyParam = None)

  def withBody(body: Any): ApiRequest[U] = copy[U](bodyParam = Some(body))

  def withFormParam(name: String, value: Any): ApiRequest[U] = copy[U](formParams = formParams + (name -> value))

  def withPathParam(name: String, value: Any): ApiRequest[U] = copy[U](pathParams = pathParams + (name -> value))

  def withQueryParam(name: String, value: Any): ApiRequest[U] = copy[U](queryParams = queryParams + (name -> value))

  def withHeaderParam(name: String, value: Any): ApiRequest[U] = copy[U](headerParams = headerParams + (name -> value))
}