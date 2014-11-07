package controllers

import akka.actor._
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object CastActor {
  def props(out: ActorRef, name: String, opponent: String) = Props(new CastActor(out, name, opponent))
}

class CastActor(out: ActorRef, name: String, opponent: String) extends Actor {
  val opponentSelection: ActorSelection =  Akka.system.actorSelection(s"user/$opponent")
  var game: Option[ActorRef] = None

  override def receive = {
    case StartGame(None) =>
      opponentSelection ! LetsPlay(self)
    case LetsPlay(opponentRef) =>
      game = Some(context.actorOf(GameActor.props(self, opponentRef)))
    case StartGame(Some(gameActor)) => game = Some(gameActor)
      game.map(_ ! "startGame" -> Player(name, self))
    case spell: IncomingSpell =>
      game.map(_ ! spell -> name)
    case OutgoingMessage(msg) =>
      out ! msg
  }

  override def postStop(): Unit = {
    game.map(_ ! PoisonPill)
  }
}

case class IncomingSpell(message: String)

case class OutgoingMessage(message: String)

case class StartGame(game: Option[ActorRef])

case class LetsPlay(player: ActorRef)