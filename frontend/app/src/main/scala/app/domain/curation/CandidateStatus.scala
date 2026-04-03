package app.domain.curation

object CandidateStatus {
  val Eingang = "Eingang"
  val InPruefung = "InPruefung"
  val Rueckfrage = "Rueckfrage"
  val Zugeordnet = "Zugeordnet"
  val Uebernommen = "Uebernommen"
  val Verworfen = "Verworfen"
  val Zurueckgestellt = "Zurueckgestellt"

  val values: Seq[String] = Seq(Eingang, InPruefung, Rueckfrage, Zugeordnet, Uebernommen, Verworfen, Zurueckgestellt)
}
