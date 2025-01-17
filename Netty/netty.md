# Netty
## 一、概述
### 1.1 Netty介绍
(1)Netty是一个异步的，基于事件驱动的网络应用框架。根据客户端的一些读写行为来产生相应的事件驱动。
用以快速开发高性能、高可靠性的网络IO模型。异步代表不用等待服务器的回复就可以去做别的事情，等有响应之后触发回调即可。  
(2)Netty主要针对的是TCP协议下，面向Clients端的高并发应用，或者Peer-to-Peer场景下的大量数据持续传输的应用。  
(3)Netty其实是一种NIO框架，适用于服务器通讯相关的多种应用场景。  

### Java的I/O模型
用什么样的通道进行数据的发送和接收，在很大程度上决定了程序通讯的性能。Java支持3种网络编程的IO模式：BIO,NIO,AIO   
(1)BIO:同步阻塞，一个连接一个线程。 适用于连接数目比较少并且固定的框架。  
(2)NIO:同步非阻塞，一个线程处理多个请求，客户端发送的连接请求都会注册到多路复用器上，多路复用器轮询到连接有IO请求就进行处理。
适用于连接数目多，并且连接比较短的情况，聊天室、弹幕系统、服务器之间通讯等。    
(3)AIO:异步非阻塞，也叫NIO2，引入异步通道的概念，采样Proactor模式,有效的请求才启动线程。一般用于连接数较多，并且连接时间长的应用。比如一些充分调用操作系统参与并发操作等情况。    

### NIO(重点) java non-blocking IO
常用方法：    
capacity()方法，返回缓冲区容量  
put()方法，添加数据到缓冲区  
hasRemaining()方法，缓冲区是否还有数据  
get()方法，获取缓冲区中的数据，一个一个的读  
flip()    切换到读取数据模式  
rewind()  重复读取  
clear()   清空缓冲区  但是缓冲区中的数据还在 处于被遗忘状态  
mark() 标记当前position的位置  
reset() 恢复到mark位置  
transferFrom() 复制目标通道的数据到当前通道  



三大核心部分：管道Channel、缓冲区Buffer和Selector选择器。面向缓冲区的，数据读取到一个它稍后处理的缓冲区，需要时可以在缓冲区前后移动，增加了处理过程的灵活性。
每一个Channel都会对应着一个Buffer,Selector对应一个线程，一个线程对应多个Channel，程序切换到哪一个Channel是有事件决定的。Selector会根据不同的事件，在各个通道上切换。
Buffer是一个内存块，底层是一个数组，数据的写入是通过Buffer，需要通过flip去切换输入输出，Buffer是双向的。BIO则需要指定输入流或者是输出流。  

#### Buffer缓冲区
缓冲区的本质是一个可以读写数据的内存块，可以理解成是一个含有数组的容器对象，并且提供了一组方法，可以对缓冲区数据进行操作。  
根据数据类型的不同，提供了相应类型的缓冲区，boolean类型除外  
ByteBuffer\CharBuffer\ShortBuffer......通过allocate()获取缓冲区  

capacity 容量，缓冲区中最大存储数据的容量，创建缓冲区的时候被设定并且不可以改变   
limit    界限，缓冲区中可以操作数据的大小 limit后面的数据时不能进行读写的 
position 位置，缓冲区中正在操作数据的位置  
mark:    标记，表示记录当前position的位置，可以通过reset()恢复到mark位置  
大小关系： 0 <= mark <= position <= limit <= capacity  

#### Channel管道
通道可以同时进行读写操作，但是流只能读或者写。通道可以实现异步读写数据  
BIO的Stream流是单向的，NIO的Channel是双向的  
Channel是一个接口，主要的实现子类有FinalChannel 文件数据读写\DatagramChannel UDP数据读写\ServerSocketChannel和SocketChannel 用于TCP数据读写  
ByteBuffer支持类型化的put和get,但是put和get的类型要匹配   
可以将普通Buffer变成只读Buffer  
NIO还提供了MappedByteBuffer，可以让文件直接在内存中进行修改  
NIO还支持多个Buffer完成读写操作，Scattering 和 Gathering 分散聚合：  
Scattering：将数据写入到buffer的时候，可以采用Buffer数组[分散]   
Gathering：从buffer读取数据的时候，可以采用Buffer数组的方式[聚合]  

####  Selector选择器
(1) NIO运用的是非阻塞的IO方式，可以用一个线程处理多个客户端的连接，就会使用到selector选择器。  
(2) Selector能够检测到多个注册的通道上是否有事件发生，如果有，就湖区事件然后对事件进行处理，这样就可以只用一个单线程去管理多个通道。也就管理了多个连接和请求。  
(3) 只有在连接真正有读写事件发生的时候，才会进行读写，避免了开启多个线程和线程之间上下文的切换导致的开销。  
open() 得到一个选择器对象  
select() 监控所有注册的通道，当其中有IO操作可以进行的时候，将其中对应的SelectionKey加入到集合内部中来并返回
selectedKeys 从集合内部得到所有的selectionKey

当客户端连接的时候，会通过ServerSocketChannel得到SocketChannel  
将socketChannel注册到Selector上，一个Selector可以注册多个SocketChannel  
注册之后返回一个SelectionKey，会跟这个Selector关联  
Selector就会进行监听select方法，返回有事件发生的通道的个数，然后获取到各个有事件发生的SelectionKey  
这个SelectionKey就能得到了发生事件的Channel,通过Channel就能得到业务的处理  

#### 零拷贝
零拷贝指的是没有CPU参与的拷贝。零拷贝是网络编程的关键，跟多性能优化都离不开。  
Java程序中，主要的常用的零拷贝有内存映射(mmap)和sendFile。  
传统的IO要进行至少4次拷贝，3次系统状态的切换。很耗费性能。
mmap通过内存映射，将文件映射到内核缓冲区，同时用户空间可以共享内核空间的数据，这样，在进行网络传输的时候，就可以减少内核空间到用户控件的拷贝次数。  
在Linux2.1版本提供了sendFile函数，数据根本不需要进过用户态，直接从内核缓冲区进入到SocketBuffer，同时由于和用户态完全无关，就减少了一次上下文的切换。  
(1)零拷贝值，是从操作系统角度来说的，因为内核缓冲区之间，没有数据是重复的，只有kernel buffer有一份数据。
(2)零拷贝不仅仅带来更少的数据复制还能减少上下文切换，CPU缓存伪共享问题等。  
mmap适合少量的数据嘟咧，sendFile适合大文件传输。mmap需要进过4次上下文的切换，3次数据拷贝。
sendFile需要3次上下文切换，最少需要2次数据拷贝。
sendFile可以利用DMA方式，减少CPU拷贝，mmap则不能，必须从内核拷贝到Socket缓冲区。  

#### 原生NIO存在的问题
(1)NIO的类库比较复杂，需要熟练掌握Selector、ServerSocketChannel、socketChannel、ByteBuffer等  
(2)需要具备其他的额外技能，Java多线程，因为NIO涉及到Reactor模式，需要对多线程和网络编程非常熟悉，才能编写出高质量的NIO程序    
(3)开发的工作量大，客户端短线重连，网络闪烁，半包读写，失败缓存等等  
(4)Epoll Bug会导致Selector空轮询，最终导致CPU 100%  


### Netty概述
https://netty.io/
Netty是由JBoss提供的一个Java社区开源框架。提供异步、基于事件驱动的网络应用程序架构，用于快速开发高性能、高可靠的网络IO程序。  
Netty可以帮助快速搭建一个网络应用，相当于简化了NIO的开发过程。  
Netty是目前最流行的NIO框架，Netty在互联网领域、大数据分布式计算领域、游戏行业、通讯行业等都有广泛的应用。ElasticSearch、Dubbo内部都采用了Netty框架。

Netty的优点：  
1、设计优雅，对NIO进行了封装，适用于各种传输类型的统一API调度和非阻塞的Socket，基于灵活且可扩展的事件模型，
可以清晰的分离关注点，高度定制的线程模型，单线程-一个或者多个线程池   
2、使用方便，详细记录的Javadoc 用户指南和实例，没有其他的依赖项  
3、高性能、高吞吐量、低延迟，减少了资源的消耗，最小化不必要的内存复制  
4、安全，完整的SSL/TSL 和 StartTLS支持  
5、社区活跃，不断的更新，更多的新功能被不断的加入到Netty中  

netty版本有 netty3.x  netty4.x  netty5.x   因为netty5.x有重大bug 已经被官网废除了，目前使用最多的是netty4.1.x这个版本
下载地址：  https://bintray.com/netty/downloads/netty

### 线程模型
传统阻塞的IO服务模型 和 Reactor模型，Reactor根据数量和处理的资源线程池数量的不同，有3种典型的实现  
单Reactor单线程、单Reactor多线程、主从Reactor多线程   
Netty线程模型主要是基于主从的Reactor多线程模型做了一定改进之后的。

**阻塞IO服务模型**  
获取数据输入的是阻塞的IO模型，每个链接都需要独立的线程完成数据的输入、业务处理、和数据的返回等工作。  
会存在的问题，当并发大的时候没回创建大量的线程占用很大的系统资源。如果线程没有可读资源的时候，造成线程资源的浪费。  

**Reactor模式** 
多个连接共用一个阻塞对象，应用程序只需要在一个阻塞对象等待，无需等待所有连接。当某个连接有新的数据可以处理的时候，操作系统通知应用程序，线程从阻塞状态返回，执行业务处理。  
基于线程池去复用线程资源，不必再为每一个连接去创建线程，将连接完成后的业务处理分配给线程进行处理，一个线程可以处理多个连接任务。  

Reactor(也叫反应器模式、分发者模式、通知模式)  
通过一个或者多个输入同时传递给服务器，服务器会存在一个ServiceHandler，就会将这些传入请求分派到相应的处理线程  
使用了IO复用加事件监听，收到事件之后进行分发给线程  

Reactor模式核心的组成部分就是 Reactor 和 Handlers
reactor:在一个单独的线程中运行，负责监听和分发事件，分发给适当的处理程序来对IO事件进行反应。
Handlers:处理程序执行IO事件要完成的实际事件。

**单Reactor多线程模式**   
Reactor对象通过select监控客户端请求时间，通过dispatcher进行分发。  
如果是建立连接请求，由Accept去处理连接请求，然后创建一个Handler对象处理完成连接后的各种事件。  
如果不是建立连接请求，则由Reactor分发调用对应的Handler分发来处理。Handler只负责响应事件，不做处理事件的业务。通过read读取到数据之后，分发给后面的worker线程池的某个线程进行处理。
Worker线程池会分配具体的线程完成真正的请求将结果返回给Handler,Handler通过send方法将结果返回给CLient。  

优点：充分利用多核CPU的处理能力。  
缺点：多线程数据共享和访问比较复杂，Reactor自己是单线程的，要处理所有的事件监听和响应分发。  

**主从Reactor多线程**  
主Reactor只管理连接请求的建立，而从Reactor可以有多个,各自都可以处理事件的分发和Handler的调用，再把业务的调用分配给Worker线程池去处理。  

优点：父线程与子线程的数据交互简单职责明确，父线程只要接收新连接，子线程分发完成后续的处理。  
缺点：编程的复杂程度较高。
这种主从多线程模型在许多项目中应用广泛，Nginx、MemCached、Netty等。

### Netty模型
BossGroup线程维护Selector，只关注Accept,当接收到Accept事件，获取对应的SocketChannel并且注册到Work线程（事件循环）
当Worker线程监听到selector中通道发生自己关注的事件之后，就进行处理。
  
Netty工作原理示意图解析：  
1、Netty抽象出两组线程池，一组BossGroup专门负责接收客户端的连接，另一组WorkerGroup则专门负责网络的读写  
2、BossGroup和WorkerGroup的类型都是NioEventLoopGroup  
3、NioEventLoopGroup相当于一个事件循环组，这个组中含有多个事件循环，每一个事件循环是NioEventLoop   
4、NioEventLoop表示一个不断循环的执行处理任务绑定任务的线程，每一个NioEventLoop都有一个Selector，用于监听绑定在其上的socket的网络通讯  
5、NioEventLoopGroup可以有多个线程，即可以含有多个NioEventLoop  
6、每个Boss下的 NioEventLoop 循环执行的步骤：  
 6.1、轮询accept事件  
 6.2、处理accept事件，与Client建立连接，生成NioSocketChannel，并将其注册到某一个的worker NioEventLoop上的Selector上  
 6.3、再去处理任务队列上的任务，即runAllTasks  
7、每个Worker下的NioEventLoop 循环执行的步骤：   
 7.1、轮询read write事件  
 7.2、处理IO，在对应的NioSocketChannel处理   
 7.3、处理任务队列的任务，即runAllTasks   
8、每一个Worker NioEventLoop在处理业务的时候，会使用到管道pipeline,管道中包含了很多的channel ，通过pipeline可以获取到channel通道，管道中维护了很多的处理器、拦截器等等。  

### debug (om.learn.netty.simple)
Netty在创建事件循环组的时候 NioEventLoopGroup 线程的数量时CPU核心数x2    NettyRuntime.availableProcessors() * 2  
每一个线程就叫 NioEventLoop 叫一个事件循环

任务队列中的Task有3种典型的应用场景
1、用户程序自定义的普通任务
2、用户自定义的定时任务
3、非当前Reactor线程调用Channel的各种方法
例如在推送系统的业务线程里面，根据用户的标识，找到对应的Channel引用，然后调用Write类方法向该用户推送消息，就会进入到这种场景。最终的write会提交到任务队列中后被异步消费。

Netty抽象出两组线程池，BossGroup专门负责接收客户端连接，WorkerGroup专门负责网络读写操作    
NioEventLoop表示一个不断循环执行处理任务的线程，每一个NioEventLoop都有一个Selector，用于监听绑定在其上的socket网络通道      
NioEventLoop内部采用串行化的设计，从消息的读取->解码->处理->编码->发送，始终由IO线程NioEventLoop负责    
每个NioEventLoop中包含一个Selector、一个taskQueue  
每个NioEventLoop的Selector上可以注册监听多个NioChannel  
每个NioChannel只会绑定在唯一的NioEventLoop上  
每个NioChannel都绑定有一个自己的ChannelPipeline  

### 异步模型
(1) 异步和同步是相对的，当一个异步过程调用发出之后，调用者不能立刻得到结果。实际处理这个调用的组件在完成之后，通过状态、通知和回调来通知调用者。  
(2) Netty中的IO操作是异步的，包括Bind、Write、Connect等操作会简单的返回一个ChannelFuture  
(3) 调用者并不能立刻获得结果，而是通过Future-Listener机制，用户可以方便的主动获取或者通过通知机制获得IO操作结果  
(4) Netty的异步模式是建立在future和callback之上的。callback就是回调。future的核心设计思路是：  
假设一个方法，计算过程可能非常耗时，等待fun返回显然不合适。  
那么可以在调用fun的时候，马上返回一个Future，后续可以通过Future去监控方法的处理过程。(Future-Listener机制)

**Future**
表示异步执行的结果，可以通过它提供的方法来检测执行是否完成，比如检索计算等等。  
ChannelFuture是一个接口，可以添加监听器，当监听的时间完成之后们就会通知到监听器。 

Netty在使用的时候，拦截操作和转换出入站数据只需要提供callback和future即可，这是的链式操作简单、高效、有利于别写可重用、通用的代码。
Netty框架的目标就是从业务逻辑从网络基础应用编码中分离出来。    
 
**Future-Listener机制**    
当Future对象刚刚创建的时候，处于非完成状态，调用者可以通过返回的ChannelFuture来获取操作执行的状态，注册监听函数来执行完成后的操作。    
通过isDone() 来判断当前操作是否已经完成  
通过isSuccess() 来判断已完成的当前操作是否成功  
通过getCause() 来获取已完成的当前操作失败的原因  
通过addListener 来注册监听器，当操作已经完成，将会通知监听器，如果Future对象已经完成，则通知指定的监听器  

Http协议是短连接

Bootstrap是引导，一个Netty应用通常由一个Bootstrap开始，主要的作用是配置整个Netty程序，串联各个组件。  
BootStrap类是客户端的启动的引导类，ServerBootstrap是服务端启动的引导类。  

**Selector**  
Netty是基于Selector对象实现IO多路复用，通过Selector一个线程就可以监听多个连接的Channel事件  
当像一个Selector中注册Channel之后，Selector内部的机制就可以自动不断的查询这些注册的Channel是否有已经就绪的IO事件(可读，可写，网络连接完成等等)  
这样程序就可以简单的使用一个线程高效的管理多个Channel  

**pipeline和ChannelPipeline**   
ChannelPipeline是一个Handler的集合，它负责处理和拦截inbound或者outbound的事件和操作，相当于一个贯穿Netty的链  
(ChannelPipeline是保存ChannelHandler的List,用于处理或者拦截Channel的入站和出站事件)  
ChannelPipeline实现了一种高级形式的拦截过滤模式，使用户可以完全控制事件的处理方式，以及Channel中各种ChannelHandler如何交互  
  
Channel有一个和他对应的ChannelPipeline 这两个是相互对应，相互可以获取到的。  
Channel Pipeline 维护了一个由ChannelHandlerContext组成的双向链表，每一个ChannelHandlerContext有一个与它关联的Handler去处理专门的事件。  
入站和出站事件发生在这个双向链表中，入站事件会从链表的头传递到尾，出站事件会从链表的尾向前传递到最开始的头。 


**ChannelHandlerContext**  
保存Channel相关的所有上下文信息，同时关联一个ChannelHandler对象。    
ChannelHandlerContext中包含一个具体的事件处理器ChannelHandler,同时ChannelHandlerContext也绑定了对应的pipeline和Channel的信息  


**EventLoopGroup和它的实现类NioEventLoopGroup**  
EventLoopGroup是一组EventLoop的抽象，每一个EventLoop都维护着一个Selector实例。  
EventLoopGroup提供next接口，可以从组里面按照一定规则获取其中一个EventLoop来处理任务。  
通常一个服务端口也就是一个ServerSocketChannel对应一个Selector和一个EventLoop线程。BossEventLoop负责接收客户端的连接并将SocketChannel交给WorkerEventLoopGroup来进行IO处理。  

BossEventLoopGroup通常是一个单线程的EventLoop，EventLoop维护着一个注册了ServerSocketChannel的Selector实例，BossEventLoop不断轮询Selector将连接事件分离出来。   
OP_ACCEPT事件，将接收到的SocketChannel交给WorkerEventLoopGroup  
WorkerEventLoopGroup会由next选择其中一个EventLoop来将这个SocketChannel注册到其维护的Selector上，并对其后续的IO事件进行处理  

**Unpooled类**  
Netty提供一个专门用来操作缓冲区(即Netty的数据容器)的工具类    
ByteBuf buf = Unpooled.buffer(10);  
是一个byte[10]的数组 不需要使用flip进行读写切换反转  
底层是维护了一个readeIndex 和 writeIndex  
ByteBuf buf =  Unpooled.copiedbuffer("aaaa",Charset.forName("utf-8"));  
 
 
### Google Protobuf  
在辨别网络应用程序的时候，因为数据在网络中传输的都是二进制的字节码数据，在发送数据的时候就需要编码，接收数据的时候就需要解码。  
codec(解码器)的组成有两个，decoder解码器和encoder编码器  
  
Netty本身就提供了编解码器，ObjectDecoder和ObjectEncoder可以用来时间POJO对象或者各种业务对象的编码和解码，底层使用的是Java的序列化技术，而Java本身的序列化本身效率就不太高。  
无法跨语言、序列化后的体积大、是二进制的5倍还要多，序列化的性能不好。所有才会有 Google的Protobuf来解决这个问题。  

#### Protobuf
是谷歌发布的一个开源项目， Google protocol Buffers 是一种轻便高效的结构化数据存储格式，可以用于结构化数据的序列化。它适合做数据存储或者是RPC的数据交换。  
支持跨平台、跨语言。 http + json  ->  tcp + protobuf

### Netty编解码器和handler的调用机制
netty的核心组件主要有Channel、EventLoop、ChannelFuture、ChannelHandler、ChannelPipe等。
ChannelHandler充当了处理入站和出站数据的应用程序逻辑的容器。例如，时间ChannelInboundHandler接口(ChannelInboundHandlerAdapter)，就可以接收入站和出站的数据，这些数据被业务逻辑处理。  
当要给客户端发送响应的时候，可以从ChannelBoundHandler冲刷数据。
ChannelPipe提供了ChannelHandler链的管理方案。事件的运动方向是从客户端到服务端，就叫入站，从服务端到客户端，就叫出站。  

netty发送或者接收一个消息就会发生一次数据转换。入站把字节码转换成想要的格式，出站把数据编码成字节数据。   
由于不可能知道远程节点是否会一次性发送一个完整的信息，tcp有可能出现粘包和拆包问题。解码器ByteToMessageDecoder会对入站的数据进行缓冲。  

### TCP粘包和拆包问题
TCP是面向连接、面向流的，提供高可靠性服务。收发两端都要有一一成对的socket   
发送端为了将多个发给接收端的包，更有效的发给对方，使用了优化方法Nagle算法  
将多次间隔较小的数据量小的数据，合并成一个大的数据块，然后进行封包。这样虽然提高了效率  
但是接收端就难于分辨出完整的数据包，因为面向流的通信是无消息保护边界的  
因为TCP无消息保护边界，需要在接收端处理消息的时候，处理消息边界问题，也就是所说的粘包和拆包  

客户端发送的是两个数据包，由于客户端一次读取到的字节数是不确定的，就可能出现这些情况：
1、服务端分两次，接收到了两个独立的数据包 D1,D2
2、服务端一次性接收到了两个数据包 D1D2
3、服务端分两次，第一次读取到D1D2_1,第二次读取到D2_2
4、服务端分两次，第一次读取到D1_1,第二次读取到D1_2D2

**解决**  
使用自定义协议 + 编解码器 来解决
服务器端每次读取数据长度的问题，就不会出现服务器多读或者少读的问题，从而避免TCP的粘包和拆包。  

## Netty源码分析
### Netty启动过程梳理  
1) 创建 2 个 EventLoopGroup 线程池数组。数组默认大小 CPU*2，方便 chooser 选择线程池时提高性能  
2) BootStrap 将 boss 设置为 group 属性，将 worker 设置为 childer 属性  
3) 通过 bind 方法启动，内部重要方法为 initAndRegister 和 dobind 方法  
4) initAndRegister 方法会反射创建 NioServerSocketChannel 及其相关的 NIO 的对象， pipeline ， unsafe，同时也为 pipeline 初始了 head 节点和 tail 节点。  
5) 在 register0 方法成功以后调用在 dobind 方法中调用 doBind0 方法，该方法会 调用 NioServerSocketChannel的 doBind 方法对 JDK 的 channel 和端口进行绑定，完成 Netty 服务器的所有启动，并开始监听连接事件  

### Netty 接受请求过程梳理
总体流程：接受连接----->创建一个新的 NioSocketChannel----------->注册到一个 worker EventLoop 上-------->注册 selecot Read 事件    
1) 服务器轮询 Accept 事件，获取事件后调用 unsafe 的 read 方法，这个 unsafe 是 ServerSocket 的内部类，该方法内部由 2 部分组成  
2) doReadMessages 用于创建 NioSocketChannel 对象，该对象包装 JDK 的 Nio Channel 客户端。该方法会像创建 ServerSocketChanel 类似创建相关的 pipeline ， unsafe，config  
3) 随后执行 执行 pipeline.fireChannelRead 方法，并将自己绑定到一个 chooser 选择器选择的 workerGroup 中的一个 EventLoop。并且注册一个 0，表示注册成功，但并没有注册读（1）事件  