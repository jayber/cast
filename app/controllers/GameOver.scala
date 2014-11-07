package controllers

import play.api.libs.json.{Writes, JsValue, Json}

case class GameOver(won: PlayingPlayer, lost: PlayingPlayer) extends State {
  def tellPlayers() {
    GameActor.tellPlayers(Json.toJson(this), won.player, lost.player)
  }

  implicit val gameOverWrites = new Writes[GameOver] {
    override def writes(o: GameOver): JsValue = {
      Json.obj("winner" -> o.won.name)
    }
  }
}
