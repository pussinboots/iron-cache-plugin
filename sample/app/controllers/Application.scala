package controllers

import play.api._
import play.api.mvc._
import play.api.Play.current
import play.api.cache.Cache
import com.github.tmwtmp100.cache.IronCacheApi
import scala.concurrent.duration._
import javax.inject.Inject

class Application @Inject() (ironPlugin: IronCacheApi) extends Controller {

  //private val ironPlugin = play.api.Play.current.plugin[IronCachePlugin].get
  
  def index = Action {
    Ok(views.html.index.render("Your new application is ready."))
  }

  def cacheSetExpiration(value: String) = Action {
    ironPlugin.set("test", value, Duration(60, MINUTES))
    Ok(value + " set.")
  }

  def cacheSetString(key: String, value: String) = Action {
    ironPlugin.set(key, value)
    Ok("Set " + key +  " to " + value)
  }

  def cacheSetInt(key: String, value: Int) = Action {
    ironPlugin.set(key, value)
    Ok("Set " + key + " to " + value)
  }

  def cacheGet() = Action {
    ironPlugin.get("test") match {
      case Some(value) => Ok("Cache value found: " + value)
      case None => Ok("Cache value not found")
    }
  }

  def cacheIncrement(key: String, incVal: Int) = Action {
    ironPlugin.increment(key, incVal) match {
      case Some(amount) => Ok("Cache value increased by " + incVal + " by " + amount)
      case _ => Ok("Error with key inc-key while incrementing value.")
    }
  }

  def clearCache() = Action {
    ironPlugin.clearCache()
    Ok("Cache has been cleared.")
  }

  def cacheDelete() = Action {
    ironPlugin.remove("test")
    Ok("Removed.")
  }
  
}