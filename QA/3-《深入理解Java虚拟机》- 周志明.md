字字珠玑，很适合精读。
# 《深入理解Java虚拟机》周志明
## 展望未来  
即时编译器：应用系统预热之后，热点代码被HotSpot的探测机制定位捕获，将其编译为物理硬件可以直接执行的机器码。所以即时编译器输入代码的质量也决定了代码运行的效率。  
C1:耗时短，输出代码优化程度低的客户端编译器  
C2:耗时长，但是输出代码优化质量更高的服务端编译器 Graal编译器作为C2的替代产品。  
本地类型变量推断  

## 一、Java内存区域与内存溢出异常 
Java与C++之间隔着内存动态分配和垃圾收集器。  
  
### 运行时数据区  
1、程序计数器：当前线程所执行的字节码的行号指示器。下一条指令的地址。字节码解释器改变这个计数器的值来选取下一条需要执行的字节码指令。
它是程序控制流的指示器，分支、循环、异常等等都需要依赖于这个计数器。  
Java的多线程是通过线程轮流切换、分配CPU执行时间来实现的。在一个确定的时间，cpu只可能执行其中的一条指令，为了切换线程之后能找到准确的位置，程序计数器是线程私有的。    
若某个线程正在执行的是一个Java方法，那么这个线程的程序计数器是字节码指令的地址。如果正在执行的是本地方法Native，这个计数器的值是空Undefined。
这个区域不会有OutOfMemoryError。
  
2、Java虚拟机栈：描述的是Java方法执行线程内存模型。每个方法被执行的时候都会创建一个栈帧 Stack Frame,里面存储的是局部变量表、操作数栈、动态链接、方法出口等信息。  
存储局部变量的空间是以Solt槽来表示，除了double和long类型是占据两个槽之外，其余的数据类型只占用一个。  
如果线程请求栈的深度大于虚拟机允许的深度，将抛出StackOverflowError异常，如果是运行虚拟机栈容量是可以扩容的，当请求不到足够的内存时，会抛出OutOfMemoryError异常。  
  
3、本地方法栈：与虚拟机栈发挥的作用相似。虚拟机栈为Java方法服务（字节码），本地方法栈为本地方法提供服务（Native）。

4、堆：虚拟机管理的内存中最大的一块，被所有线程共享，唯一目的就是存放对象实例。几乎所有的对象实例都会在这里被分配。  
几乎（即时编译技术的进步，逃逸分析，栈上分配，标量替换，这些技术使得在堆中分配对象是不绝对的）  
现代的大部分垃圾收集理论都是基于分代收集的概念设计的，所以有新生代，老年代，永久代，Eden区，幸存者from区，幸存者to区域。  
线程共享的Java堆可以划分出多个线程私有的分配缓冲区（TLAB）,用于提升对象分配的效率。
堆空间在物理上不连续，在逻辑上连续。虚拟机参数可以设定堆的大小。-Xmx和-Xms,当堆没有内存能完成实例分配，堆也无法再扩展的时候，就会抛出OutOfMemory异常。

5、方法区：每个线程的共享数据，用于存储已经被虚拟机加载的类型信息、常量、静态常量、即时编译器编译之后的代码缓存等数据。  
运行时常量池也是方法区的一部分。Class文件中除了有版本、字段、方法、接口，还有常量池Conttant Pool Table,存储编译期间生产的各种字面量与符号引用，当类加载了之后，，这些信息就被存放到方法区的运行时常量池中。另外，运行期间也能动态的把新的常量放入池中。
当无法申请到内存的时候会抛出OutOfMemory异常。  

6、直接内存：不是虚拟机运行时数据区的一部分，但是频繁使用，也会导致OOM。
NIO:一种基于通道和缓冲区的IO方式，它可以直接使用Native方法直接分配堆外内存，通过堆内存中的DirectByteBuffer直接操作内存数据，能显著提升IO性能。
当设置内存时，若忽略掉这部分数据，使得内存区域的综合超过了物理内存的限制，在动态扩展的时候就会出现OOM。

### HotSpot虚拟机在新建对象的时候到底发生了什么？   
Java程序在运行过程中无时无刻都在创建对象，当虚拟机遇到一条创建对象的字节码指令的时候：  
1、首先检查指令的参数在常量池中是否能够定位到一个类的符号引用，并且检查这个符号引用的类是否已经被加载、解析、初始化过。（检查要创建的对象的类型是否在常量池中存在。）  
2、加载完毕之后开始为这个对象分配内存，所需要的内存空间大小在类加载之后是能够确定的。  
&emsp;2.1、指针碰撞：假设堆内存是规整的（使用过的内存和空闲的内存时隔开的，中间使用指针作为界线指示器），就只需要把指针往空闲方挪动即可，步长等于对象在加载之后的确定的需要占据空间的大小。  
&emsp;2.2、空闲列表：假设堆内存不是规则的，虚拟机就必须维护一个记录堆空间使用情况列表，从列表中找出一块足够大的空间划分给该对象。  
&emsp;因此，选择哪种分配方式由堆内存是否规整决定，而堆内存是否规整又由使用的垃圾收集器是否具有压缩整理能力决定。  
&emsp;比如：Serial,ParNew是具有压缩整理能力的垃圾收集器，可以使用指针碰撞办法来分配对象内存，而像CMS这种基于清除算法的收集器，理论上只能使用复杂的空闲列表分配算法来分配对象的内存。  
3、空间划分完毕之后，还有考虑线程的安全性问题。创建对象的动作在虚拟机中是非常频繁的。    
&emsp;3.1、采用CAS配合自旋的失败重试办法。  
&emsp;3.2、把内存的动态分配按照线程划分在不同的空间之中进行，每个线程预先分配缓冲（TLAB）,线程的缓冲用完之后，分配新的缓冲时再采用同步锁定的方式保证安全。  
4、将分配到的内存空间初始化零值（除对象头之外）。保证对象的实例字段在不给初值的情况下能直接使用。  
5、初始化对象头信息。对象的GC分代年龄，属于哪一个类的实例，怎么寻找对象的元数据信息等。  
做完这些，一个虚拟机层面可用的对象才算创建成功，但是从Java程序的角度，才刚刚开始创建对象，执行构造方法<init>,将对象需要的资源按照它预定的意图创建好。  

### Java对象的内存布局
一个Java对象在堆内存中的结构由三部分组成：对象头,实例数据，对齐填充。  
对象头：有两部分组成，第一部分是对象自身的运行时数据：hashCode,GC分代年龄，锁状态标志，线程持有的锁，偏向线程的ID,偏向时间戳等。
另一部分是类型指针，对象指向它元数据的指针，通过这个指针来判断是能够类的实例。  
实例数据：对象真正存储的有效信息，longs/doubles,ints,oops等......相同宽度字段总是分配到一起存放。  
对齐填充：补充这个对象到8字节的整数倍。

### Java对象的访问定位方式
两种方式，句柄池和直接指针。  
句柄池：堆内存中开辟出来的一块空间作为句柄池，reference中存储的是句柄池的地址，句柄再指向对象实际的地址。（优点：reference存储的是一个稳定的地址，当对象的实际地址改变的时候，改变的是句柄的指针，reference不会改变）。  
直接指针：reference直接指向的就是对象实际的地址。（优点：节省了一次指针定位的时间）  
HotSpot使用的是直接指针。  

### OOM情况
OOM heap space  对象的总容量触及了最大堆的容量  
StackOverFlowError 栈深度大于允许的最大深度,如果设置栈运行动态扩容，当扩展栈内存的时候无法申请到内存的时候就会报OOM JavaVMStackSOF.leak
OOM PermGen space 运行时常量池溢出，方法区，使用Cglib代理的时候需要注意  
OOM directMemory 使用了直接内存，Netty,NIO等场景时需要注意  


## 二、垃圾收集器与内存分配策略
Java与C++之间永远隔着一堵由动态内存分配和垃圾收集技术围成的高墙，里面的人想出来，外面的人想进去。  
当需要排查各种内存溢出、内存泄露等问题的时候，当垃圾收集成为系统要达到更高并发的瓶颈的时候，就需要对这些自动化的技术实施的细节进行监控和调整。    
程序计数器、虚拟机栈、本都方法栈三个区域随着线程的死亡而死亡，栈中的栈帧随着方法的调用和返回而不断的进栈出栈。    
而Java的堆和方法区两个空间有着明显的不确定性。一个类的多个不同实现类，一个方法不同的条件分支也是无法确定的。只有当程序运行起来，才知道这些信息，而这些信息时时刻刻都在发生着变化。垃圾收集器关注的正是这一部分内存空间应该如何管理。  

### 判断对象已死
引用计数法：有引用就让计数器的值加一，没有就减一，当计数器的值为0，则代表对象不可能再被使用。无法解决循环引用的问题，已经被废弃的一种算法。  
可达性分析：通过一系列GC Root作为起始节点，通过引用关系向下搜索，如果一个对象到这些GC Roots之间都没有引用链路，判断对象不可达。可以被垃圾回收。  
可以作为GC Root的对象有：  
1、栈帧中的本地变量表中的引用对象（参数，局部变量，临时变量）  
2、方法区中类静态属性引用的对象（Java类的引用类型静态变量）  
3、在方法区中常量引用  
4、本地方法栈中的JNI(native方法)引用的对象  
5、Java虚拟机的内部引用（常驻异常对象，系统类加载器）
6、被synchronized关键字持有的对象
7、反映虚拟机情况的JMXBean,本地代码缓存等

### 引用分类
强软弱虚，在缓存系统中经常使用软引用和弱引用。  
**强引用**：强引用关系还在，永远不会被回收。    
**软引用**：在系统将要发生内存溢出异常的时候，会把软引用放入回收范围之内进行第二次回收，如果还是没有足够的空间才会抛出内存泄露的异常。  
**弱引用**：只能存活到下一次垃圾收集发生之前。  
**虚引用**：无法通过这种引用获得对象实例。唯一的作用就是在这个对象被垃圾收集的时候收到一个系统通知。  

### 对象的自我拯救
经过可达性分析之后，没有与GC Roots相连的对象会被标记，随后会对这些对象进行一次筛选，是否有必要执行finalize()方法。  
若对象没有覆盖finalized方法或者这个对象已经执行过了finalized方法，都会被认为是没有必要执行，接着就会被回收。  
被判定为有必要的对象，就会进入一个F-Queue队列中让他们去执行它们各自的finalized方法。  
这个时候假如有对象在finalized方法中再次调用自己，或者是执行缓慢，这样做的目的就是不想被回收。  
所以虚拟机一开始的设定是假如对象已经执行过一个finalized方法也被判定为没必要，直接被回收。   
对象唯一的自我拯救机会是在finalized方法中，将自己与GC Root上的对象建立引用链路即可，这样在第二次标记的时候就会被移除F-Queue队列。  
这种拯救方法只能被使用一次。第二次被判定不可达的时候，虚拟机不会让对象有机会执行finalized方法。  
并且这种拯救对象的方法是极力不推荐的。建议忘记Java中的finalized()方法。    

### 回收方法区
方法区回收的内容是废弃的常量和不再使用的类型。  
废弃常量：一个字符串"aaa"曾经出现在常量池中，但是当前系统没有任何一个字符串的值是"aaa",就需要把这个"aaa"常量移除常量池。类似的，方法，字段的符号引用也是这样的。  
类型不再使用：这个相对苛刻。要满足三个要求。1、这个类的实例都已经被回收。2、加载这个类的类加载器也被回收。3、这个类对应的反射对象也被回收，保证无法通过反射去访问这个类的方法。  
在大量使用反射，动态代理，Cglib等字节码框架的场景中，需要Java虚拟机具备类型卸载能力，保证不会对方法区造成太多的内存压力。  
  

### 分代收集理论
**弱分代假说**：绝大多数对象都是朝生夕死。   
**强分代假说**：熬过越多次垃圾收集的对象，就越难以消亡。  
  
这两个假说决定了很多垃圾收集器的设计理念和统一原则：应该将堆划分成不同的区域，将对象依据年龄分配到不同的区域中进行存储。  
这样，同一个区域中的对象年龄就大概是相同的，这样做的好处是：  
如果同一个区域的对象基本都是朝生夕死，集中放在一起，就只需要关注保留少量的存活，不去标记那些大量会被收集的对象。  
如果同一个区域的对象基本都是难以消亡的对象，集中在一起，只需要使用较低的评率来回收这个区域的对象即可。同时兼顾了内存开销和时间开销。  
  
但是这些概念还不足够解决复杂的垃圾收集的问题，因为会存在着跨代引用的问题。新生代对象被老年代引用，老年代也会被新生代引用。难道要去扫描整个老年代或者新生代？  
所以有了第三个**跨代引用假说**：跨带引用相对于同代引用来说仅仅占极少数。隐含的意思就是，互相引用的两个对象，应该是倾向于同时生存或者同时消亡。  
比如:某个新生代的对象引用了老年代对象，新生代的对象会在收集的时候得以存活，随着年龄的增长也来到老年代中。
利用这条假说，在新生代上建立一个全局的记忆集结构，这个结构把老年代划分成若干个小块，标识出哪一块内存会存在跨带引用。  
当发生了新生代的垃圾回收的时候，只需要把那些被标记了有跨带引用的老年代中对象作为GC Root即可。  
这种方法需要在对象改变了引用之后，维护记录数据的正确性，会增加一些额外的开销，但是比起全部扫描整个老年代来说，是很划算的。  
  
部分收集：不是对整个堆内存进行垃圾收集。分为：  
新生代收集：Minor GC / Young GC  
老年代收集：Major GC / Old GC  
混合收集： Mix GC 收集整个新生代和部分老年代，目前只有G1收集器有这种行为  
整堆收集：Full GC 

### 标记-清除算法
先标记需要收集的对象，回收所有被标记的对象。或者也可以标记存活对象，回收所有没有被标记的对象。   
这种算法有两个主要缺点：  
1、执行效率不稳定。有时堆中的大量对象需要被回收，对象的数量决定了执行的效率。
2、空间碎片化。会产生大量不连续的内存碎片。若后面有大对象被创建，就不得不再一次进行垃圾回收，间接导致GC频繁发生。


### 标记-复制算法
准确的说是半区复制，把实际容量划分为相等的两份，把存活下来的对象赋值到另外一个半区上，然后把已经使用过的内存空间清理掉。  
如果内存中的大部分对象在GC Root之后大部分是存活的，就会产生大量的复制开销。   
如果内存中只有少部分是存活的，就能很好的解决内存碎片问题。    
优缺点是很明显的，实现简单，运行高效。缺点就是实际可用空间只有一半。  
这种算法被运用在具有“朝生夕死”的新生代上，IBM公司的研究显示，98%的对象都熬不过第一轮的垃圾收集。所以根本不需要1:1的比例。  
  
1989年，出现了一种更加优化的半区优化策略——Appel式回收。HotSpot虚拟机的Serial、ParNew收集器都是采用这种思想。  
具体做法是：把新生代划分成一块比较大的区域Eden区（伊甸园区）和两块比较小的Survivor区（幸存者0区，幸存者1区），当幸存者区域不足以存放一次Miner GC之后幸存对象的时候，就需要依赖其他内存区域进行分配担保（**逃生门安全设计**）。
对象只分配在伊甸园区和其中的一个幸存者区，发生垃圾收集的时候，把存活的对象一次性复制到另一块幸存者区域，然后清理到伊甸园区和已经用过的那块幸存者区域的内存空间。  
HotSpot虚拟机的伊甸园区和两块幸存者区域的比例是 8:1:1   

### 标记-整理算法
标记过程都一样，后续的步骤不是直接对可回收对象进行清理，而是把所有存活对象往内存空间的一端移动，然后清理掉边界以外的内存空间。  
但是，移动对象并且更新引用将会是一项极为负重的工作。而且最关键的是移动的过程必须**暂停全部的用户线程**，stop the world。  

权衡一下：如果使用标记清除，那么产生的内存碎块只能依赖于复杂的内存分配来解决，但是一个系统创建对象是及其频繁的过程，这样势必导致系统的吞吐量下降。
如果使用标记整理，那么就会存在对象的移动，在垃圾收集的时候，系统会发生停顿。  
HotSpot虚拟机中，专注于吞吐量的Parallel Scavenge收集器采用的是标记整理算法。
专注于低延迟的CMS收集器则是基于标记清除算法。

## HotSpot算法实现细节（难）
### 根节点枚举
垃圾收集过程中的安全指的是，可达性分析的过程中，对象的引用关系不会发生变化。    
虽然知道GCRoots集合一般是在全局引用与栈帧的本地变量表中。目标明确，但是数据量未免也太过庞大。  
并且所有的收集器在根节点枚举这一步的时候，必须暂停用户线程。  
虽然可达性分析中耗时最长的查找引用链过程已经能够坐到和用户线程同步，但是在根节点枚举这一步，必须暂停全部的用户线程。  
    
    
当类加载的动作完成之后，虚拟机会把对象引用的信息放到OopMap的一个数据结构中，这样收集器在扫描的时候就知道这些引用了，不需要一个不漏的从方法区等GC Roots开始查找。  
但不是所有的指令都会生成OopMap，只是在**安全点**记录这些信息。通过这个OopMap,虚拟机可以快速的完成GC Roots枚举。  
所以不是所有字节码位置都会停下来进行垃圾收集，而是强制要求必须执行到安全点之后才会停顿下来开始垃圾收集。  
安全点的选择是以“是否具有让程序上时间执行的特征”，比如方法调用、循环、异常跳转等。  
另外还有一个问题是：如何让所有线程都运行到安全点？    
抢先式中断：系统先停止所有用户线程，如果发现某一个线程不在安全点上，就恢复这个线程的执行，让他跑到了安全点上之后再中断来响应GC事件。（基本不用）  
主动式中断：当垃圾收集器需要中断线程的时候，设置一个标志位，各个线程执行的时候会不断的主动轮询这个标志，一旦发现标志位是真，就自己在最近的安全点上主动中断挂起。所以轮询标志位的操作在线程中很频繁。  
   
安全点的设计完美的解决了如何让线程停顿下来进入垃圾回收状态的问题。但是如果线程是处于Sleep或者Blocked状态，这种线程无法走到安全点。  
**安全区域**解决了这种问题。  
安全区域指的是在某一段代码片段中，引用关系不会发生变化，因此在这个区域中的任何位置开始进行垃圾收集都是安全的。线程处于这种状态的时候，可达性分析是安全的。  
所以线程走进了安全区域的时候，会标识自己进入了安全区域。这样要发起垃圾收集就不用管这些申明了自己在安全区域内的线程了。    
当线程离开安全区域的时候，先检查是否完成了根节点枚举，没有就等待收到可以离开安全区域的信号为止。  
  
    
### 记忆集与卡表
为了解决跨代引用，在新生代中建立了记忆集的数据结构，用于避免把整个老年代加进GC Roots扫描范围。  
**记忆集**：一种用于记录从非收集区域指向收集区域的指针集合。这种记录的成本在空间占用和维护上都是很占据成本的。  
收集器只需要判断这个区域是否有指向收集区域的指针就可以了，细节东西不需要关心。一种每一个记录都精确到一块内存区域，该区域内有对象含有跨带指针叫做卡表。
**卡表**：（本区域的一块内存中，有对象持有着指向收集区域的引用。这个对象的地址是这样的。。。）卡表是一个字节数组，每一个元素都对应着标识内存区域一块特定大小的内存（卡页）。
一个卡页的内存中通常包含不止一个对象，只要卡页内有对象的字段存在着跨代指针，就把这个卡表的数组元素的标识值为1，称为这个元素变脏。没有标识的为0。  
总结起来就是：有其他分代区域中的对象引用了本区域的对象时，其他分代对象对应在卡表的指针元素就会变脏。
在发生了垃圾收集，只要筛选出卡表中变脏的元素，就能知道哪些卡页内存块中包含跨带指针，把他们加入GC Roots中一并扫描即可。

### 写屏障
维护卡表。如何在对象赋值的时候去更新维护卡表。其他分代的对象引用了本区域的对象的时候，卡表上对应的元素就应该变脏。    
假如是字节码，虚拟机完全可以处理。但是即时编译器编译之后的产物已经是纯粹的机器指令，虚拟机不可能介入其中。  
写屏障相当于对引用类型复制这个动作进行的一个环绕通知。一旦收集器在写屏障中增加了对卡表的操作，只要更新了引用，就会产生额外的开销。不过这个跟扫描整个分代的代价比起来还是可以接受的。  

### 伪共享问题
当多线程修改独立变量，这些变量共享一个缓存行，就会彼此影响。  
卡表的卡页上不仅仅有一个对象，可能是多个。所以当同一个卡页上的多个对象赋值更新了之后，就会更新同一个卡表元素的标识。所以需要多进行一步判断，判断卡表元素标记没有被修改过的时候，才去将他标记变为脏。已经是脏的就不管了。  
jdk7之后可以使用-XX:+UseCondCardMark来决定是否开启这个条件判断。  

### 并发的可达性分析
在根节点枚举这一个步骤中，在各种优化技巧（OopMap）的加持下，它的停顿时间是非常短的，并且不会随着堆容量的增大而增长。
接下来，进行可达性分析的时候，如果堆中对象越多，可达性分析锁需要的时间也就越长。
但是，可以通过一些手段，让可达性分析的过程和用户线程并发执行。

**三色标记**
白色：没有被垃圾收集器访问。刚开始的时候所有对象都是白色的。  
黑色：已经被垃圾收集器访问，并且这个对象的所有引用都已经扫描过。它是安全存活的。  
灰色：对象被垃圾收集器访问，但是这个对象存在的引用没有扫描完毕。  
灰色是波峰的波纹。  
多个用户线程和多个GC线程同时存在的情况下，可能出现两种情况：
把原本消亡的对象标记为存活。可以容忍。  
把原本存活的对象标记为死亡（原本是黑色的对象被标记为白色）。分析出现的原因只有两个：  
1、赋值器插入了一条或者多条从黑色对象到白色对象的引用。  
2、赋值器删除了全部从灰色对象到该白色对象的直接或者间接引用。  
只要破坏这两个条件中的一个就可以解决并发扫描时候对象消失问题。  
增量更新：破坏的是第一个条件。当给黑色对象插入白色对象引用的时候，就记录下这个插入的引用记录下来，等扫描结束，再将这些记录过的引用关系中的黑色对象为根，重新扫描一次。  
原始快照：破坏第二个条件。当灰色对象要删除指向白色对象的引用关系的时候，就将这个要删除的节点的引用记录下来，在扫描结束之后，将这些记录过引用关系的灰节点为根，重现扫描。
CMS是通过增量更新来做并发标记的。G1,Shenandoah则是使用原始快照来实现的。


### 经典的垃圾回收器
![Image 图片丢失](https://raw.githubusercontent.com/xiaoshaDestiny/My-Note-Utils-Learn/raw/master/QA/image/GarbageCollectors.jpg)
**G1**
Garbage First垃圾收集器技术历史上的里程碑，收集器面向局部收集的设计思路和基于Rigion的内存布局。  
面向服务端应用。设计的目标是将垃圾收集的时间不超过N毫秒。  
G1不再坚持固定大小以及固定数量的分代区域划分，把堆空间划分成了多个大小相等的独立区域（Region）  
每一个Region可以根据需要扮演Eden\Survivor\老年代。  
Region中还开辟了专门给大对象的Humongous区域。超过Region的一半的对象就判定为大对象。  
-XX:G1HeapRegionSize 设置Region大小,范围是（1-32MB之间，2的幂）  
G1收集器会去跟踪各个Region里面的垃圾堆积的价值大小。价值是根据回收空间的大小和所需要回收的大概时间来计算。并且维护一个优先级列表。
使用参数-XX:MaxGCPauseMillis指定允许停顿的最大毫秒数，有限处理那些回收价值大的Region。
G1收集器的大致流程：  
1、初始标记：标记一下从GC Roots能直接关联的对象。耗时短，需要用户线程停止工作。（还是安全点，安全区域那一套）    
2、并发标记：从GC Roots进行可达性分析，递归扫描对象图。耗时长，可以和用户线程一起进行  
3、最终标记：短时间暂停用户线程。并发处理用户线程改变过的对象引用  
4、筛选回收：更新Region的统计数据，对各个Region进行回收价值和回收成本进行排序，根据用户期望的停顿时间制定回收计划    
把决定回收的Region里面存活的对象放到空的Region中，在清理掉原来的Region空间，这个步骤是必须暂停用户线程，由多个收集器线程一起完成的    
当把期望回收时间调的很低的时候，会导致收集的速度跟不上分配速度，垃圾的堆积导致FullGC的发生，通常这个停顿时间大概是100,200,300毫秒比较合理。    


**2004年Sun公司就发表了G1收集思想这样的论文，但是一直到2012年才有G1收集器的实现。为什么需要这么长的时间？至少有这些问题要解决。**  
1、Region和Region之间的引用如何解决？    
解决的思路还是使用卡表。但是复杂得多，每个Region都有维护自己的记忆集，并且卡表是双向的，记录了我指向谁，谁指向我。
G1至少要耗费堆内存的10%-20%来维护这些信息。
  
2、并发标记如何保证结果不被本地线程干扰？    
用户线程改变了引用关系导致标记结果出现错误
G1的采用的解决方案是原始快照，CMS采用的是增量更新。这同样会导致如果内存回收速度跟不上内存分配速度，G1收集器还是会冻结用户线程的执行，开始进行Full GC  
  
3、怎么确保停顿时间达到期望值？这个预测模型怎么建立？    
G1收集器是通过衰减均值的理论来实现的，统计每个Region回收的耗时，记忆集里面的脏卡数量，计算出平均值，标准方差，置信度等信息，这样就能确保停顿的时间不超过设置的期望值。  


**G1和CMS的对比**   
从G1开始，最先进的垃圾收集器的设计导向都不约而同的追求应付内存的分配速率。而不是一次性把整个Java堆清理干净。  
G1对比CMS有很多优点，可以设置停顿时间，按照收益去确定回收集合，Region这样的内存布局等等，整体上复制算法，解决内存碎块问题，其实他本身就是碎块。
但是G1的缺点也很明显，内存的占用这些额外的负载就比CMS要高很多。算法本身也就更加复杂，记忆集卡表的维护。为了实现原始快照的搜索算法，还要使用写前屏障来跟踪指针变化的情况等等。  
所以，在小内存的机器上尽量使用CMS，而大内存的应用上使用G1会更好，这个内存的平衡到哪大概是6G-8G左右。不过随着HotSpot的开发者对G1的偏爱，对G1不断的升级会让对比的结果向G1倾斜。  

  
### 低延时的垃圾收集器
#### 什么是完美的垃圾收集器？如何衡量？
内存占用、吞吐量、延迟。构成了一个不肯三角。  
计算机硬件的发展允许我们摒弃内存占用。毕竟能用钱解决的事情都不叫事情。  
初始标记、最终标记这些阶段是必须要停顿的。
##### Shenandoah
由RedHat公司开发的，在2014年贡献给了OpenJDK,后来成为JEP189，这个项目的目标是实现一种能子啊任何堆内存大小下都可以把垃圾收集的停顿时间限制在10毫秒之内的垃圾收集器。  
1、G1的回收阶段是支持并发整理的，但是不能做到与用户线程并发。而Shenandoah可以做到。    
2、Shenandoah不使用分代收集，不会有专门的新生代Region、老年代Region  
3、shenandoah摒弃了G1中耗费大量资源去维护的记忆集，改为“连接矩阵”的全局数据结构去维护跨Region引用的问题。降低了维护记忆集的消耗，也降低了伪共享的发生概率。  
连接矩阵简单可以理解为二维表格，Region N 有引用指向 Region M 在N行M列就会产生标记。  
Shenandoah工作流程分为9个阶段：  
(1) 初始标记:暂停用户线程。stop the world 停顿时间与堆大小无关，只与GC Roots的数量有关。  
(2) 并发标记：遍历对象图。可以和用户线程一起并发。时间长短取决于堆中存活的对象数量和图的复杂程度。  
(3) 最终标记：暂停用户线程。stop the world 统计出回收价值高的Region。  
(4) 并发清理：清理掉整个区域内一个存活对象都没有的Region。（瞬间完成了应付内存分配的需求）。  
(5) 并发回收：把回收集里面的存活对象复制一份到其他没有被使用的Region中。运行时间的长短取决于回收集合的大小。这个阶段是和用户线程一起执行的，要完成的难度相当大，因为用户线程还在不断的移动对象进行读写访问。    
(6) 初始引用更新：复制结束之后，还要把堆中指向旧对象的引用改为指向新对象。会产生短暂的停顿。其实这个阶段只是建立了一个线程集合点，确保所有并发回收阶段中进行的收集线程都完成了对象移动的任务而已。  
(7) 并发引用更新：和用户线程一起并发执行，只需要把旧的引用更新为新的引用就好。  
(8) 最终引用更新：修正存在于GC Roots中的引用，这个阶段还是会短暂停顿。  
(9) 并发清理：此时整个回收集中的Region再无存活对象，再次调用并发清理去回收这些内存空间。  

Brooks Pointer (转发指针)  
在应用程序并发的同时去复制对象。一门面向对象的语言，对象时时刻刻都在发生着改变。在这样的情况下安全的复制对象是很有难度的。  
在没有Brooks Point之前，采用的是一种对象的内存保护陷阱来完成的。   
当访问旧对象的时候，旧对象的内存空间产生自陷中段，进入预设好的异常处理器中，就会把访问转发到新的对象上。   
能够实现对象移动和线程的并发，但是需要操作系统层面的支持，就需要从用户态切换到核心态，代价很大，不能频繁使用。   

  
而Brooks Pointer是采用一种增加一个转发指针的引用字段的形式来完成的。当不处于并发移动的时候，这个指针指向的就是自己。    
当对象存在副本的时候，只需要修改这个转发指针的指向新副本对象，然后对象的访问转发到新的副本对象上即可。  
但是这个过程仍然需要考虑线程安全问题：    
收集线程建立对象副本 -> 用户线程写访问对象的某个字段 -> 收集线程更新转发指针指向新副本对象    
上面这个过程如果不加CAS控制是会出现线程安全问题的，导致的后果就是，新对象和原对象不一致。  
  
后面的**Shenandoah读写屏障看不懂**。前面的写屏障还好理解一点(AOP)。  
  
效率：运行时间对比G1来说总体要长一点，但平均停顿时间得到质的飞越。

##### ZGC
ZGC是一款基于Region内存布局的，暂时不设置分代，使用了读屏障，染色指针和内存多重映射等技术来实现的可并发的标记-整理算法，以低延迟为首要目标的垃圾收集器。  
**内存布局**   
小型Region容量固定为2MB,用于存放小于256KB的小对象；  
中型Region容量固定为32MB,用于存放256KB-4MB之间的对象；  
大型Region容量不固定，可以动态变化，用于放置4MB以上的大对象，每一个大Region中只有一个对象。大对象不会被重新分配。

**并发整理的实现**
Shenandoah使用的是转发指针和读屏障来实现。ZGC采用读屏障和染色指针。  
**染色指针**：看不太懂，大概是这样：    
将少量的标记信息存储在指针上。64位的硬件最大支持256TB内存，这只是理论上，64位的Linux虚拟机支持47位（128TB）的虚拟地址空间和46位的物理地址空间（64TB）。  
64位的windows系统值支持44位（16TB）  
Linux下64位指针的高18位不能用来寻址，剩下的46位取出4位用来标记（是否被移动过，是否只能通过finalize方法才能访问，marked0,marked1）  
这样只有42位能寻址，ZGC能够管理的内存将不会超过4TB   
除了这个限制之外，不能支持32位平台，也不能开启指针压缩（-XX:+UseCompressedOops）,但是带来的收益是相当可观的。  

ZGC的工作流程：  
并发标记：遍历对选哪个图做可达性分析，与Shenandoah不同，ZGC标记的指针不是在对象上进行的，标记阶段会更新染色指针中的Marked0,Marked1标志位。  
并发预备重新分配：针对全堆的标记，得到需要清理的Region有哪些。而不是像G1那样我维护记忆集，卡表。  
并发重新分配：把存活对象分配到新的Region上，维护了一个Forward Table记录了旧对象到新对象的转发关系。  
如果用户线程访问了对象，被内存屏障所截获，根据转发表将访问转发到新的对象上去，并且更新这个引用的值，这样下一次就不会被截获，不会去查转发表了。ZGC的这个行为叫做“自愈”。   
并发重映射：修正整个堆中指向重新分配集合中的所有引用。其实这个步骤是不必要的，因为自愈功能，所有的引用肯定都会被更新。所以ZGC把这个步骤放到了下一次GC进行并发标记的时候去完成。  

**ZGC的性能极好，是迄今为止垃圾收集器研究领域的最前沿成果**。  
它出世之时是JDK11时期，正好是Oracle调整许可授权，把商业特性都开源给了OpenJdk。遗憾的是还没有在正式的JDK版本中使用。  
    

##### Epsilon收集器
不会进行垃圾收集。
收集器的工作除了收集垃圾之外，还负责堆内存的管理布局，对象分配，与解释器、编译器、监控系统协作等。  
一个应用只需要运行数秒钟，Java虚拟机能正确分配内存、堆空间耗光时就退出，那Epsilon收集器就是最好的选择。  

### 怎样选择垃圾收集器？
影响答案的三个因素：
(1) 应用的主要关注点是什么？科学计算，数据分析，那么吞吐量就是主要关注点。SLA应用，停顿时间直接影响到服务质量，延迟就是关注点。客户端或者嵌入式应用，内存占用就是关注点。  
(2) 硬件条件
(3) JDK的发行商
(4) 如果系统跑在Windows上，无缘ZGC,只能尝试Shenandoah。
(5) 硬件设施和jdk版本落后，根据内存规模可以衡量。如果是4-64GB CMS一般能处理好，如果堆内存比较大，可以尝试G1。

### 垃圾收集日志
在JDK9之前，没有统一的日志处理框架。JDK9之后，使用Xlog参数进行设置。  
查看GC基本信息：9之前使用参数 -XX:+PrintGC  9之后使用参数-Xlog:gc  
查看GC详细信息：9之前使用参数 -XX:+PrintGCDetails 9之后使用参数 -X-log:gc*  
查看GC前后堆、方法区可用容量变化：9之前 -XX:+PrintHeapAtGC 9之后-Xlog:gc+heap=debug  
查看GC过程中用户线程并发时间以及停顿时间：9之前 -XX:Print-GCApplicationConcurrentTime 和 -XX:PrintGCApplicationStoppedTime 9之后使用 -Xlog:safepoint  


### 内存分配与回收策略
Java自动管理内存的目标就是：自动化的解决对象内存分配以及自动回收以及分配给对象的内存。  
对象优先在伊甸园中分配，当空间不足够的时候，将发生一次MinerGC。在垃圾收集的过程中对象分配空间不足，又会触发担保机制，把对象分配到老年代中。   
使用-XX:PretenureSizeThreshold参数，将大对象直接进入老年代，为了避免大对象在Eden和两个Survivor区间来回复制产生大量的内存复制操作。  
长期存活的对象将进入老年代，在每一个对象的对象头中都记录了分代的年龄计数器。
对象在Eden中诞生，经过了第一次MinerGC之后仍然存活就会被移动到Survivor区，对象年龄设置为1岁，对象在Survivor中每熬过一次MinerGC年龄就会加一，当年龄增加到15的时候，就会晋升到老年代中。（默认是15，这个阈值可以通过参数 -XX:MaxTenuringThreshold 调整）。  
如果在Survivor空间中相同年龄的所有对象所占空间大小的总和 大于 整个Survivor区域空间的一般 无需等待15次，年龄大于等于这些的对象将会一起被送入老年代。这更加符合分代收集理论的假说。  

**分配空间担保**  
发生MinerGC之前，必须检查老年代最大可用连续空间是否 大于 新生代区域所有对象的总空间。大于则代表MinerGC是安全的，如果不大于也不允许分配担保，就会进行一次Full GC。
如果允许分配担保，回去检查历史信息，看历次晋升老年代对象的平均大小 和 最大连续空间的大小，如果够，就进行这次有风险的MinerGC。
如果这次风险尝试失败了，还是会进行一次Full GC。
JDK6 之后对这个规则进行了改进：假如老年代的最大连续空间，大于新生代对象总大小 或者大于历次晋升的平均大小，就会进行MinerGC,否则进行FullGC。

## 三、性能监控和故障处理工具 (自己动手，靠经验堆出来的)

### 基础工具
jps:列出正在运行的虚拟机进程，显示执行主类名称，和本地虚拟机唯一ID。    
jstat: 显示本地或者远程虚拟机进程中的类加载，内存，垃圾收集，即时编译等运行时数据。 jstat -gc 10 2 每10毫秒查询一次，总共查询2次堆内存状况。    
jinfo: 查看和调整虚拟机各项参数的值 jinfo -flag pid   也可以使用java -XX:+PrintFlagsFinal 查看默认参数  
jmap: 用于生成堆转储快照。  在虚拟机发生内存溢出异常的时候自动生成堆转储快照文件 -XX:+HeapDumpOnOutOfMemoryError
jmap还可以用来查询finalize执行队列，Java堆和方法区的详细信息，空间使用率，目前采用的是什么收集器等等。  
jhat:虚拟机堆转储快照分析工具。生成一个微型的Http/web服务器，在浏览器上打开查看。不过一般不使用。  
jstack:堆栈跟踪工具。生成当前时刻的线程快照

### 可视化故障处理工具
JDK提供的：JConsole,JHSDB,VirsualVM,JMC四个。  
JConsole JDK5 免费    
JHSDB JDK9  免费 java -cp .\sa-jdi.jar sun.jvm.hotspot.HSDB  
VirsualVM JDK6 免费    
JMC 配合飞行记录，付费


# 第三部分 虚拟机执行子系统
## 第六章 类文件结构
代码编译的结果从本地机器码转变为字节码，是存储格式发展的一小步，却是编程语言发展的一大步。    
程序编译成二进制本地机器码已经不再是唯一的选择，越来越多的程序语言选择了与操作系统和机器指令集无关、平台中立的格式作为程序编译后的存储格式。  

### Class类文件的结构
一组以8个字节为基础单位的二进制流。    
(1) 头四个字节称为魔数，唯一作用是确定这个文件是否是一个能被虚拟机接收的文件。0XCAFEBABY。咖啡      
(2) 后面的四个字节是Class文件的版本号。jdk1.1能支持45.0-45.65535  jdk1.2能支持45.0-46.65535  jdk8能支持52.0  jdk13能支持到57.0  
(3) 紧跟着版本号的值常量池。主要存放两大类常量：字面量和符号引用。    
字面量：接近于常量的概念，文本字符串，被声明为final的常量值。    
符号引用，主要包括以下：  
被模块导出或者开放的包（Package）、类和接口的全限定名、字段的名称和描述符、方法的名称和描述符、方法句柄和方法类型、动态调用和动态常量  
(4) 访问标识：是否定义为public,是否是abstract类型，是类还是接口等等。  
(5) 类索引，父类索引，接口索引集合。计算的结果会放在常量池里面。  
(6) 字段表集合：描述接口或者类中声明的变量。   
(7) 方法表集合：一个方法的字节码指令长度不允许超过65535行。局部变量表和异常表。  
(8) 属性表集合。  


## 第七章 虚拟机类加载机制
Java虚拟机把描述类的数据从Class文件加载到内存，对数据进行校验、转换解析和初始化，这个过程就叫虚拟机的类加载机制。  
一个类型被加载到虚拟机内存中开始，到卸载出内存，生命周期是: 加载->[验证->准备->解析]->初始化->使用->卸载   
什么时候会开始加载一个类：new 关键字实例对象，读写取静态字段，反射，父类被动初始化等。

### 加载需要完成的事情？
1、通过一个类的全限定名来获取定义此类的二进制字节流。  
2、将这个字节流所代表的静态存储结构转化为方法区的运行时数据结构。  
3、在内存中生成一个代表这个类的Class对象，作为方法区这个类的各种数据的访问入口。  

### 验证的时候做的事情？
确保Class文件的字节流包含的信息不回危害虚拟机自身的安全。  
文件格式检验：以魔数0xCAFEBABY开头,主次版本号能在虚拟机接受的范围内，常量池中是否有不支持的类型等等。  
元数据分析：对字节码进行语义分析，是不是抽象类，有没有把需要实现的方法都写好了。是不是继承了被final修饰的类。对这些错误进行检查。  
字节码验证：检查方法里面代码的语法，类型使用是否正确等等。   
符号引用验证：常量池中的符号引用是不是能够正确的找到对应的类型等，这些类型是不是都能被访问，检查权限private protected这些。  
一般在生产上，可以通过参数 -XVerify:none去关闭大部分的验证，缩短虚拟机加载类的时间。  

### 准备阶段做了什么？
为类中定义的变量（被static修饰的变量）分配能存，并且初始化数值。而实例变量会随着对象实例化的时候随着对象一起分配在堆中。

### 解析阶段做了什么？
将常量池内的符号引用转换为直接引用。   
符号引用：一组符号来表述引用目标，一些字面量。    
直接引用：直接指向目标的指针、相对偏移量或者是一个能间接定位到目标的句柄。  

### 初始化做了什么？
这一阶段，虚拟机才会将执行代码的主导权交给应用程序。因为初始化就是执行类构造器<clinit>()的一个过程。这个方法是编译产物。
既然是执行代码，那么久一定有线程安全问题，所以虚拟机在加载的过程会进行同步加锁，其他线程会被阻塞。

### 类加载器
启动类加载器 BootstrapClassLoader  
扩展类加载器 ExtensionClassLoader  
应用程序类加载器 ApplicationClassLoader
**双亲委派模型**   
如果一个类加载器收到了加载类的请求，先不尝试去加载，而是把加载请求委派给父类加载器去完成，
父类加载器无法完成这个加载请求的时候才会反馈给子类加载器（负责的范围内没有搜索到所需的类），子类加载器才会去加载。  

## 第八章 虚拟机字节码执行引擎
### 运行时栈帧结构
栈帧StackFrame，是支持方法调用和方法执行的数据结构。每一个栈帧都包含了局部变量表，操作数栈，动态链接，方法返回地址和一些附加信息。    
在编译期间就能确定需要多大的局部变量表，需要多深的操作数栈。  

1、局部变量表：方法参数和方法内的局部变量。以变量槽为最小单位（slot）,long和double占两个，this也占一个，从下标0开始计算。  
2、操作数栈：栈的深度在编译时期就能确定了。通过压栈和出栈来进行计算。  
3、动态链接：指向运行时常量池中该栈帧所属方法的引用  
4、方法返回地址：存放调用该方法的PC寄存器的值  
5、附加信息，允许加入一些其他的信息，调试，性能收集  

### 方法调用
所有的方法调用在Class文件里面存储的都只是一个常量池中的符号引用。
invokestatic调用静态方法  
invokespecial调用实例构造器，私有方法和父类中的方法  
invokevirtual调用虚方法  
invokeinterface调用接口方法，在运行期再确定一个实现接口的对象  
invokedynamic运行时动态解析调用限定符锁引用的方法  
 
**解析调用**  
在编译的时候就已经知道要调用哪个方法，这个过程叫做解析。静态方法，私有方法。
在类加载的解析阶段就会把涉及的符号引用全部换成明确的直接引用，不用等到运行期再去完成。

**分派调用**    
重写方法。Java是一门静态多分派，动态单分派的语言。

### 基于栈的字节码解释执行引擎
Java虚拟机的执行引擎在执行Java代码的时候可以选择通过解释器执行和通过即时编译器产生的本地代码执行，这两种选择。也就是可以选择解释执行还是编译执行。  
解释执行还是编译执行只有虚拟机自己去做决定。    
基于栈的字节码解释执行，整个运算过程的中间变量都是以操作数栈的出栈，入栈作为信息交换的途径。    
  
<hr>

# 第四部分 程序编译与代码优化
从计算机诞生的那一天起，对效率的追逐就是程序员天生的坚定信仰，这个过程犹如一场没有终点、永不停歇的F1方程式竞赛，程序员是车手，技术平台测试在赛道上飞驰的赛车。  
## 第十章 前端编译优化
关注点是“代码写的对不对”
一些有代表性的编译器：  
前端编译器：JDK的javac、Eclipse JDT的增量式编译器(EJC)    
即时编译器：C1,C2,Graal编译器  
提前编译器：JDK的Jaotc  
Java团队把对性能优化集中到运行时的即时编译器，这样非Java语言在JVM上运行也能享受到性能的优化。  

## Javac编译器
语法糖：不提供实质性的代码功能改进，能提升可读性，减少编码出错的概率。  
### 泛型
本质是参数化类型，可以将操作类型指定为方法签名中的一些参数，能够运用在类、接口、方法的的创建中。   
Java语言的泛型实现方式叫做擦除式泛型。c#则是具现化泛型。  

## 第十一章 后端编译优化
编译器在任何时间、任何状态下吧Class文件转换成本地基础设施相关的二进制字节码，整个过程就可以视为后端编译。  
### 即时编译器
Java程序最初都是通过解释器进行解释执行的，当虚拟机发现某个方法或代码块的运行特别频繁，就会把这些代码认定为“热点代码 Hot Spot Code”，虚拟机将会把这些热点代码编译成本地机器码，用各种手段尽可能的进行代码优化，完成这部分工作的后端编译器称为即时编译器。  
#### 解释器和编译器
当程序需要快速启动和执行的时候，解释器可以迅速发挥作用。后续编译器发挥作用，把代码编译称为机器码，减少解释器的中间损耗，提升效率。
解释器可以作为编译器的逃生门。  
HotSpot虚拟机内置三个即时编译器。客户端编译器Client Compiler C1,服务端编译器 Server Compiler C2。  
第三个则是在JDK10才出现的，长期目标是替换C2的Graal编译器。  

解释模式：虚拟机仅仅使用解释器运行。   
编译模式：虚拟机仅仅使用编译器运行。   
混合模式：解释器和编译器同时运行。   

编译器需要占用时间去进行本地代码的优化，并且占用时间越多，代码优化的质量越高。并且解释器还要替编译器开启一些监控信息。为了从中寻找平衡，虚拟机采用了**分层编译**的方法：    
第0层：仅仅解释器运行。  
第1层：开始客户端编译器，解释器不开启性能监控。  
第2层：开启客户端编译器，解释器开启方法和回边次数的性能监控。  
第3层：开始客户端比阿尼，解释器多开启分支跳转，虚方法调用版本等的新能监控，统计收集信息。  
第4层：开启服务端编译器，耗时更长，性能也更好，还会根据监控信息进行一些不可靠的激进优化。  

### 即时编译对象和触发条件
热点代码：多次重复调用的方法和多次执行的循环体。  
**怎么才算多次？是否需要触发热点探测？**两种检测思路：基于采样的热点探测和基于计数器的热点探测。  
采样热点探测：J9 检测处于栈顶的方法。简单高效，缺点是难以精确定位热度，还容易受到线程阻塞等原因的影响。  
计数器热点探测：HotSpot 虚拟机为每个方法或者是代码块建立计数器。统计执行次数，当超过一定阈值就认为是热点代码。难以维护，但是更加精确。   
HotSpot虚拟机为每个方法都生成了调用计数器和回边计数器（在循环边界往循环体里面跳转）。计数器的值达到阈值，客户端模式下默认是1500，服务端模式下默认是10000，就会触发即时编译器工作。  
可以通过 -XX:CompileThreshold 设置这个次数 所以方法的调用变成了 判断有没有编译后版本 - 判断计数器的值与阈值这样的过程。  
  
准确的来说，计数器统计的是一段时间之内方法执行的频率。若在一段时间内改方法不足以交给即时编译器去处理，那么就会将计数器的值减少一半。这个时间长度就叫半衰周期。  
可以通过-XX:-UseCounterDecay来设置关闭这个热度衰减， -XX:CounterHalfLifeTime来设置半衰周期时间。

### 提前编译
在运行之前把代码直接翻译成机器码保存起来的静态翻译工作。另外一种是把即时编译器在做的编译工作提前做好并且保存下来。

### 编译器优化技术
方法内联：消除调用成本，简单理解就是把代码复制。  
逃逸分析：分析方法的作用域。对象在方法里面定义，作为参数传递到其他方法中，称为方法逃逸。赋值给其他线程中访问的实例变量，称为线程逃逸。根据是否逃逸就能对代码做出优化。栈上分配，标量替换，同步消除。  
公共子表达式消除：表达式变量没有被替换。不需要花时间重新计算。  
数组边界检查消除：通过数据流分析循环变量在[0,length]之间，就可以取消判断语句。  

# 第五部分 高效并发
并发处理的广泛应用是Amdahl定律代替摩尔定律成为计算机性能发展源动力的根本原因，也是人类压榨计算机运算能力的最有力武器。计算机的运算速度与他的存储和通讯子系统的速度差异太大了。   
TPS:代表一秒钟内服务端平均能响应的请求总数。   

简单记录，大部分内容在JUC并发编程里面都有。  

CPU和Java的即时编译器都会对代码进行排序优化。   
JMM内存模型：线程的工作内存中保存了被该线程使用的变量在主内存中的副本。线程对数据的操作都在自己的工作内存中解决。  

变量对所有线程可见，当一条线程修改了变量的值，其他线程能立即得知。volatile在并发下是不安全的。只保证有序性和内存可见性，不保证原子性。  
禁止指令优化重排序，使用的是内存屏障。  

long和double的非原子协定：允许虚拟机将没有被volatile修饰的64位数据的读写操作划分为两次32位的操作来进行。64位虚拟机不会出现非原子性访问。  

HotSpot的每一个线程都直接映射到一个操作系统原生线程来实现，HotSpot自己不会去干涉线程调度的，全部交给操作系统去处理。  
Java采用抢占式的线程调度模式。  
用户态与核心态的切换就叫线程上下文切换。  
  
线程安全：当多个线程访问一个对象的时候，如果不考虑这些线程在运行时环境下的调度和交替执行，也不需要进行额外的同步，或者在调用方进行任何其他的协调操作，调用这个对象的行为都可以获得正确的结果，那就成这个对象是线程安全的。  

互斥同步，非阻塞同步。  
锁消除，逃逸分析。  
锁粗化，反复对一个对象加锁。  
轻量级锁：不存在多线程竞争的时候，减少重量级锁使用操作系统互斥产生的消耗。对象头的信息，GC分代年龄，哈希码，偏向线程ID,锁标志位等信息。
