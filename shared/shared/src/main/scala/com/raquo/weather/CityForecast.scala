package com.raquo.weather

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

/**
 * @param dateTimeIssued  Time that the forecast was issued at.
 * @param dateEffective   Date (yyyy-mm-dd) that the forecast is for.
 * @param iconCode        Two-digit weather conditions code. See https://eccc-msc.github.io/open-data/msc-data/citypage-weather/readme_citypageweather-datamart_en/#icons-of-the-xml-product
 * @param temperatureC    Temperature in degrees Celsius
 * @param windKts         Average wind over the last hour, in kts
 * @param windDirDegrees  Average wind direction over the last hour, in degrees
 */
case class CityForecast(
  dateTimeIssued: String,
  dateEffective: String,
  iconCode: String,
  temperatureC: Double,
  windKts: Double,
  windDirDegrees: Double,
)

object CityForecast {

  implicit val codec: JsonValueCodec[CityForecast] = JsonCodecMaker.make
}
