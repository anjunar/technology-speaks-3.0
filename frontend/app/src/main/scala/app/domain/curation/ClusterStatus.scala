package app.domain.curation

object ClusterStatus {
  val Offen = "Offen"
  val InBearbeitung = "InBearbeitung"
  val Entscheidungsreif = "Entscheidungsreif"
  val Abgeschlossen = "Abgeschlossen"

  val values: Seq[String] = Seq(Offen, InBearbeitung, Entscheidungsreif, Abgeschlossen)
}
