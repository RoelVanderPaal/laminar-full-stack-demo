package com.raquo.weather.ecapi

import ru.tinkoff.phobos.decoding.{*, given}
import ru.tinkoff.phobos.encoding.{*, given}
import ru.tinkoff.phobos.syntax.{*, given}
import ru.tinkoff.phobos.derivation.semiauto.{*, given}

import java.time.LocalDateTime

case class CityStationReportXml(
  currentConditions: Option[CurrentConditionsXml],
  forecastGroup: Option[ForecastGroupXml]
)

case class CurrentConditionsXml(
  dateTime: List[DateTimeXml],
  iconCode: IconCodeXml,
  temperature: TemperatureXml,
  pressure: PressureXml,
  relativeHumidity: RelativeHumidityXml,
  wind: WindXml
)

case class ForecastGroupXml(
  dateTime: List[DateTimeXml],
  forecast: List[ForecastXml],
)

case class ForecastXml(
  period: ForecastPeriodXml,
  textSummary: TextSummaryXml,
  cloudPrecip: CloudPrecipXml
)

case class ForecastPeriodXml(
  @attr textForecastName: String, // e.g. "Tonight", "Wednesday"
  @text dayOfWeekForecastName: String // e.g. "Tuesday night", "Wednesday"
)

case class TextSummaryXml(
  @text text: String // e.g. "Becoming cloudy this evening with 60 percent chance of drizzle this evening and overnight."
)

case class CloudPrecipXml(
  textSummary: TextSummaryXml
)

case class AbbreviatedForecastXml(
  iconCode: IconCodeXml,
  pop: PopXml,
  textSummary: TextSummaryXml
)

case class PopXml(
  @attr units: String, // "%"
  @text value: String // e.g. "60"
)

case class DateTimeXml(
  @attr name: String, // "observation" | "forecastIssue"
  @attr zone: String,
  @attr UTCOffset: String,
  year: Int,
  month: Int,
  day: Int,
  hour: Int,
  minute: Int
) {

  def toLocalDateTime: LocalDateTime = {
    LocalDateTime.of(year, month, day, hour, minute)
  }
}

case class IconCodeXml(
  @attr format: String,
  @text code: String
)

case class TemperatureXml(
  @attr unitType: String,
  @attr units: String,
  @text value: String
)

case class PressureXml(
  @attr unitType: String,
  @attr units: String,
  @text value: String
)

case class RelativeHumidityXml(
  @attr units: String,
  @text value: String
)

case class WindXml(
  speed: WindSpeedXml,
  direction: WindDirectionXml,
  bearing: WindBearingXml
)

case class WindSpeedXml(
  @attr unitType: String,
  @attr units: String, // "km/h"
  @text value: String // Could be a numeric value in units, but also e.g. "Calm"
)

case class WindDirectionXml(
  @text value: String // e.g. "S", "WNW", "VR", ""
)

case class WindBearingXml(
  @attr units: String, // "degrees"
  @text value: String // Could it be a string ???
)

// -- Converters --

// extension(CityStationReportXml)

// -- XML decoders --

implicit val WindSpeedDecoder: ElementDecoder[WindSpeedXml] = deriveElementDecoder

implicit val WindDirectionDecoder: ElementDecoder[WindDirectionXml] = deriveElementDecoder

implicit val windBearingDecoder: ElementDecoder[WindBearingXml] = deriveElementDecoder

implicit val windDecoder: ElementDecoder[WindXml] = deriveElementDecoder

implicit val temperatureDecoder: ElementDecoder[TemperatureXml] = deriveElementDecoder

implicit val relativeHumidityDecoder: ElementDecoder[RelativeHumidityXml] = deriveElementDecoder

implicit val pressureDecoder: ElementDecoder[PressureXml] = deriveElementDecoder

implicit val iconCodeDecoder: ElementDecoder[IconCodeXml] = deriveElementDecoder

implicit val textSummaryDecoder: ElementDecoder[TextSummaryXml] = deriveElementDecoder

implicit val cloudPrecipDecoder: ElementDecoder[CloudPrecipXml] = deriveElementDecoder

implicit val ForecastPeriodDecoder: ElementDecoder[ForecastPeriodXml] = deriveElementDecoder

implicit val DateTimeDecoder: ElementDecoder[DateTimeXml] = deriveElementDecoder

implicit val ForecastDecoder: ElementDecoder[ForecastXml] = deriveElementDecoder

implicit val ForecastGroupDecoder: ElementDecoder[ForecastGroupXml] = deriveElementDecoder

implicit val CurrentConditionsDecoder: ElementDecoder[CurrentConditionsXml] = deriveElementDecoder

implicit val CityStationReportDecoder: XmlDecoder[CityStationReportXml] = deriveXmlDecoder("siteData")
