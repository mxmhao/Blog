# iOS屏幕旋转控制，极简
iOS屏幕旋转控制的简单实现，使用方式也非常简单，需要控制旋转的UIViewController遵守ShouldNotAutorotate协议即可，如：
```
MyUIViewController : UIViewController <ShouldNotAutorotate>
```
此方式适用于部分需要控制旋转，而部分不需要控制旋转的项目，有想法的自行修改。[源码](https://github.com/mxmhao/iOS_App_Template/tree/master/Template/ShouldNotAutorotate)
```
//  UIViewController+ShouldNotAutorotate.h
#import <UIKit/UIKit.h>

/** 使用此协议的UIViewController不要重写shouldAutorotate和supportedInterfaceOrientations方法，否则旋转的控制就会失效 */
@protocol ShouldNotAutorotate @end

@interface UIViewController (ShouldNotAutorotate)

@end


//  UIViewController+ShouldNotAutorotate.m
#import "UIViewController+ShouldNotAutorotate.h"
#import <objc/runtime.h>

@implementation UIViewController (ShouldNotAutorotate)

+ (void)load
{
    Method sa = class_getInstanceMethod(self, @selector(shouldAutorotate));
    Method xm_sa = class_getInstanceMethod(self, @selector(xm_shouldAutorotate));
    method_exchangeImplementations(sa, xm_sa);

    Method sio = class_getInstanceMethod(self, @selector(supportedInterfaceOrientations));
    Method xm_sio = class_getInstanceMethod(self, @selector(xm_supportedInterfaceOrientations));
    method_exchangeImplementations(sio, xm_sio);
}

- (BOOL)xm_shouldAutorotate
{
    if ([self conformsToProtocol:@protocol(ShouldNotAutorotate)]) return NO;
    return [self xm_shouldAutorotate];//返回原来的结果
}

- (UIInterfaceOrientationMask)xm_supportedInterfaceOrientations
{
    if ([self conformsToProtocol:@protocol(ShouldNotAutorotate)]) return UIInterfaceOrientationMaskPortrait;
    return [self xm_supportedInterfaceOrientations];//返回原来的结果
}

@end
```
