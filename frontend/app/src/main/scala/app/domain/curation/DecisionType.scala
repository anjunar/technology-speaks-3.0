package app.domain.curation

object DecisionType {
  val Uebernommen = "Uebernommen"
  val Verworfen = "Verworfen"
  val Zurueckgestellt = "Zurueckgestellt"
  val Rueckfrage = "Rueckfrage"
  val Umklassifiziert = "Umklassifiziert"

  val values: Seq[String] = Seq(Uebernommen, Verworfen, Zurueckgestellt, Rueckfrage, Umklassifiziert)
}
