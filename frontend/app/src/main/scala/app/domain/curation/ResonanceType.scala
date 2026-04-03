package app.domain.curation

object ResonanceType {
  val Impuls = "Impuls"
  val Frage = "Frage"
  val Einwand = "Einwand"
  val Ergaenzung = "Ergaenzung"
  val Erfahrung = "Erfahrung"
  val Verweis = "Verweis"
  val Verdichtung = "Verdichtung"

  val values: Seq[String] = Seq(Impuls, Frage, Einwand, Ergaenzung, Erfahrung, Verweis, Verdichtung)
}
