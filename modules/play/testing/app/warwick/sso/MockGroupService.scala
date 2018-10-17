package warwick.sso

import java.time.ZonedDateTime

import javax.inject.Inject
import play.api.{ConfigLoader, Configuration}

import scala.collection.JavaConverters._
import scala.util.{Success, Try}

/**
  * Mock GroupService with static group data based on application Configuration.
  *
  * webgroups.test-data.groups = [
  *   {
  *     name = "in-mygroup"
  *     description = "Optional description"
  *     members = [ "cusebr", "cuscav" ]
  *     owners = [ "cusfal" ]
  *     type = "Arbitrary"
  *     department.code = "in"
  *     department.name = "IT Services"
  *     updated = "2018-08-20T11:49:36.497+01:00"
  *     restricted = false
  *   }
  * ]
  */
class MockGroupService @Inject()(
 config: Configuration
) extends GroupService {

  lazy val groups: Seq[Group] = config.get[Seq[Group]]("webgroups.test-data.groups")

  implicit val groupLoader: ConfigLoader[Seq[Group]] = ConfigLoader { config => path => {
    config.getConfigList(path).asScala.map { gc =>
      val conf = Configuration(gc)
      Group(
        GroupName(conf.get[String]("name")),
        conf.getOptional[String]("description"),
        conf.getOptional[Seq[String]]("members").getOrElse(Nil).map(Usercode.apply),
        conf.getOptional[Seq[String]]("owners").getOrElse(Nil).map(Usercode.apply),
        conf.getOptional[String]("type").getOrElse("Arbitrary"),
        Department(None, conf.getOptional[String]("department.name"), conf.getOptional[String]("department.code")),
        ZonedDateTime.parse(conf.get[String]("updated")),
        conf.getOptional[Boolean]("restricted").getOrElse(false)
      )
    }
  }}

  private def findGroup(groupName: GroupName): Option[Group] = groups.find(_.name == groupName)

  override def getWebGroup(groupName: GroupName): Try[Option[Group]] = Success(findGroup(groupName))

  override def isUserInGroup(usercode: Usercode, groupName: GroupName): Try[Boolean] = Success(findGroup(groupName).exists(_.members.contains(usercode)))

  override def getGroupsForUser(usercode: Usercode): Try[Seq[Group]] = Success(groups.filter(_.members.contains(usercode)))

  override def getGroupsInDepartment(department: Department): Try[Seq[Group]] = Success(groups.filter(_.department.code == department.code))

  override def getGroupsForQuery(query: String): Try[Seq[Group]] = Success(
    groups.filter { group =>
      group.name.string.contains(query) ||
      group.title.exists(_.contains(query))
    }
  )
  
  override def hasCache: Boolean = false
}
