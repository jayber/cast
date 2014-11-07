package controllers

import akka.actor._
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.libs.json.Json
import play.api.mvc.WebSocket
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object Socket {
  def cast = WebSocket.acceptWithActor[String, String] { request => out =>
    val user = request.session(Application.user_key)
    val opponent = request.session.get(Application.opponent_key)
    SocketActor.props(out, user, opponent)
  }
}

object SocketActor {
  def props(out: ActorRef, user: String, opponent: Option[String]) = Props(new SocketActor(out, user, opponent))
}

class SocketActor(out: ActorRef, user: String, opponent: Option[String]) extends Actor {
  val houseKeeper = Akka.system.actorOf(HouseKeeper.props(user, out), "housekeeper="+user)
  val player = opponent match {
    case Some(opponentName) =>
      val myActor = Akka.system.actorOf(CastActor.props(out, user, opponentName), user)
      Logger.debug(s"Creating new CastActor - user: $user, myActor: ${myActor.path}")
      Some(myActor)
    case None => None
  }

  override def postStop() = {
    player.map(_ ! PoisonPill)
    houseKeeper ! PoisonPill
  }

  def receive = {
    case "sendOpponents" => houseKeeper ! "sendOpponents"
    case "startGame" =>
      player.map(_ ! StartGame(None))
    case msg: String => if (!msg.isEmpty) {
      Logger.debug(s"socket received: $msg")
      player.map(_ ! IncomingSpell(msg))
    }
  }
}

object HouseKeeper {
  def props(name: String, out: ActorRef) = {
    Props(new HouseKeeper(name, out))
  }
}

class HouseKeeper(name: String, out: ActorRef) extends Actor {
  override def receive: Receive = {
    case "sendOpponents" =>
      Logger.debug("received sendOpponents")
      val siblings = context.actorSelection("../*")
      siblings ! "sendName" -> out
    case ("sendName",target: ActorRef) =>
      if (sender!=self) {
        Logger.debug("received sendName")
        target ! Json.obj("opponent"->name).toString()
      }
  }
}