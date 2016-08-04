package arcade

import play.api.data.validation.ValidationError
import play.api.libs.json._

object Errors {
  case object DuplicateAdmiral         extends Error
  case object AdmiralNotFound          extends Error

  case object DuplicateAnchor          extends Error
  case object AnchorNotFound           extends Error

  case object DuplicateSpotting        extends Error
  case object SpottingNotFound         extends Error
  case object TooShortSpottingInterval extends Error
  case object AnchorAlreadySpotted     extends Error
  case object AnchorAlreadySunk        extends Error

  case object Maintenance              extends Error

  case class JsonParseError(jsError: JsError) extends Error {
    override def toJson: JsObject = super.toJson.deepMerge(jsError.toJson)
  }
  object JsonParseError {
    def apply(errors: Seq[(JsPath, Seq[ValidationError])]): JsonParseError = JsonParseError(JsError(errors))
  }

  sealed trait Error {
    this: Product =>

    def code: String = productPrefix
    def toJson: JsObject = Json.obj("code" -> code)
  }

  private implicit val validationErrorWrites: Writes[ValidationError] = new Writes[ValidationError] {
    override def writes(o: ValidationError): JsValue = Json.obj(
      "messages" -> o.messages,
      "args"     -> o.args.map(_.toString)
    )
  }

  private implicit class JsErrorOps(val self: JsError) extends AnyVal {
    def toJson: JsObject = {
      Json.obj(
        "errors" -> self.errors.map { case (jsPath, error) =>
          Json.obj(
            "path" -> jsPath.toString,
            "error" -> error.map(Json.toJson(_))
          )
        }
      )
    }
  }

}
