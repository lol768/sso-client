package warwick.sso

import com.google.inject.{Singleton, ImplementedBy, Inject}
import com.typesafe.config.ConfigValue
import play.api.Configuration
import scala.collection.JavaConversions._

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

  private val roles: Map[RoleName, Role] = configuration.getObject("sso-client.role-groups").map { roles =>
    roles.map {
      case (roleName: String, groupName: ConfigValue) =>
        RoleName(roleName) -> Role(RoleName(roleName), GroupName(groupName.unwrapped.asInstanceOf[String]))
    }.toMap
  }.getOrElse(Map.empty)

  override def getRole(name: RoleName): Role = roles.getOrElse(name, throw new IllegalStateException(s"Role '${name.string}' is not configured"))

}

