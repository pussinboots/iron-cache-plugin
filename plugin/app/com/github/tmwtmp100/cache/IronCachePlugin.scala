package com.github.tmwtmp100.cache


import javax.inject.{Inject, Singleton}
import java.util.concurrent.TimeUnit

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Await

import play.api.cache.CacheApi
import play.api.libs.ws._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment, Application}
import play.cache.{CacheApi => JavaCacheApi, DefaultCacheApi => DefaultJavaCacheApi}

import scala.reflect.ClassTag

@Singleton
class IronCacheApi @Inject()(app: Application, ws: WSClient) extends CacheApi {

	val hostAddress = app.configuration.getString("iron.cache.host").getOrElse("cache-aws-us-east-1")
  	val cacheName   = app.configuration.getString("iron.cache.name").getOrElse("cache")
	val oAuthToken  = app.configuration.getString("iron.token").get
	val projectId   = app.configuration.getString("iron.project.id").get

	private val projectAddress = "https://" + hostAddress + ".iron.io/1/projects/" + projectId
	private val baseAddress  = projectAddress + "/caches/" + cacheName
	private val clearAddress = baseAddress + "/clear"
	private val address      = baseAddress + "/items/"

	private val auth = ("Authorization", "OAuth " + oAuthToken)
	private val jsonCT = ("Content-Type","application/json")
	private val appName = "Iron Cache Plugin"

	override def set(key: String, value: Any, expiration: Duration) = {
      play.Logger.info(address + key)
      val exp = if (expiration.isFinite()) expiration.toSeconds.toInt else 0
      val typedValue: JsValue = value match {
        case i: String => JsString(value.asInstanceOf[String])
        case i: Int =>    JsNumber(value.asInstanceOf[Int])
        case i: Double => JsNumber(value.asInstanceOf[Double])
        case i: Boolean => JsBoolean(value.asInstanceOf[Boolean])
        case _ => JsNull
      }
      ws.url(address + key).withHeaders(auth, jsonCT)
        .put(Json.obj("value" -> typedValue, "expires_in" -> exp))
        .onComplete(x =>
            x.get.status match {
              case 200 => {}
              case _ => play.Logger.warn(appName + " experienced a problem setting " + key + ":" + x.get.json \ "msg")
            }
        )
    }

	override def get[A](key: String)(implicit ct: ClassTag[A]) = {
		val ironGet = ws.url(address + key).withHeaders(auth)
			.get().map { response =>
					import play.api.libs.json._
					implicit val optionStringReads: Reads[Option[String]] = Reads.optionWithNull[String]
					response.status match {
						case 200 => (response.json \ "value").asOpt[String]
						case _ => {
							play.Logger.debug(appName + " could not retrieve key " + key + ":" + response.json \ "msg")
							None
						}
					}
			}
		val result = Await.result(ironGet, Duration.Inf)
		Option(
			result match {
				case x if ct.runtimeClass.isInstance(x) => x.asInstanceOf[A]
				case x if ct == ClassTag.Nothing => x.asInstanceOf[A]
				case x => x.asInstanceOf[A]
			}
		)
	}

	override def remove(key: String) {
		ws.url(address + key).withHeaders(auth)
			.delete()
	}

	override def getOrElse[A](key: String, expiration: Duration)(orElse: => A)(implicit ev: ClassTag[A]) = {
	    get(key).getOrElse(orElse)
	}

	def increment(key: String, amount: Int): Option[Int] = {
		play.Logger.debug("Increment\n" + address + key + "/increment")
		val incrementCall = ws.url(address + key + "/increment").withHeaders(auth, jsonCT)
													.post(Json.obj("amount" -> amount))
													.map { response =>
														response.status match {
															case 200 => (response.json \ "value").asOpt[Int]
															case _ => {
																play.Logger.error(appName + " experienced an error while incrementing " + key + ":" + response.json \ "msg")
																None
															}
														}
													}

		Await.result(incrementCall, Duration.Inf)
	}

	def clearCache() {
		ws.url(clearAddress).withHeaders(auth).post("")
	}

	/*override def enabled: Boolean = {
		val relatedKeys = List("iron.token", "iron.project.id")
		val isEnabled: Boolean = {
			relatedKeys.count { key =>
				app.configuration.getString(key).isInstanceOf[Some[String]]
			} == relatedKeys.size
		}
		isEnabled match {
				case true => {
				play.Logger.info(appName + " has been enabled.")
				// Now check to see if the service is reachable
				import java.net.{URLConnection, URL, HttpURLConnection}
				
				val u = new URL(projectAddress)
				val conn = u.openConnection().asInstanceOf[HttpURLConnection]
				conn.setRequestProperty("Authorization",  s"OAuth $oAuthToken")
				//conn.setConnectTimeout(HttpRequestTimeout)
				conn.connect
				val status = conn.getResponseCode
				if(status > 500){
						play.Logger.error("Iron Cache service is unresponsive. The plugin will not work.")
						throw new IllegalAccessException()
				}
			}
			case _ => play.Logger.warn(appName + " is not enabled due to missing required properties. " +
																	"Check to see if the token and project ID have been set.")   
		}
		//TODO fixe implicite Application use of WS during plugin enabled cause stack overflow
		/*isEnabled match {
			case true => {
				play.Logger.info(appName + " has been enabled.")
				// Now check to see if the service is reachable
				WS.url(projectAddress).withHeaders(auth).get().map { response =>
					if(response.status > 500){
						play.Logger.error("Iron Cache service is unresponsive. The plugin will not work.")
						throw new IllegalAccessException()
					}
				}
			}
			case _ => play.Logger.warn(appName + " is not enabled due to missing required properties. " +
																	"Check to see if the token and project ID have been set.")
		}*/
		isEnabled
	}*/
}

class IronCacheModule extends Module {
	override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(
		bind[CacheApi].to[IronCacheApi]
	)
}
