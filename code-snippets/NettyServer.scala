  package play.core.server

  ....

  private val NettyOptionPrefix = "http.netty.option."

  def applicationProvider = appProvider

  private def newBootstrap: ServerBootstrap = {
    val serverBootstrap = new ServerBootstrap(
      new org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory(
        Executors.newCachedThreadPool(NamedThreadFactory("netty-boss")),
        Executors.newCachedThreadPool(NamedThreadFactory("netty-worker"))))
