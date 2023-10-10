package com.raquo.weather

import cats.effect.IO
import cats.syntax.all.*
import com.raquo.data.ApiResponse
import com.raquo.weather.ecapi.{CityStationReportXml, DateTimeXml}
import org.http4s.*
import org.http4s.client.Client
import org.http4s.implicits.*

import scala.concurrent.duration.*
import scala.util.{Failure, Success, Try}

class WeatherFetcher(httpClient: Client[IO]) {

  /**
   * @param cityStationId [[CityStation]] ID
   * @return (cityStationId, reportXml). IO can fail with ApiError
   */
  def fetchCityWeather(cityStationId: String): IO[ApiResponse[(String, CityStationReportXml)]] = {
    //val x = EntityDecoder.decodeBy[IO, String](MediaRange.`application/*`) { y =>
    //  DecodeResult.success(y.body)
    //}
    // #TODO implement an XML codec that takes care of error handling (what about 40x / 50x responses?)
    httpClient.get(uri"https://dd.weather.gc.ca/citypage_weather/xml/BC" / (cityStationId + "_e.xml")) { response =>
      for {
        responseText <- response.as[String]

        result = if (response.status.isSuccess) {
          ecapi.CityStationReportDecoder.decode(responseText) match {
            case Left(decodingError) =>
              ApiResponse.Error(
                s"City id ${cityStationId}: ${decodingError.getMessage}",
                Status.InternalServerError.code
              )
            case Right(reportXml) =>
              ApiResponse.Result((cityStationId, reportXml))
          }
        } else {
          ApiResponse.Error(
            s"Weather API error [${response.status.code}]: ${responseText}",
            Status.InternalServerError.code
          )
        }

      } yield result
    }
  }

  def fetchGradient(gradientId: String): IO[ApiResponse[GradientReport]] = {
    Gradient.forId(gradientId) match {
      case None =>
        ApiResponse.Error(
          s"Unknown gradient id: `$gradientId`.",
          Status.BadRequest.code
        ).pure

      case Some(gradient) =>
        val cityStationIds = gradient.cityIds
        // Make several requests in parallel to speed things up
        cityStationIds.parTraverse { cityStationId =>
          // The 0 second delay is for demo purposes. You can increase it to e.g. 10 seconds
          // to prove that requests are being made in parallel. If they were made
          // sequentially, you would expect a total latency of (N * 10 sec), where N is the
          // number of requests to be made, but the actual latency will be just 10 seconds.
          IO.sleep(0.seconds) >> fetchCityWeather(cityStationId)
        }
        .map { cityStationResponses =>
          val maybeError = cityStationResponses.collectFirst {
            case err: ApiResponse.Error => err
          }
          maybeError match {
            case Some(error) => error
            case None =>
              val successResponses = cityStationResponses.collect {
                case success: ApiResponse.Result[(String, CityStationReportXml) @unchecked] => success.result
              }
              ApiResponse.Result(
                cityStationReportsToGradientReport(gradient, successResponses.toMap)
              )
          }
        }
    }
  }

  private def findLocalDateTime(datetimes: List[DateTimeXml]): DateTimeXml = {
    val localDateTimes = datetimes.filter(_.zone != "UTC")
    assert(
      localDateTimes.length == 1,
      s"Expected 1 local datetime, found ${localDateTimes.length}:\n\n${localDateTimes.toString()}"
    )
    localDateTimes.head
  }

  /**
   * @param gradient
   * @param cityReportXmls
   * @return
   */
  private def cityStationReportsToGradientReport(
    gradient: Gradient,
    cityReportXmls: Map[String, CityStationReportXml]
  ): GradientReport = {
    // #TODO[Integrity] Assert that we're getting the expected units
    //  (e.g. that the API reports degrees in Celsius – we have this data, just not checking right now.)

    val currentConditionsByCity = cityReportXmls.collect {
      case (cityStationId, CityStationReportXml(Some(currentConditionsXml), _)) =>
        val dateObserved = findLocalDateTime(currentConditionsXml.dateTime)
        val maybeWind = if (currentConditionsXml.wind.speed.value.nonEmpty) {
          Some(EcWindConditions(
            speedKmh = Try(currentConditionsXml.wind.speed.value.toDouble).getOrElse(0),
            direction = currentConditionsXml.wind.direction.value,
            bearingDegrees = Try(currentConditionsXml.wind.bearing.value.toDouble).getOrElse(0),
          ))
        } else {
          None
        }
        val currentConditions = CityCurrentConditions(
          localDatetimeObserved = dateObserved.toLocalDateTime.toString,
          temperatureC = Try(currentConditionsXml.temperature.value.toDouble).toOption,
          pressureKPa = Try(currentConditionsXml.pressure.value.toDouble).toOption,
          relativeHumidityPercent = Try(currentConditionsXml.relativeHumidity.value.toDouble).toOption,
          wind = maybeWind
        )

        (cityStationId, currentConditions)
    }

    GradientReport(
      cities = gradient.cities,
      currentConditionsByCity = currentConditionsByCity,
      forecastDays = Nil, // #nc
      forecastsByDay = Map.empty // #nc
    )
  }

}
