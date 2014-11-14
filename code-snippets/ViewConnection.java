package com.couchbase.client;

/**
 * The {@link ViewConnection} is responsible for managing and multiplexing
 * HTTP connections to Couchbase View endpoints.
 *
 * It implements {@link Reconfigurable}, which means that will be fed with
 * reconfiguration updates coming from the server side. This stream changes
 * the collection of {@link HttpHost}s, which represent the view endpoints.
 */
public class ViewConnection extends SpyObject implements Reconfigurable {
..

 /**
   * Write an operation to the next {@link HttpHost}.
   *
   * To make sure that the operations are distributed throughout the cluster,
   * the {@link HttpHost} is changed every time a new operation is added. Since
   * the {@link #getNextNode()} method increments the {@link HttpHost} index and
   * calculates the modulo, the nodes are selected in a round-robin fashion.
   *
   * Also, the target host will be added directly, so that a DNS lookup is
   * avoided, potentially causing delays and timeouts.
   *
   * @param op the operation to schedule.
   */
  public void addOp(final HttpOperation op) {
    if (!running) {
      throw new IllegalStateException("Shutting down");
    }

    HttpCoreContext coreContext = HttpCoreContext.create();

    if (viewNodes.isEmpty()) {
      getLogger().error("No server connections. Cancelling op.");
      op.cancel();
    } else {
      if (!"default".equals(user)) {
        try {
          /**
           * This is how the auth-header is added to the view queries
           *  Format: base64Enc(<bucket username>:<bucket password>)
           **/
          op.addAuthHeader(HttpUtil.buildAuthHeader(user, password));
        } catch (UnsupportedEncodingException ex) {
          getLogger().error("Could not create auth header for request, "
            + "could not encode credentials into base64. Canceling op."
            + op, ex);
          op.cancel();
          return;
        }
      }

      HttpHost httpHost = getNextNode();
      HttpRequest request = op.getRequest();

      request.setHeader(HTTP.TARGET_HOST, httpHost.toHostString());
      requester.execute(
        new BasicAsyncRequestProducer(httpHost, request),
        new BasicAsyncResponseConsumer(),
        pool,
        coreContext,
        new HttpResponseCallback(op, this, httpHost)
      );
    }
  }
