package app.domain.curation

class ClassifyCandidateRequest(
  var resonanceType: String = ""
)

class AssignTargetRequest(
  var documentId: String = "",
  var sectionId: String | Null = null
)

class AddCandidateRequest(
  var candidateId: String = ""
)

class WriteSummaryRequest(
  var summary: String = ""
)

class DecisionNoteRequest(
  var note: String = ""
)

class CreateClusterRequest(
  var title: String = ""
)
