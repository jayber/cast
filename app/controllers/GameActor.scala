package controllers

import akka.actor.{ActorRef, Props, Actor}
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsValue, Json}

object GameActor {
  val TICK_PERIOD = Tweakables.TICK_PERIOD

  def scheduleTick(gameActor: ActorRef) = {
    Akka.system.scheduler.scheduleOnce(TICK_PERIOD, gameActor, "tick")
  }

  def props(player1: ActorRef, player2: ActorRef) = Props(new GameActor(player1, player2))

  def tellPlayers(jsonMessage: JsValue, player1: Player, player2: Player) {
    tellPlayers(jsonMessage.toString(), player1, player2)
  }

  def tellPlayers(message: String, player1: Player, player2: Player) {
    val value = OutgoingMessage(message)
    player1.ref ! value
    player2.ref ! value
  }
}

class GameActor(player1: ActorRef, player2: ActorRef) extends Actor {
  var state: State = Waiting(None,self)
  player1 ! StartGame(Some(self))
  player2 ! StartGame(Some(self))

  override def receive = {
    case ("startGame", player: Player) => state match {
      case waiting: Waiting => state = waiting.startGame(player)
      case _ => state = Waiting(None,self).startGame(player)
    }
    case (IncomingSpell(spell), playerName: String) => state match {
      case playing: Playing => state = playing.castSpell(spell, playerName)
      case _ => //ignore
    }
    case "tick" => state match {
      case playing: Playing => state = playing.tick()
      state match {
        case Playing(_) => GameActor.scheduleTick(self)
        case _ =>
      }
      case _ => //ignore
    }
  }

}

case class Player(name: String, ref: ActorRef)
