/*
 * See https://github.com/johntron/superagent-no-cache
 */
import ie from "component-ie";

function with_query_strings (request) {
  const timestamp = Date.now().toString();
  if (request._query !== undefined && request._query[0]) {
    request._query[0] += "&" + timestamp;
  } else {
    request._query = [timestamp];
  }

  return request;
}

export default function _superagentNoCache(request, mockIE) {
  request.set("X-Requested-With", "XMLHttpRequest");
  request.set("Expires", "-1");
  request.set("Cache-Control", "no-cache,no-store,must-revalidate,max-age=-1,private");

  if (ie || mockIE) {
    with_query_strings(request);
  }

  return request;
}
