package warwick.sso

import java.time.{ZoneId, ZonedDateTime}

import scala.collection.JavaConverters._

case class GroupName(string: String)

case class Group(
  name: GroupName,
  title: Option[String],
  members: Seq[Usercode],
  owners: Seq[Usercode],
  `type`: String,
  department: Department,
  updatedAt: ZonedDateTime,
  restricted: Boolean
) {

  def contains(usercode: Usercode): Boolean = owners.contains(usercode) || members.contains(usercode)

}

object Group {

  def apply(g: uk.ac.warwick.userlookup.Group): Group =
    Group(
      GroupName(g.getName),
      Option(g.getTitle),
      g.getUserCodes.asScala.map(Usercode),
      g.getOwners.asScala.map(Usercode),
      g.getType,
      Department(None, Some(g.getDepartment), Some(g.getDepartmentCode)),
      ZonedDateTime.ofInstant(g.getLastUpdatedDate.toInstant, ZoneId.systemDefault()),
      g.isRestricted
    )

}
