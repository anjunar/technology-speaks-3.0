package com.anjunar.technologyspeaks.curation

import jakarta.json.bind.annotation.JsonbProperty

import scala.annotation.meta.field

class ClassifyCandidateRequest(
  @(JsonbProperty @field)
  var resonanceType: String = ""
)

class AssignTargetRequest(
  @(JsonbProperty @field)
  var documentId: String = "",
  @(JsonbProperty @field)
  var sectionId: String = null
)

class AddCandidateRequest(
  @(JsonbProperty @field)
  var candidateId: String = ""
)

class WriteSummaryRequest(
  @(JsonbProperty @field)
  var summary: String = ""
)

class DecisionNoteRequest(
  @(JsonbProperty @field)
  var note: String = ""
)

class CreateClusterRequest(
  @(JsonbProperty @field)
  var title: String = ""
)
