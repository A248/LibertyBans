/* 
 * ArimBans, a punishment plugin for minecraft servers
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.api.util;

// possible todo: Add more from https://httpstatuses.com
public enum HttpStatus {
	
	CONTINUE(100, "Continue", "The client should continue with its request."),
	SWITCHING_PROTOCOLS(101, "Switching Protocols", "Informs the client that the server will switch to the protocol specified in the Upgrade message header field."),
	UNASSIGNED_104(104, "Unassigned 104", "Unassigned 104"),
	
	OK(200, "OK", "The request sent by the client was successful."),
	CREATED(201, "Created", "The request was successful and the resource has been created."),
	ACCEPTED(202, "Accepted", "The request has been accepted but has not yet finished processing."),
	NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information", "The returned meta-information in the entity header is not the definitative set of information, it might be a local copy or contain local alterations."),
	NO_CONTENT(204, "No Content", "The request was successful but not require the return of an entity body."),
	RESET_CONTENT(205, "Reset Content", "The request was successful and the user agent should reset the view that sent the request."),
	PARTIAL_CONTENT(206, "Partial Content", "The partial request was successful."),
	MULTI_STATUS(207, "Multi-Status", "A Multi-Status response conveys information about multiple resources in situations where multiple status codes might be appropriate"),
	ALREADY_REPORTED(208, "Already Reported", "Used inside a DAV: propstat response element to avoid enumerating the internal members of multiple bindings to the same collection repeatedly"),
	IM_USED(226, "I'm Used", "The server has fulfilled a GET request for the resource, and the response is a representation of the result of one or more instance-manipulations applied to the current instance."),
	
	MULTIPLE_CHOICES(300, "Multiple Choices", "The requested resource has multiple choices, each of which has a different location."),
	MOVED_PERMANENTLY(301, "Moved Permanently", "The requested resources has moved permanently to a new location."),
	FOUND(302, "Found", "The requested resource has been found at a different location but the client should use the original URI."),
	SEE_OTHER(303, "See Other", "The requested resource is located at a different location which should be returned by the location field in the response."),
	NOT_MODIFIED(304, "Not Modified", "The resource has not been modified since the last request."),
	USE_PROXY(305, "Use Proxy", "The requested resource can only be accessed through a proxy which should be provided in the location field."),
	UNUSED(306, "Unused", "This status code is no longer in use but is reserved for future use."),
	TEMPORARY_REDIRECT(307, "Temporary Redirect", "The requested resource is temporarily moved to the provided location but the client should continue to use this location as the resource may again move."),
	PERMANENT_REDIRECT(308, "Permanent Redirect", "The target resource has been assigned a new permanent URI and any future references to this resource ought to use one of the enclosed URIs."),
	
	BAD_REQUEST(400, "Bad Request", "The request could not be understood by the server."),
	UNAUTHORIZED(401, "Unauthorized", "The request requires authorization."),
	PAYMENT_REQUIRED(402, "Payment Required", "Reserved for future use."),
	FORBIDDEN(403, "Forbidden", "Whilst the server did understand the request, the server is refusing to complete it. This is not an authorization problem."),
	NOT_FOUND(404, "Not Found", "The requested resource was not found."),
	METHOD_NOT_ALLOWED(405, "Method Not Allowed", "The supplied method was not allowed on the given resource."),
	NOT_ACCEPTABLE(406, "Not Acceptable", "The resource is not able to return a response that is suitable for the characteristics required by the accept headers of the request."),
	PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required", "The client must authenticate themselves with the proxy."),
	REQUEST_TIMEOUT(408, "Request Timeout", "The client did not supply a request in the period required by the server."),
	CONFLICT(409, "Conflict", "The request could not be completed as the resource is in a conflicted state."),
	GONE(410, "Gone", "The requested resource is no longer available on the server and no redirect address is available."),
	LENGTH_REQUIRED(411, "Length Required", "The server will not accept the request without a Content-Length field."),
	PRECONDITION_FAILED(412, "Precondition Failed", "The supplied precondition evaluated to false on the server."),
	REQUEST_ENTITY_TOO_LARGE(413, "Request Entity Too Large", "The request was unsuccessful because the request entity was larger than the server would allow"),
	REQUEST_URI_TOO_LONG(414, "Request URI Too Long", "The request was unsuccessful because the requested URI is longer than the server is willing to process (that's what she said)."),
	UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type", "The request was unsuccessful because the request was for an unsupported format."),
	REQUEST_RANGE_NOT_SATISFIABLE(416, "Request Range Not Satisfiable", "The range of the resource does not overlap with the values specified in the requests Range header field and not alternative If-Range field was supplied."),
	EXPECTATION_FAILED(417, "Expectation Failed", "The expectation supplied in the Expectation header field could not be met by the server."),
	I_AM_A_TEAPOT(418, "I'm a teapot", "I'm a teapot"),
	
	TOO_MANY_REQUESTS(429, "Too Many Requests", "The user has sent too many requests in a given amount of time (\"rate limiting\")"),
	REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header Fields Too Large", "The server is unwilling to process the request because its header fields are too large. The request MAY be resubmitted after reducing the size of the request header fields"),
	CONNECTION_CLOSED_WITHOUT_RESPONSE(444, "Connection Closed Without Response", "A non-standard status code used to instruct nginx to close the connection without sending a response to the client, most commonly used to deny malicious or malformed requests."),
	UNAVAILABLE_FOR_LEGAL_REASONS(451, "Unavailable For Legal Reasons", "The server is denying access to the resource as a consequence of a legal demand."),
	CLIENT_CLOSED_REQUEST(499, "Client Closed Request", "A non-standard status code introduced by nginx for the case when a client closes the connection while nginx is processing the request"),
	
	INTERNAL_SERVER_ERROR(500, "Internal Server Error", "The request was unsuccessful because the server encountered an unexpected error."),
	NOT_IMPLEMENTED(501, "Not Implemented", "The server does not support the request."),
	BAD_GATEWAY(502, "Bad Gateway", "The server, whilst acting as a proxy, received an invalid response from the server that was fulfilling the request."),
	SERVICE_UNAVAILABLE(503, "Service Unavailable", "The request was unsuccessful as the server is either down or slash^H^H^H^H^Hdug^H^H^Hreddited."),
	GATEWAY_TIMEOUT(504, "Gateway Timeout", "The server, whilst acting as a proxy, did not receive a response from the upstream server in an acceptable time."),
	HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version Not Supported", "The server does not supported the HTTP protocol version specified in the request"),
	VARIANT_ALSO_NEGOTIATES(506, "Variant Also Negotiates", "The server has an internal configuration error: the chosen variant resource is configured to engage in transparent content negotiation itself, and is therefore not a proper end point in the negotiation process."),
	INSUFFICIENT_STORAGE(507, "Insufficient Storage", "The method could not be performed on the resource because the server is unable to store the representation needed to successfully complete the request."),
	LOOP_DETECTED(508, "Loop Detected", "The server terminated an operation because it encountered an infinite loop while processing a request with \"Depth: infinity\". This status indicates that the entire operation failed."),
	NOT_EXTENDED(510, "Not Extended", "The policy for accessing the resource has not been met in the request. The server should send back all the information necessary for the client to issue an extended request."),
	NETWORK_AUTHENTICATION_REQUIRED(511, "Network Authentication Required", "The client needs to authenticate to gain network access."),
	NETWORK_CONNECT_TIMEOUT_ERROR(599, "Network Connect Timeout Error", "This status code is not specified in any RFCs, but is used by some HTTP proxies to signal a network connect timeout behind the proxy to a client in front of the proxy."),
	
	UNKNOWN(400, "Unknown HTTP Status Code", "Unknown or unsupported HTTP status code");
	
	private final int code;
	private final String name;
	private final String description;

	private HttpStatus(int code, String name, String description) {
		this.code = code;
		this.name = name;
		this.description = description;
	}
	
	public static HttpStatus fromCode(final int code) {
		for (HttpStatus status : HttpStatus.values()) {
			if (status.code == code) {
				return status;
			}
		}
		return UNKNOWN;
	}
	
	public int getCode() {
		return code;
	}
	
	public String getName() {
		return name;
	}
	
	public String getDescrption() {
		return description;
	}
	
}
