package org.grapheco.lynx.types.time

import org.grapheco.lynx.types.property.{LynxInteger, LynxString}
import org.grapheco.lynx.types.structural.LynxPropertyKey
import org.grapheco.lynx.types.{LTLocalTime, LocalDateTimeType, LocalTimeType, LynxValue, TypeMismatchException}
import org.grapheco.lynx.types.time.LynxComponentTime.{getHourMinuteSecond, getNanosecond, truncateTime}
import org.grapheco.lynx.types.time.LynxComponentTimeZone.getZone
import org.grapheco.lynx.types.traits.HasProperty
import org.grapheco.lynx.util.LynxTemporalParseException

import java.time.{LocalTime, ZoneId}

/**
 * @ClassName LynxLocalTime
 * @Description TODO
 * @Author huchuan
 * @Date 2022/4/1
 * @Version 0.1
 */
case class LynxLocalTime(localTime: LocalTime) extends LynxTemporalValue with LynxComponentTime with HasProperty {
  def value: LocalTime = localTime

  def lynxType: LocalTimeType = LTLocalTime

  override def sameTypeCompareTo(o: LynxValue): Int = o match {
    case n: LynxLocalTime => localTime.compareTo(n.localTime)
    case _ => throw TypeMismatchException(this.lynxType, o.lynxType)
  }

  //LynxComponentTime
  var hour: Int = localTime.getHour
  var minute: Int = localTime.getMinute
  var second: Int = localTime.getSecond
  var millisecond: Int = (localTime.getNano * Math.pow(0.1, 6)).toInt
  var microsecond: Int = (localTime.getNano * Math.pow(0.1, 3) - millisecond * Math.pow(10, 3)).toInt
  var nanosecond: Int = localTime.getNano % Math.pow(10, 3).toInt
  var fraction: Int = localTime.getNano


  override def keys: Seq[LynxPropertyKey] = Seq("hour", "minute", "second", "millisecond", "microsecond", "nanosecond").map(LynxPropertyKey)

  override def property(propertyKey: LynxPropertyKey): Option[LynxValue] = Some(propertyKey.value match {
    case "hour" => LynxInteger(this.hour)
    case "minute" => LynxInteger(this.minute)
    case "second" => LynxInteger(this.second)
    case "millisecond" => LynxInteger(this.millisecond)
    case "microsecond" => LynxInteger(this.microsecond + this.millisecond * Math.pow(10, 3).toLong)
    case "nanosecond" => LynxInteger(this.fraction)
    case _ => null
  })
}

object LynxLocalTime {

  def now(): LynxLocalTime = LynxLocalTime(LocalTime.now())

  def now(zoneId: ZoneId): LynxLocalTime = LynxLocalTime(LocalTime.now(zoneId))

  def of(localTime: LocalTime): LynxLocalTime = LynxLocalTime(localTime)

  def of(hour: Int, minute: Int, second: Int, nanosOfSecond: Int): LynxLocalTime =
    LynxLocalTime(LocalTime.of(hour, minute, second, nanosOfSecond))

  def parse(localTimeStr: String): LynxLocalTime = {
    val timeTuple = getHourMinuteSecond(localTimeStr)
    var timeStr = timeTuple._1.formatted("%02d") + ":" + timeTuple._2.formatted("%02d") + ":" + timeTuple._3.formatted("%02d") + (timeTuple._4 match {
      case 0 => ""
      case v: Int => "." + v.toString
    })
    LynxLocalTime(LocalTime.parse(timeStr))
  }

  def parse(map: Map[String, Any]): LynxLocalTime = {
    if (map.isEmpty) {
      throw LynxTemporalParseException("At least one temporal unit must be specified")
    }
    var v: LocalTime = null
    if (map.contains("timezone")) {
      if (map.size == 1) {
        of(LocalTime.now(getZone(map("timezone") match {
          case v: String => v
          case LynxString(v) => v.replace(" ", "_")
          case v: Any => v.toString
        })))
      } else {
        throw LynxTemporalParseException("Cannot assign time zone if also assigning other fields")
      }
    }
    else if (map.contains("hour")) {
      val (hour, minute, second) = getHourMinuteSecond(map, requiredHasDay = false)
      val nanoOfSecond = getNanosecond(map, requiredHasSecond = true)
      of(hour, minute, second, nanoOfSecond)
    } else if (map.contains("unitStr")) {
      val (hour, minute, second, nanoOfSecond) = truncateTime(map)
      of(hour, minute, second, nanoOfSecond)
    } else if (map.contains("time")) {
      val timeMap = map.get("time").orNull match {
        case v: LynxTime => v
        case v: LynxLocalTime => v
      }
      val hour = map.getOrElse("hour", timeMap.hour) match {
        case v: Int => v
        case LynxInteger(v) => v.toInt
      }
      val minute = map.getOrElse("minute", timeMap.minute) match {
        case v: Int => v
        case LynxInteger(v) => v.toInt
      }
      val second = map.getOrElse("second", timeMap.second) match {
        case v: Int => v
        case LynxInteger(v) => v.toInt
      }
      val millisecond = map.getOrElse("millisecond", timeMap.millisecond) match {
        case v: Int => v * Math.pow(10, 6)
        case LynxInteger(v) => v * Math.pow(10, 6).toInt
      }
      val microsecond = map.getOrElse("microsecond", timeMap.microsecond) match {
        case v: Int => v * Math.pow(10, 3)
        case LynxInteger(v) => v * Math.pow(10, 3).toInt
      }
      val nanosecond = map.getOrElse("nanosecond", timeMap.nanosecond) match {
        case v: Int => v
        case LynxInteger(v) => v.toInt
      }
      of(hour, minute, second, (millisecond + microsecond + nanosecond).toInt)

    }
    else throw LynxTemporalParseException("parse date from map: map not contains (hour, minute, second) ")
  }
}
