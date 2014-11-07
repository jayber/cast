package controllers

case class PlayingPlayer(player: Player, spellBuffer: Seq[Option[String]], score: Int) {

  def cast(spell: String) = {
    val cost = spell.length
    spellBuffer(0) match {
      case None => score - cost match {
        case newScore if newScore >= 0 => PlayingPlayer(player, spellBuffer.updated(0, Some(spell)), newScore)
        case newScore if newScore < 0 => this
      }
      case _ => this
    }
  }

  def advanceScore = {
    copy(score = score + Playing.TICK_ENERGY)
  }

  def advanceBuffer = {
    copy(spellBuffer = None +: spellBuffer.dropRight(1))
  }

  def name = {
    player.name
  }
}
