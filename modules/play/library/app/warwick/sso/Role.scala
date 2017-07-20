package warwick.sso

import com.google.inject.{ImplementedBy, Inject, Singleton}
import com.typesafe.config.{ConfigObject, ConfigValue}
import play.api.Configuration

import scala.collection.JavaConverters._

case class RoleName(string: String)
case class Role(name: RoleName, groupName: GroupName)

@ImplementedBy(classOf[RoleServiceImpl])
trait RoleService {

  def getRole(name: RoleName): Role

}

@Singleton
class RoleServiceImpl @Inject()(
  configuration: Configuration
) extends RoleService {

  private val roles: Map[RoleName, Role] = configuration.getOptional[ConfigObject]("sso-client.role-groups").map { roles =>
    roles.asScala.map {
      case (roleName: String, groupName: ConfigValue) =>
        RoleName(roleName) -> Role(RoleName(roleName), GroupName(groupName.unwrapped.asInstanceOf[String]))
    }.toMap
  }.getOrElse(Map.empty)

  override def getRole(name: RoleName): Role = roles.getOrElse(name, throw new IllegalStateException(s"Role '${name.string}' is not configured"))

}

