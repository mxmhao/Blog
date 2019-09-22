# UIPresentationController使用 -- 仿UIAlertController
## 吐槽模式
最近看了很多关于UIPresentationController的文章，个人表示太难看懂了，他们的自己封装的太多了，不利于快速学习，对于这类知识不需要太多的写封装，又不是专门教架构和封装，跳来跳去的眼睛都花了，真是蛋疼！

## 教授模式
当父UIViewController调用present(_:animated:completion:)来呈现子UIViewController过程中，会使用UIPresentationController来控制转场的，所以我把所有的自定义UIPresentationController的都放在了子UIViewController中。  
直接看我写的子UIViewController：
```
//此视图控制器是将要 被 呈现的
class TestViewController: UIViewController, UIViewControllerTransitioningDelegate {
//UIViewControllerTransitioningDelegate
//这个代理是必须的，用来控制转场动画，个人觉得TestViewController的呈现动画由TestViewController自己去实现是最好的，
//所以我用TestViewController继承了UIViewControllerTransitioningDelegate代理

  init() {
    super.init(nibName: nil, bundle: nil)
    //自定义呈现，这两个操作必须放在init方法中
    modalPresentationStyle = .custom  //这个很重要
    transitioningDelegate = self //UIViewControllerTransitioningDelegate的代理
  }

  required init?(coder aDecoder: NSCoder) {
    super.init(coder: aDecoder)
    fatalError("init(coder:) has not been implemented")
  }

  deinit {
    print("TestViewController -- 释放")
  }

  override func viewDidLoad() {
    super.viewDidLoad()
    view.layer.cornerRadius = 10    //只设置这一个，自己会有剪切，子视图不会被剪切
  }

  @IBAction func dismissAction(_ sender: Any) {
    dismiss(animated: true)
  }

  //这个是UIViewControllerTransitioningDelegate中的方法
  public func presentationController(forPresented presented: UIViewController, presenting: UIViewController?, source: UIViewController) -> UIPresentationController? {
    let pc = XMAlertPresentationController(presentedViewController: presented, presenting: presenting)
    return pc//返回自定义的UIPresentationController
  }
}
```
然后再看自定义的UIPresentationController
```
//present or dismiss 两个过程实现
class XMAlertPresentationController: UIPresentationController {
  var view: UIView?      //此试图包含一个点击手势
  var bgView: UIView?    //不能交互的视图

  deinit {
    print("XMAlertSheetPresentationController -- 释放")
  }

  //呈现动画将要开始
  override func presentationTransitionWillBegin() {
    //
    view = UIView(frame: (containerView?.bounds)!)
    view?.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(dismissAction)))
    view?.backgroundColor = UIColor(white: 0, alpha: 0)    //这个一定要的，不然下面的动画就没效果了
    containerView?.addSubview(view!)
    bgView = UIView(frame: (containerView?.bounds)!)
    bgView?.isUserInteractionEnabled = false
    containerView?.addSubview(bgView!)
    //通过使用「负责呈现」的 controller 的 UIViewControllerTransitionCoordinator，我们可以确保我们的动画与其他动画一道儿播放。
    //背景色变动画，使用present or dismiss默认的动画实现
    guard let transitionCoordinator = presentingViewController.transitionCoordinator else {
      return
    }

    transitionCoordinator.animate(alongsideTransition: {(context: UIViewControllerTransitionCoordinatorContext!) -> Void in//动画0.4秒
      self.view?.backgroundColor = UIColor(white: 0, alpha: 0.4)
    })
  }

  //呈现动画已结束
  override func presentationTransitionDidEnd(_ completed: Bool) {
    // 如果呈现没有完成，那就移除背景 View，没有完成就是出了错误
    if !completed {
      view?.removeFromSuperview()
      bgView?.removeFromSuperview()
    }
  }

  //消失动画将要开始
  override func dismissalTransitionWillBegin() {
    //背景色变动画，使用present or dismiss默认的动画实现
    guard let transitionCoordinator = presentingViewController.transitionCoordinator else {
      self.view?.backgroundColor = UIColor(white: 0, alpha: 0.0)
      return
    }
    transitionCoordinator.animate(alongsideTransition: {(context: UIViewControllerTransitionCoordinatorContext!) -> Void in
      self.view?.backgroundColor = UIColor(white: 0, alpha: 0.0)
    })
  }

  //消失动画已结束
  override func dismissalTransitionDidEnd(_ completed: Bool) {
    if completed {
      view?.removeFromSuperview()
      bgView?.removeFromSuperview()
    }
  }

  //计算presentedView的frame
  override var frameOfPresentedViewInContainerView: CGRect {
    let size = containerView!.bounds.size//containerView是转场容器视图的，这里的size相当于mainScreen的size
    if presentedViewController.preferredInterfaceOrientationForPresentation.isLandscape {//竖屏
      let width = size.height - 20
      return CGRect(x: (size.width - width)/2.0, y: size.height - 270, width: width, height: 260)
    }
    return CGRect(x: 10, y: size.height - 270, width: size.width - 20, height: 260)
  }

  //当前横竖屏变换时调用，调整自己写的视图
  open override func containerViewWillLayoutSubviews() {
    view?.frame = containerView!.frame
    bgView?.frame = containerView!.frame
    //当屏幕旋转后presentedView的frame需要自己调整，所以下面一行是必须的，presentedView == TestViewController.view这就明白了吧
    presentedView?.frame = frameOfPresentedViewInContainerView  //这行是必须的
  }

  func dismissAction() {//点击消失
    presentedViewController.dismiss(animated: true)
  }
}
```
如何使用测试：
```
let vc = TestViewController();
self.present(vc, animated: true)
```
是不是使用起来很简单？  
你可以打印XMAlertPresentationController中的presentedViewController和presentingViewController看看是什么类型
## 贤者模式
写完了，看着是不是很简单，把上面的两个类直接考到文件里就可使用，下面是效果图，[源码](https://github.com/mxmhao/PresentationController)
