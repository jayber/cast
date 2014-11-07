package controllers

import akka.actor.ActorRef
import play.api.libs.json.Json

trait State

case class Waiting(firstPlayerOption: Option[Player], game: ActorRef) extends State {
  def startGame(newPlayer: Player): State = {
    firstPlayerOption match {
      case None =>
        Waiting(Some(newPlayer), game)
      case Some(firstPlayer) if (firstPlayer != newPlayer) =>
        GameActor.scheduleTick(game)
        GameActor.tellPlayers(Json.obj("message"->"Play!"), firstPlayer, newPlayer)
        Playing(firstPlayer, newPlayer)
      case _ => this
    }
  }
}


