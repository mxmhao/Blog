## 一个非常简单的线程锁，使用方便，[源码](https://github.com/mxmhao/iOS_App_Template/blob/master/Utils/XMLock.h)
```
/*
 --------------使用---------------
 //创建锁
 static XMLock lock;//保证某个作用域内唯一
 static dispatch_once_t onceToken;
 dispatch_once(&onceToken, ^{
    lock = XM_CreateLock();
 });
 
 方式一：注意别嵌套成死锁
 XM_Lock(lock);
 ... //中间不要有return、break、continue、throw、goto...等中断语句，否则XM_UnLock不能被执行
 XM_UnLock(lock);
 
 
 方式二：注意别嵌套成死锁
 id obj;
 //推荐用法
 XM_OnThreadSecure(lock, [arr addObject:obj]);
 XM_OnThreadSecure(lock, [arr removeObject:obj]);
 
 //不推荐这么使用，影响阅读，可以使用方式一
 XM_OnThreadSecure(
    lock,
    int v = arr.count;
    [arr addObject:obj];
    sleep(3);//这么使用可以测试锁是否有效
 );
 NSLog(@"%d", v);
 */

#ifndef XMLock_h
#define XMLock_h

#include <dispatch/dispatch.h>

#define XM_CreateLock() dispatch_semaphore_create(1)
#define XM_Lock(x) dispatch_semaphore_wait(x, DISPATCH_TIME_FOREVER)
#define XM_UnLock(x) dispatch_semaphore_signal(x)

typedef dispatch_semaphore_t XMLock;

/**
 简单包裹
 @param lock XMLock
 @param x 任意表达式，表达式中不要有return、break、continue、throw、goto...等中断语句，否则XM_UnLock不能被执行
 */
#define XM_OnThreadSafe(lock, x) \
XM_Lock(lock); \
x; \
XM_UnLock(lock)

#endif /* XMLock_h */
```
