package min.test.akka

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

class Master extends Actor {
	override def receive: Receive = {
		case "hi" => {
			print("收到: hi")
			sender ! "nihao"
		}
	}
}

object Master {
	def main(args: Array[String]): Unit = {
		val host = "127.0.0.1"
		val port = 8888
		//创建ActorSystem的必要参数
		val configStr =
			s"""
			   |akka.actor.provider = "akka.remote.RemoteActorRefProvider"
			   |akka.remote.netty.tcp.hostname = $host
			   |akka.remote.netty.tcp.port = $port
			""".stripMargin
		val conf = ConfigFactory.parseString(configStr)
		//ActorSystem是单例的，用来创建Actor
//		val as = ActorSystem.create("MasterActorSystem", conf)
		//ConfigFactory.load会默认读取src/main/resources下的文件
		val as = ActorSystem.create("MasterActorSystem", ConfigFactory.load("master.conf"))
		//启动Actor，Master会被实例化，生命周期方法会被调用
		val actor = as.actorOf(Props[Master], "Master")
//		actor ! "hi"

//		as.whenTerminated
	}
}
